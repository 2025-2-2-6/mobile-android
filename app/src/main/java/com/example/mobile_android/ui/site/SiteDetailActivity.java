package com.example.mobile_android.ui.site;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.ui.post.PostAdapter;
import com.example.mobile_android.ui.post.PostListActivity;
import com.example.mobile_android.util.CategoryUtils;
import com.example.mobile_android.util.TokenManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SiteDetailActivity extends AppCompatActivity {

    private String siteId;
    private Site site;
    private PostDao postDao;
    private List<Post> recentPostsList = new ArrayList<>();

    // Views
    private ImageButton btnBack;
    private ImageView btnEdit;
    private ImageView btnDelete;
    private TextView tvSiteName;
    private com.google.android.material.chip.Chip chipCategory;
    private TextView tvUrl;
    private TextView tvLastCrawl;
    private TextView tvMemo;
    private TextView tvTotalCount;
    private TextView tvNewCount;
    private RecyclerView rvRecentPosts;
    private TextView tvViewAll;
    private View skeletonLoading;
    private View contentLayout;

    private PostAdapter recentPostsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_detail);

        siteId = getIntent().getStringExtra("SITE_ID");
        if (siteId == null || siteId.isEmpty()) {
            Toast.makeText(this, "사이트 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // PostDao 초기화
        AppDatabase db = AppDatabase.getInstance(this);
        postDao = db.postDao();

        initViews();
        setupListeners();
        loadSiteDetails();
        loadRecentPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 다른 화면에서 돌아왔을 때 데이터 새로고침
        // LiveData가 자동으로 최신 데이터를 반영하지만, 명시적으로 재조회
        if (siteId != null) {
            loadRecentPosts();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        tvSiteName = findViewById(R.id.tv_site_name);
        chipCategory = findViewById(R.id.chip_category);
        tvUrl = findViewById(R.id.tv_url);
        tvLastCrawl = findViewById(R.id.tv_last_crawl);
        tvMemo = findViewById(R.id.tv_memo);
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvNewCount = findViewById(R.id.tv_new_count);
        rvRecentPosts = findViewById(R.id.rv_recent_posts);
        tvViewAll = findViewById(R.id.tv_view_all);
        skeletonLoading = findViewById(R.id.skeleton_loading);
        contentLayout = findViewById(R.id.content_layout);

        rvRecentPosts.setLayoutManager(new LinearLayoutManager(this));
        recentPostsAdapter = new PostAdapter(this, recentPostsList);
        rvRecentPosts.setAdapter(recentPostsAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> showEditSiteDialog());

        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostListActivity.class);
            intent.putExtra("SITE_ID", siteId);
            intent.putExtra("SITE_NAME", tvSiteName.getText().toString());
            startActivity(intent);
        });
    }

    private void showSkeleton(boolean show) {
        if (show) {
            skeletonLoading.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);
        } else {
            skeletonLoading.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
        }
    }

    private void loadSiteDetails() {
        showSkeleton(true);
        String token = TokenManager.getBearerToken(this);
        ApiClient.getApiService().getSiteById(token, siteId).enqueue(new Callback<Site>() {
            @Override
            public void onResponse(Call<Site> call, Response<Site> response) {
                showSkeleton(false);
                if (response.isSuccessful() && response.body() != null) {
                    site = response.body();
                    updateUI();
                } else {
                    Toast.makeText(SiteDetailActivity.this, "사이트 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Site> call, Throwable t) {
                showSkeleton(false);
                Toast.makeText(SiteDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        if (site == null) return;

        tvSiteName.setText(site.getName() != null ? site.getName() : "이름 없음");

        // 카테고리 Chip 설정
        String category = site.getCategory() != null ? site.getCategory() : "기타";
        chipCategory.setText(category);
        tvUrl.setText(site.getUrl() != null ? site.getUrl().replace("https://", "").replace("http://", "") : "");

        // 메모 표시
        if (site.getDescription() != null && !site.getDescription().trim().isEmpty()) {
            tvMemo.setText(site.getDescription());
            tvMemo.setVisibility(View.VISIBLE);
        } else {
            tvMemo.setVisibility(View.GONE);
        }

        // 통계와 마지막 크롤링 날짜는 loadRecentPosts()의 LiveData에서 처리
    }

    private void loadRecentPosts() {
        // 먼저 서버에서 게시물 목록을 가져와서 DB에 저장
        fetchPostsFromServer();

        // PostDao를 사용해 로컬 DB에서 해당 사이트의 최신 4개 게시물 조회
        postDao.getPostsBySite(siteId).observe(this, posts -> {
            if (posts != null && !posts.isEmpty()) {
                // 최대 4개만 표시
                List<Post> recentPosts = posts.stream()
                        .limit(4)
                        .collect(Collectors.toList());

                recentPostsList.clear();
                recentPostsList.addAll(recentPosts);
                recentPostsAdapter.notifyDataSetChanged();

                // 통계 업데이트 (전체 게시물 수)
                tvTotalCount.setText(String.valueOf(posts.size()));

                // 새 게시물 수 계산
                long newCount = posts.stream()
                        .filter(Post::isActuallyNew)
                        .count();
                tvNewCount.setText(String.valueOf(newCount));

                Log.d("SiteDetailActivity", "게시물 수 업데이트 - 전체: " + posts.size() + ", 신규: " + newCount);
            } else {
                recentPostsList.clear();
                recentPostsAdapter.notifyDataSetChanged();
                tvTotalCount.setText("0");
                tvNewCount.setText("0");
            }
        });

        // 마지막 크롤링 날짜 LiveData로 관찰
        postDao.getLatestPostDateBySite(siteId).observe(this, latestDate -> {
            if (latestDate != null && !latestDate.isEmpty()) {
                // 게시물이 있으면 가장 최근 게시물의 날짜 표시
                tvLastCrawl.setText("마지막 수집: " + latestDate.substring(0, Math.min(10, latestDate.length())));
            } else if (site != null && site.getCreatedAt() != null) {
                // 게시물이 없으면 사이트 생성 날짜 사용
                tvLastCrawl.setText("마지막 수집: " + site.getCreatedAt().substring(0, Math.min(10, site.getCreatedAt().length())));
            } else {
                tvLastCrawl.setText("마지막 수집: 알 수 없음");
            }
        });
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("사이트 삭제")
                .setMessage("정말 이 사이트를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteSite())
                .setNegativeButton("취소", null)
                .show();
    }

    private void showEditSiteDialog() {
        if (site == null) {
            Toast.makeText(this, "사이트 정보를 불러오는 중입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_site, null);

        android.widget.EditText etSiteName = dialogView.findViewById(R.id.et_site_name);
        android.widget.EditText etSiteUrl = dialogView.findViewById(R.id.et_site_url);
        android.widget.EditText etSiteCategory = dialogView.findViewById(R.id.et_site_category);
        android.widget.EditText etSiteMemo = dialogView.findViewById(R.id.et_site_memo);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btn_save_site);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel_site);

        // 기존 데이터 로드
        etSiteName.setText(site.getName());
        etSiteUrl.setText(site.getUrl());
        etSiteCategory.setText(site.getCategory() != null ? site.getCategory() : "");
        etSiteMemo.setText(site.getDescription() != null ? site.getDescription() : "");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnSave.setOnClickListener(v -> {
            String name = etSiteName.getText().toString().trim();
            String url = etSiteUrl.getText().toString().trim();
            String category = CategoryUtils.normalizeCategory(
                etSiteCategory.getText().toString()
            );
            String memo = etSiteMemo.getText().toString().trim();

            if (name.isEmpty()) {
                etSiteName.setError("사이트 이름을 입력해주세요");
                return;
            }
            if (url.isEmpty()) {
                etSiteUrl.setError("사이트 URL을 입력해주세요");
                return;
            }

            updateSite(name, url, category, memo);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateSite(String name, String url, String category, String memo) {
        site.setName(name);
        site.setUrl(url);
        site.setCategory(category);
        site.setDescription(memo);

        String token = TokenManager.getBearerToken(this);
        ApiClient.getApiService().updateSite(token, siteId, site).enqueue(new Callback<Site>() {
            @Override
            public void onResponse(Call<Site> call, Response<Site> response) {
                if (response.isSuccessful() && response.body() != null) {
                    site = response.body();
                    updateUI();
                    Toast.makeText(SiteDetailActivity.this, "사이트가 수정되었습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SiteDetailActivity.this, "수정 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("SiteDetailActivity", "수정 실패: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Site> call, Throwable t) {
                Toast.makeText(SiteDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("SiteDetailActivity", "네트워크 오류", t);
            }
        });
    }

    private void deleteSite() {
        String token = TokenManager.getBearerToken(this);
        ApiClient.getApiService().deleteSite(token, siteId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SiteDetailActivity.this, "사이트가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);  // MyPage에서 갱신하도록 신호
                    finish();
                } else {
                    Toast.makeText(SiteDetailActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SiteDetailActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPostsFromServer() {
        String token = TokenManager.getBearerToken(this);
        ApiClient.getApiService().getPosts(
                token,
                1,              // page
                1000,           // page_size (충분히 큰 값)
                null,           // query
                siteId,         // site_id
                null,           // since
                null,           // until
                "created_at",   // order_by
                "desc"          // order
        ).enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostListResponse postListResponse = response.body();
                    if (postListResponse.getItems() != null) {
                        // 백그라운드 스레드에서 DB에 저장
                        new Thread(() -> {
                            postDao.upsertBySite(siteId, postListResponse.getItems());
                            Log.d("SiteDetailActivity", "서버에서 " + postListResponse.getItems().size() + "개 게시물 가져와서 DB에 저장 완료");
                        }).start();
                    }
                } else {
                    Log.e("SiteDetailActivity", "게시물 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                Log.e("SiteDetailActivity", "게시물 조회 네트워크 오류", t);
            }
        });
    }
}
