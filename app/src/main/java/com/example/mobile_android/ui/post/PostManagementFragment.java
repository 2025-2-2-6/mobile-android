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
import android.widget.TextView;
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
import com.example.mobile_android.util.TokenManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyPosts;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private ApiService apiService;
    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private String currentFilter = "all";
    private String searchQuery = "";
    private String categoryFilter = ""; // 선택된 카테고리 필터
    private List<String> currentCategories = new ArrayList<>(); // 현재 표시중인 카테고리 목록

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
        setupCategoryFilter(view);
        loadPostsFromServer();
        observeDatabase();
        observeCategories();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.postRecyclerView);
        tabLayout = view.findViewById(R.id.tab_layout);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        layoutEmptyPosts = view.findViewById(R.id.layout_empty_posts);
        tvEmptyTitle = view.findViewById(R.id.tv_empty_title);
        tvEmptySubtitle = view.findViewById(R.id.tv_empty_subtitle);

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

        // SwipeRefreshLayout 설정
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    getResources().getColor(android.R.color.holo_blue_bright),
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_orange_light)
            );
            swipeRefresh.setOnRefreshListener(() -> {
                loadPostsFromServer();
            });
        }
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

        // 카테고리 필터링
        if (!categoryFilter.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(post -> {
                        String postCategory = post.getCategory() != null ? post.getCategory() : "";
                        return postCategory.equals(categoryFilter);
                    })
                    .collect(Collectors.toList());
        }

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

        // 빈 상태 UI 업데이트
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (currentPostList.isEmpty()) {
            layoutEmptyPosts.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            // 필터에 따라 빈 상태 메시지 변경
            if ("new".equals(currentFilter)) {
                tvEmptyTitle.setText("새 게시물이 없습니다");
                tvEmptySubtitle.setText("아직 읽지 않은 게시물이 없습니다");
            } else if ("saved".equals(currentFilter)) {
                tvEmptyTitle.setText("저장된 게시물이 없습니다");
                tvEmptySubtitle.setText("게시물을 저장하여 나중에 확인하세요");
            } else {
                tvEmptyTitle.setText("게시물이 없습니다");
                tvEmptySubtitle.setText("사이트를 등록하고 게시물을 수집하세요");
            }
        } else {
            layoutEmptyPosts.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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

    private void setupCategoryFilter(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_category_filters);
        if (chipGroup == null) return;

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                categoryFilter = "";
            } else {
                int selectedId = checkedIds.get(0);
                if (selectedId == R.id.chip_category_all) {
                    categoryFilter = "";
                } else {
                    // 동적으로 생성된 칩의 텍스트를 가져옴
                    Chip selectedChip = group.findViewById(selectedId);
                    if (selectedChip != null) {
                        categoryFilter = selectedChip.getText().toString();
                    }
                }
            }
            filterPosts();
        });
    }

    private void observeCategories() {
        postDao.getDistinctCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                updateCategoryChips(categories);
            }
        });
    }

    private void updateCategoryChips(List<String> categories) {
        ChipGroup chipGroup = getView() != null ? getView().findViewById(R.id.chip_category_filters) : null;
        if (chipGroup == null) return;

        // 현재 ChipGroup에 있는 동적 칩 개수 확인
        int dynamicChipCount = 0;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child.getId() != R.id.chip_category_all) {
                dynamicChipCount++;
            }
        }

        // 카테고리 목록이 변경되지 않았고, 칩이 정상적으로 있으면 스킵
        if (categories.equals(currentCategories) && dynamicChipCount == categories.size()) {
            return;
        }
        currentCategories = new ArrayList<>(categories);

        // 모든 동적 칩 제거 (R.id.chip_category_all은 유지)
        int childCount = chipGroup.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = chipGroup.getChildAt(i);
            if (child.getId() != R.id.chip_category_all) {
                chipGroup.removeViewAt(i);
            }
        }

        // 새 카테고리 칩 추가
        for (String category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());

            // 스타일 프로그래밍 방식으로 적용
            chip.setChipBackgroundColorResource(R.color.chip_background_state);
            chip.setChipStrokeColorResource(R.color.chip_stroke_state);
            chip.setChipStrokeWidth(1);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_state));
            chip.setChipCornerRadius(16 * getResources().getDisplayMetrics().density);
            chip.setChipMinHeight(32 * getResources().getDisplayMetrics().density);
            chip.setChipStartPadding(12 * getResources().getDisplayMetrics().density);
            chip.setChipEndPadding(12 * getResources().getDisplayMetrics().density);
            chip.setTextSize(13);
            chip.setCheckedIconVisible(false);
            chip.setChipIconVisible(false);

            chipGroup.addView(chip);
        }
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
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }

        String token = TokenManager.getBearerToken(requireContext());
        apiService.getPosts(token, 1, 100, null, null, null, null, "created_at", "desc")
                .enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (response.isSuccessful() && response.body() != null) {
                    databaseExecutor.execute(() -> postDao.upsert(response.body().getItems()));
                    Toast.makeText(requireContext(), "게시물을 업데이트했습니다", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                Toast.makeText(requireContext(), "게시물 불러오기 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
