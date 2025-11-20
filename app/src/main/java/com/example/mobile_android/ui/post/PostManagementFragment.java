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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 게시물 관리 화면
 * - 전체 게시물 목록 조회
 * - 탭 필터링 (전체, 새 게시물, 저장됨)
 * - 검색 기능
 * - 캘린더 등록된 게시물 표시
 */
public class PostManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private List<Post> filteredList = new ArrayList<>();

    private TabLayout tabLayout;
    private EditText etSearch;
    private CardView cardCalendarInfo;

    private ApiService apiService;
    private String currentFilter = "all";

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
        loadPosts();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.postRecyclerView);
        tabLayout = view.findViewById(R.id.tab_layout);
        etSearch = view.findViewById(R.id.et_search);
        cardCalendarInfo = view.findViewById(R.id.card_calendar_info);

        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter(requireContext(), filteredList);
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
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupClickListeners() {
        if (cardCalendarInfo != null) {
            cardCalendarInfo.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "캘린더 이동", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void loadPosts() {
        // 모든 게시물 조회
        Call<com.example.mobile_android.model.PostListResponse> call = apiService.getPosts(
                1, 100, null, null, null, null, "created_at", "desc"
        );

        call.enqueue(new Callback<com.example.mobile_android.model.PostListResponse>() {
            @Override
            public void onResponse(Call<com.example.mobile_android.model.PostListResponse> call,
                                   Response<com.example.mobile_android.model.PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body().getItems());
                    filterPosts();
                    updateCalendarInfo();
                }
            }

            @Override
            public void onFailure(Call<com.example.mobile_android.model.PostListResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "게시물 불러오기 실패: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterPosts() {
        filteredList.clear();

        if ("all".equals(currentFilter)) {
            filteredList.addAll(postList);
        } else if ("new".equals(currentFilter)) {
            // 24시간 이내 게시물
            long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            for (Post post : postList) {
                // TODO: created_at 파싱하여 필터링
                filteredList.add(post);
            }
        } else if ("saved".equals(currentFilter)) {
            // 저장된 게시물 (캘린더 등록된 게시물)
            // TODO: SharedPreferences에서 저장된 게시물 확인
        }

        adapter.notifyDataSetChanged();
    }

    private void updateCalendarInfo() {
        // 캘린더 등록된 게시물 개수 확인
        int calendarCount = 0;
        // TODO: 캘린더 등록된 게시물 개수 계산

        if (calendarCount > 0) {
            cardCalendarInfo.setVisibility(View.VISIBLE);
        } else {
            cardCalendarInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면으로 돌아올 때 목록 새로고침
        if (!postList.isEmpty()) {
            filterPosts();
            updateCalendarInfo();
        }
    }
}



