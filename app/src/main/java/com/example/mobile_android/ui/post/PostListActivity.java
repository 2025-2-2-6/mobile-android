package com.example.mobile_android.ui.post;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> currentPostList = new ArrayList<>();
    private List<Post> allPostList = new ArrayList<>();
    private ApiService apiService;
    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private String siteId;
    private String filterType; // 필터 타입 (null, "new_posts" 등)
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyPosts;
    private String categoryFilter = ""; // 선택된 카테고리 필터
    private List<String> currentCategories = new ArrayList<>(); // 현재 표시중인 카테고리 목록

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_post_list);

        initDatabase();
        apiService = ApiClient.getClient().create(ApiService.class);

        // Intent에서 사이트 정보 가져오기
        String siteName = getIntent().getStringExtra("SITE_NAME");
        siteId = getIntent().getStringExtra("SITE_ID");
        filterType = getIntent().getStringExtra("FILTER_TYPE"); // "new_posts" 등

        setupToolbar(siteName);
        setupRecyclerView();
        setupSearchView();
        setupSwipeRefresh();
        setupFilterChips();
        setupCategoryFilter();

        if (siteId != null) {
            observePostsBySite();
            observeCategoriesBySite();
            loadPostsFromServer();
        }
    }

    private void initDatabase() {
        postDao = AppDatabase.getInstance(this).postDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupToolbar(String siteName) {
        ImageButton btnBack = findViewById(R.id.btn_back);
        android.widget.TextView tvSiteName = findViewById(R.id.tv_site_name);

        if (siteName != null) {
            tvSiteName.setText(siteName);
        } else {
            tvSiteName.setText("게시물 목록");
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(this, currentPostList);
        recyclerView.setAdapter(postAdapter);
        layoutEmptyPosts = findViewById(R.id.layout_empty_posts);
    }

    private void setupSwipeRefresh() {
        swipeRefresh = findViewById(R.id.swipe_refresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    getResources().getColor(android.R.color.holo_blue_bright),
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_orange_light)
            );
            swipeRefresh.setOnRefreshListener(() -> {
                loadPostsFromServer();
                swipeRefresh.setRefreshing(false);
            });
        }
    }

    private void setupSearchView() {
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchButton = findViewById(R.id.clearSearchButton);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
        });
    }

    private void applyFilters() {
        String query = searchEditText.getText().toString();

        List<Post> filtered = allPostList.stream()
                .filter(post -> {
                    // is_new 필터링 (FILTER_TYPE="new_posts"인 경우)
                    if ("new_posts".equals(filterType)) {
                        if (!post.isActuallyNew()) {
                            return false;
                        }
                    }

                    // 카테고리 필터링
                    if (!categoryFilter.isEmpty()) {
                        String postCategory = post.getCategory() != null ? post.getCategory() : "";
                        if (!postCategory.equals(categoryFilter)) {
                            return false;
                        }
                    }

                    // 검색어 필터링
                    if (!query.isEmpty()) {
                        String lowerQuery = query.toLowerCase();
                        String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
                        String content = post.getContent() != null ? post.getContent().toLowerCase() : "";
                        return title.contains(lowerQuery) || content.contains(lowerQuery);
                    }

                    return true;
                })
                .collect(Collectors.toList());

        currentPostList.clear();
        currentPostList.addAll(filtered);
        postAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (currentPostList.isEmpty()) {
            layoutEmptyPosts.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmptyPosts.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void observePostsBySite() {
        LiveData<List<Post>> postsLiveData;

        // FILTER_TYPE이 "new_posts"이면 전체 게시물 조회, 아니면 특정 사이트만
        if ("new_posts".equals(filterType)) {
            postsLiveData = postDao.getAllPosts();
        } else if (siteId != null) {
            postsLiveData = postDao.getPostsBySite(siteId);
        } else {
            postsLiveData = postDao.getAllPosts();
        }

        postsLiveData.observe(this, posts -> {
            allPostList.clear();
            allPostList.addAll(posts);
            applyFilters();
        });
    }

    private void loadPostsFromServer() {
        String token = TokenManager.getBearerToken(this);
        Call<PostListResponse> call;

        // FILTER_TYPE이 "new_posts"이면 전체 게시물 조회, 아니면 특정 사이트만
        if ("new_posts".equals(filterType)) {
            call = apiService.getPosts(token, 1, 1000, null, null, null, null, "created_at", "desc");
        } else {
            call = apiService.getPosts(token, 1, 100, null, siteId, null, null, "created_at", "desc");
        }

        call.enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    databaseExecutor.execute(() -> {
                        if ("new_posts".equals(filterType)) {
                            // 전체 게시물 덮어쓰기
                            postDao.upsert(response.body().getItems());
                        } else {
                            // 특정 사이트의 게시물만 덮어쓰기
                            postDao.upsertBySite(siteId, response.body().getItems());
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                Toast.makeText(PostListActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterChips() {
        ChipGroup chipGroup = findViewById(R.id.chip_filters);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                categoryFilter = "";
            } else {
                int selectedId = checkedIds.get(0);
                if (selectedId == R.id.chip_all) {
                    categoryFilter = "";
                } else {
                    // 동적으로 생성된 칩의 텍스트를 가져옴
                    Chip selectedChip = group.findViewById(selectedId);
                    if (selectedChip != null) {
                        categoryFilter = selectedChip.getText().toString();
                    }
                }
            }
            applyFilters();
        });
    }

    private void setupCategoryFilter() {
        // ChipGroup은 이미 setupFilterChips에서 설정됨
    }

    private void observeCategoriesBySite() {
        postDao.getCategoriesBySite(siteId).observe(this, categories -> {
            if (categories != null) {
                updateCategoryChips(categories);
            }
        });
    }

    private void updateCategoryChips(List<String> categories) {
        // 카테고리 목록이 변경되지 않았으면 스킵
        if (categories.equals(currentCategories)) {
            return;
        }
        currentCategories = new ArrayList<>(categories);

        ChipGroup chipGroup = findViewById(R.id.chip_filters);
        if (chipGroup == null) return;

        // 모든 동적 칩 제거 (R.id.chip_all은 유지)
        int childCount = chipGroup.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = chipGroup.getChildAt(i);
            if (child.getId() != R.id.chip_all) {
                chipGroup.removeViewAt(i);
            }
        }

        // 새 카테고리 칩 추가
        for (String category : categories) {
            Chip chip = new Chip(this);
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


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
