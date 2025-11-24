package com.example.mobile_android.ui.site;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.network.ApiClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SiteManageActivity extends AppCompatActivity {

    private RecyclerView rvSites;
    private SiteAdapter siteAdapter;

    private final List<Site> allSites = new ArrayList<>();
    private final List<Site> displayedSites = new ArrayList<>();

    private LinearLayout chipContainer;

    private TextView tvTotalItems, tvNewItems, tvUpcomingItems, tvSiteCount;
    private TextView btnDeleteMode;   // "삭제" / "삭제 실행" 토글
    private CheckBox cbSelectAll;     // 전체 체크박스

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_manage);

        // 헤더 뒤로가기 (텍스트 ←)
        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTotalItems = findViewById(R.id.tv_total_items);
        tvNewItems = findViewById(R.id.tv_new_items);
        tvUpcomingItems = findViewById(R.id.tv_upcoming_items);
        tvSiteCount = findViewById(R.id.tv_site_count_manage);

        chipContainer = findViewById(R.id.chip_container);
        rvSites = findViewById(R.id.rv_sites_manage);
        rvSites.setLayoutManager(new LinearLayoutManager(this));

        siteAdapter = new SiteAdapter(displayedSites, true);
        rvSites.setAdapter(siteAdapter);

        // 상세 보기 (연필 or 카드 클릭 - 선택 모드 아닐 때)
        siteAdapter.setOnItemClickListener(site -> {
            SiteDetailLauncher.launch(this, site);
        });

        // 개별 삭제 (휴지통)
        siteAdapter.setOnDeleteListener(this::deleteSingleSite);

        // 삭제 모드 버튼 + 전체 체크박스
        btnDeleteMode = findViewById(R.id.btn_delete_mode);
        cbSelectAll = findViewById(R.id.cb_select_all);

        btnDeleteMode.setOnClickListener(v -> {
            if (!siteAdapter.isSelectionMode()) {
                // 선택 모드 켜기
                siteAdapter.setSelectionMode(true);
                btnDeleteMode.setText("삭제 실행");
                cbSelectAll.setVisibility(View.VISIBLE);
                cbSelectAll.setChecked(false);
            } else {
                // 실제 삭제 실행
                List<Site> selected = siteAdapter.getSelectedSites();
                if (selected.isEmpty()) {
                    Toast.makeText(this, "삭제할 사이트를 선택하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                deleteSites(selected);
            }
        });

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!siteAdapter.isSelectionMode()) return;
            if (isChecked) {
                siteAdapter.selectAll();
            } else {
                siteAdapter.clearSelection();
            }
        });

        loadSites();
    }

    // ----------------- 데이터 로딩 -----------------
    private void loadSites() {
        ApiClient.getApiService().getSites().enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    allSites.clear();
                    allSites.addAll(response.body());

                    applyFilter("전체");
                    setupTagChips();
                    updateSummary();

                } else {
                    Toast.makeText(SiteManageActivity.this,
                            "사이트 목록 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                Toast.makeText(SiteManageActivity.this,
                        "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ----------------- 요약/카운트 -----------------
    private void updateSummary() {
        int total = allSites.size();
        tvTotalItems.setText(String.valueOf(total));
        tvSiteCount.setText(displayedSites.size() + "개");

        tvNewItems.setText("0");
        tvUpcomingItems.setText("0");
    }

    // ----------------- 카테고리 태그 칩 -----------------
    private void setupTagChips() {
        chipContainer.removeAllViews();

        // "전체" 칩
        addChip("전체", allSites.size(), true);

        // category 수집
        Set<String> categories = new LinkedHashSet<>();
        for (Site site : allSites) {
            if (site.getCategory() != null && !site.getCategory().isEmpty()) {
                categories.add(site.getCategory());
            }
        }

        for (String c : categories) {
            int count = 0;
            for (Site s : allSites) {
                if (c.equals(s.getCategory())) count++;
            }
            addChip(c, count, false);
        }
    }

    private void addChip(String label, int count, boolean isDefaultSelected) {
        View chipView = LayoutInflater.from(this)
                .inflate(R.layout.item_tag_chip, chipContainer, false);

        TextView tvLabel = chipView.findViewById(R.id.tvLabel);
        TextView tvCount = chipView.findViewById(R.id.tvCount);

        tvLabel.setText(label);
        tvCount.setText(String.valueOf(count));

        chipView.setSelected(isDefaultSelected);

        chipView.setOnClickListener(v -> {
            for (int i = 0; i < chipContainer.getChildCount(); i++) {
                chipContainer.getChildAt(i).setSelected(false);
            }
            v.setSelected(true);
            applyFilter(label);
        });

        chipContainer.addView(chipView);
    }

    private void applyFilter(String category) {
        displayedSites.clear();

        if ("전체".equals(category)) {
            displayedSites.addAll(allSites);
        } else {
            for (Site site : allSites) {
                if (category.equals(site.getCategory())) {
                    displayedSites.add(site);
                }
            }
        }

        siteAdapter.setSelectionMode(false);
        btnDeleteMode.setText("삭제");
        cbSelectAll.setVisibility(View.GONE);

        siteAdapter.notifyDataSetChanged();
        updateSummary();
    }

    // ----------------- 삭제 로직 -----------------
    private void deleteSingleSite(Site site) {
        List<Site> list = new ArrayList<>();
        list.add(site);
        deleteSites(list);
    }

    private void deleteSites(List<Site> sites) {
        if (sites.isEmpty()) return;

        for (Site s : sites) {
            ApiClient.getApiService().deleteSite(s.getId())
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            loadSites();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            // 하나 실패해도 나머지는 계속
                        }
                    });
        }

        Toast.makeText(this, "삭제 요청 완료", Toast.LENGTH_SHORT).show();
        // 삭제 후 선택 모드 종료
        siteAdapter.setSelectionMode(false);
        btnDeleteMode.setText("삭제");
        cbSelectAll.setVisibility(View.GONE);
    }

    // ----------------- 상세보기 런처 -----------------
    private static class SiteDetailLauncher {
        static void launch(AppCompatActivity activity, Site site) {
            android.content.Intent intent =
                    new android.content.Intent(activity, SiteDetailActivity.class);
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_ID, site.getId());
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_NAME, site.getName());
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_URL, site.getUrl());
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_DESCRIPTION, site.getDescription());
            activity.startActivity(intent);
        }
    }
}
