package com.example.mobile_android.ui.post;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> currentPostList = new ArrayList<>(); // 어댑터에 공급할 현재 리스트
    private List<Post> allPostsList = new ArrayList<>(); // 전체 게시물 저장 (검색용)

    private TabLayout tabLayout;
    private EditText etSearch;
    private ImageButton btnClearSearch;

    private ApiService apiService;
    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private String currentFilter = "all";
    private String searchQuery = "";

    private LiveData<List<Post>> allPostsLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSearchView();
        setupTabLayout();
        loadPostsFromServer();
        observeDatabase();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.postRecyclerView);
        tabLayout = view.findViewById(R.id.tab_layout);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        apiService = ApiClient.getClient().create(ApiService.class);
        postDao = AppDatabase.getInstance(requireContext()).postDao();
        databaseExecutor = Executors.newSingleThreadExecutor();

        allPostsLiveData = postDao.getAllPosts();
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

    private void setupSearchView() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterPosts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });
    }

    private void filterPosts() {
        currentPostList.clear();

        List<Post> filteredList = allPostsList;

        // 검색어 필터링
        if (!searchQuery.isEmpty()) {
            String lowerQuery = searchQuery.toLowerCase();
            filteredList = filteredList.stream()
                    .filter(post -> {
                        String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
                        String content = post.getContent() != null ? post.getContent().toLowerCase() : "";
                        return title.contains(lowerQuery) || content.contains(lowerQuery);
                    })
                    .collect(Collectors.toList());
        }

        // 탭 필터링
        if ("new".equals(currentFilter)) {
            filteredList = filteredList.stream()
                    .filter(post -> post.isActuallyNew())
                    .collect(Collectors.toList());
        } else if ("saved".equals(currentFilter)) {
            filteredList = filteredList.stream()
                    .filter(Post::isSaved)
                    .collect(Collectors.toList());
        }

        currentPostList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "new";
                        break;
                    case 2:
                        currentFilter = "saved";
                        break;
                }
                filterPosts();
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

    }

    private void updateAdapter(List<Post> posts) {
        allPostsList.clear();
        allPostsList.addAll(posts);
        filterPosts();
    }

    private void loadPostsFromServer() {
        apiService.getPosts(1, 100, null, null, null, null, "created_at", "desc")
                .enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    databaseExecutor.execute(() -> postDao.upsert(response.body().getItems()));
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "게시물 불러오기 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
