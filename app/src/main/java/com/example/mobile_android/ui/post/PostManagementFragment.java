package com.example.mobile_android.ui.post;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> currentPostList = new ArrayList<>(); // 어댑터에 공급할 현재 리스트

    private TabLayout tabLayout;
    private EditText etSearch;
    private CardView cardCalendarInfo;

    private ApiService apiService;
    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private String currentFilter = "all";

    private LiveData<List<Post>> allPostsLiveData;
    private LiveData<List<Post>> savedPostsLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupTabLayout();
        setupClickListeners();
        loadPostsFromServer();
        observeDatabase();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.postRecyclerView);
        tabLayout = view.findViewById(R.id.tab_layout);
        etSearch = view.findViewById(R.id.et_search);
        cardCalendarInfo = view.findViewById(R.id.card_calendar_info);

        apiService = ApiClient.getClient().create(ApiService.class);
        postDao = AppDatabase.getInstance(requireContext()).postDao();
        databaseExecutor = Executors.newSingleThreadExecutor();

        allPostsLiveData = postDao.getAllPosts();
        savedPostsLiveData = postDao.getSavedPosts();
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter(requireContext(), currentPostList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(requireContext(), PostDetailActivity.class);
            intent.putExtra("POST_ID", post.getId());
            startActivity(intent);
        });
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentFilter = "all";
                        allPostsLiveData.observe(getViewLifecycleOwner(), PostManagementFragment.this::updateAdapter);
                        break;
                    case 1:
                        currentFilter = "new";
                        // 'new' 필터링 로직은 getAllPosts() 결과에서 처리 가능하므로 별도 LiveData 불필요
                        allPostsLiveData.observe(getViewLifecycleOwner(), PostManagementFragment.this::updateAdapter);
                        break;
                    case 2:
                        currentFilter = "saved";
                        savedPostsLiveData.observe(getViewLifecycleOwner(), PostManagementFragment.this::updateAdapter);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void observeDatabase() {
        // 초기 탭("all")에 대한 관찰 시작
        allPostsLiveData.observe(getViewLifecycleOwner(), this::updateAdapter);

        // 저장된 게시물 개수 관찰
        savedPostsLiveData.observe(getViewLifecycleOwner(), savedPosts -> {
            if (savedPosts.size() > 0) {
                cardCalendarInfo.setVisibility(View.VISIBLE);
            } else {
                cardCalendarInfo.setVisibility(View.GONE);
            }
        });
    }

    private void updateAdapter(List<Post> posts) {
        currentPostList.clear();
        if ("new".equals(currentFilter)) {
             // 24시간 이내 게시물 필터링 (isNew 필드 또는 createdAt 기준)
            for (Post post : posts) {
                if (post.getIsNew() != null && post.getIsNew()) {
                    currentPostList.add(post);
                }
            }
        } else {
            currentPostList.addAll(posts);
        }
        adapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        if (cardCalendarInfo != null) {
            cardCalendarInfo.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "캘린더 이동", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void loadPostsFromServer() {
        Call<PostListResponse> call = apiService.getPosts(1, 100, null, null, null, null, "created_at", "desc");

        call.enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 서버에서 받은 데이터를 DB에 upsert
                    databaseExecutor.execute(() -> {
                        postDao.upsert(response.body().getItems());
                    });
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "게시물 불러오기 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
