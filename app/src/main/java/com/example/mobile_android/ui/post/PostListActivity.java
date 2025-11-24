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

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.example.mobile_android.util.TokenManager;

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
    private EditText searchEditText;
    private ImageButton clearSearchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        initDatabase();
        apiService = ApiClient.getClient().create(ApiService.class);

        // Intent에서 사이트 정보 가져오기
        String siteName = getIntent().getStringExtra("SITE_NAME");
        siteId = getIntent().getStringExtra("SITE_ID");

        setupToolbar(siteName);
        setupRecyclerView();
        setupSearchView();
        setupFilterChips();

        if (siteId != null) {
            observePostsBySite();
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
                // 현재 선택된 칩에 따라 필터링
                com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chip_filters);
                int checkedId = chipGroup.getCheckedChipId();
                if (checkedId != View.NO_ID) {
                    filterPostsByChip(checkedId);
                } else {
                    filterPosts(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
        });
    }

    private void filterPosts(String query) {
        if (query.isEmpty()) {
            currentPostList.clear();
            currentPostList.addAll(allPostList);
        } else {
            String lowerQuery = query.toLowerCase();
            List<Post> filtered = allPostList.stream()
                    .filter(post -> {
                        String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
                        String content = post.getContent() != null ? post.getContent().toLowerCase() : "";
                        return title.contains(lowerQuery) || content.contains(lowerQuery);
                    })
                    .collect(Collectors.toList());
            currentPostList.clear();
            currentPostList.addAll(filtered);
        }
        postAdapter.notifyDataSetChanged();
    }

    private void observePostsBySite() {
        LiveData<List<Post>> postsLiveData = postDao.getPostsBySite(siteId);
        postsLiveData.observe(this, posts -> {
            allPostList.clear();
            allPostList.addAll(posts);

            // 검색어가 있으면 필터링, 없으면 전체 표시
            String query = searchEditText.getText().toString();
            filterPosts(query);
        });
    }

    private void loadPostsFromServer() {
        String token = TokenManager.getBearerToken(this);
        Call<PostListResponse> call = apiService.getPosts(token, 1, 100, null, siteId, null, null, "created_at", "desc");

        call.enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    databaseExecutor.execute(() -> {
                        // 특정 사이트의 게시물만 덮어쓰기 (전체 DB를 지우지 않음)
                        postDao.upsertBySite(siteId, response.body().getItems());
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
        com.google.android.material.chip.ChipGroup chipGroup = findViewById(R.id.chip_filters);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            filterPostsByChip(checkedId);
        });
    }

    private void filterPostsByChip(int chipId) {
        String query = searchEditText.getText().toString();

        if (chipId == R.id.chip_all) {
            // 전체: 검색어만 적용
            filterPosts(query);
        } else if (chipId == R.id.chip_recruiting) {
            // 모집중: TODO - 마감일이 지나지 않은 게시물
            filterPostsByStatus(query, "recruiting");
        } else if (chipId == R.id.chip_priority) {
            // 우선: TODO - 우선순위가 높은 게시물
            filterPostsByStatus(query, "priority");
        } else if (chipId == R.id.chip_new) {
            // NEW: 최근 게시물
            filterPostsByStatus(query, "new");
        }
    }

    private void filterPostsByStatus(String query, String status) {
        List<Post> filtered = allPostList.stream()
                .filter(post -> {
                    // 검색어 필터
                    boolean matchesQuery = query.isEmpty() ||
                            (post.getTitle() != null && post.getTitle().toLowerCase().contains(query.toLowerCase())) ||
                            (post.getContent() != null && post.getContent().toLowerCase().contains(query.toLowerCase()));

                    if (!matchesQuery) return false;

                    // 상태 필터
                    switch (status) {
                        case "new":
                            return post.isActuallyNew();
                        case "recruiting":
                            // 마감일 체크 (eventEndDate가 현재보다 미래)
                            return post.getEventEndDate() != null &&
                                   !post.getEventEndDate().isEmpty() &&
                                   isAfterToday(post.getEventEndDate());
                        case "priority":
                            // TODO: 우선순위 필드가 있다면 체크
                            return true;
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());

        currentPostList.clear();
        currentPostList.addAll(filtered);
        postAdapter.notifyDataSetChanged();
    }

    private boolean isAfterToday(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date eventDate = sdf.parse(dateStr);
            java.util.Date today = new java.util.Date();
            return eventDate != null && eventDate.after(today);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
