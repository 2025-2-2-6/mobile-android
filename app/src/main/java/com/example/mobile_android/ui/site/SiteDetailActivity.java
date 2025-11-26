package com.example.mobile_android.ui.site;

import android.content.Intent;
import android.os.Bundle;
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
    private ImageButton btnEdit;
    private ImageButton btnDelete;
    private ImageView ivSiteIcon;
    private TextView tvSiteName;
    private TextView tvCategory;
    private TextView tvUrl;
    private TextView tvLastCrawl;
    private TextView tvTotalCount;
    private TextView tvNewCount;
    private ImageView ivCrawlingStatus;
    private RecyclerView rvRecentPosts;
    private TextView tvViewAll;

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

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        ivSiteIcon = findViewById(R.id.iv_site_icon);
        tvSiteName = findViewById(R.id.tv_site_name);
        tvCategory = findViewById(R.id.tv_category);
        tvUrl = findViewById(R.id.tv_url);
        tvLastCrawl = findViewById(R.id.tv_last_crawl);
        tvTotalCount = findViewById(R.id.tv_total_count);
        tvNewCount = findViewById(R.id.tv_new_count);
        ivCrawlingStatus = findViewById(R.id.iv_crawling_status);
        rvRecentPosts = findViewById(R.id.rv_recent_posts);
        tvViewAll = findViewById(R.id.tv_view_all);

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

    private void loadSiteDetails() {
        String token = TokenManager.getBearerToken(this);
        ApiClient.getApiService().getSiteById(token, siteId).enqueue(new Callback<Site>() {
            @Override
            public void onResponse(Call<Site> call, Response<Site> response) {
                if (response.isSuccessful() && response.body() != null) {
                    site = response.body();
                    updateUI();
                } else {
                    // API 실패 시 임시 데이터
                    Toast.makeText(SiteDetailActivity.this, "사이트 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    site = createDummySite();
                    updateUI();
                }
            }

            @Override
            public void onFailure(Call<Site> call, Throwable t) {
                Toast.makeText(SiteDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                site = createDummySite();
                updateUI();
            }
        });
    }

    private Site createDummySite() {
        Site dummySite = new Site();
        dummySite.setId(siteId);
        dummySite.setName("창업진흥원");
        dummySite.setCategory("창업");
        dummySite.setUrl("https://k-startup.go.kr");
        return dummySite;
    }

    private void updateUI() {
        if (site == null) return;

        tvSiteName.setText(site.getName() != null ? site.getName() : "이름 없음");
        tvCategory.setText(site.getCategory() != null ? site.getCategory() : "기타");
        tvUrl.setText(site.getUrl() != null ? site.getUrl().replace("https://", "").replace("http://", "") : "");

        // 마지막 수집 시간 (TODO: Site 모델에 lastCrawlDate 필드 추가 필요)
        tvLastCrawl.setText("마지막 수집: " + (site.getCreatedAt() != null ? site.getCreatedAt().substring(0, 10) : "알 수 없음"));

        // 통계는 별도 API 호출 또는 Site 모델에 포함
        tvTotalCount.setText("0");  // TODO: API 연동
        tvNewCount.setText("0");    // TODO: API 연동
    }

    private void loadRecentPosts() {
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
            } else {
                recentPostsList.clear();
                recentPostsAdapter.notifyDataSetChanged();
                tvTotalCount.setText("0");
                tvNewCount.setText("0");
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
        android.widget.Button btnSave = dialogView.findViewById(R.id.btn_save_site);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btn_cancel_site);

        // 기존 데이터 로드
        etSiteName.setText(site.getName());
        etSiteUrl.setText(site.getUrl());
        etSiteCategory.setText(site.getCategory() != null ? site.getCategory() : "");

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

            if (name.isEmpty()) {
                etSiteName.setError("사이트 이름을 입력해주세요");
                return;
            }
            if (url.isEmpty()) {
                etSiteUrl.setError("사이트 URL을 입력해주세요");
                return;
            }

            updateSite(name, url, category);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateSite(String name, String url, String category) {
        site.setName(name);
        site.setUrl(url);
        site.setCategory(category);

        String token = TokenManager.getBearerToken(this);
        ApiClient.getApiService().updateSite(token, siteId, site).enqueue(new Callback<Site>() {
            @Override
            public void onResponse(Call<Site> call, Response<Site> response) {
                if (response.isSuccessful() && response.body() != null) {
                    site = response.body();
                    updateUI();
                    Toast.makeText(SiteDetailActivity.this, "사이트가 수정되었습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SiteDetailActivity.this, "수정 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Site> call, Throwable t) {
                Toast.makeText(SiteDetailActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
}
