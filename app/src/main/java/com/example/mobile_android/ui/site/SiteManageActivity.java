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
import java.util.List;

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
    private TextView btnDeleteMode;
    private CheckBox cbSelectAll;

    // 🔹 고정 카테고리 목록
    private static final String CATEGORY_ALL = "전체";
    private static final String CATEGORY_SCHOOL = "학교 공지";
    private static final String CATEGORY_SCHOLARSHIP = "장학금/지원금";
    private static final String CATEGORY_JOB = "채용/인턴십";
    private static final String CATEGORY_CONTEST = "공모전/대외활동";
    private static final String CATEGORY_DISCOUNT = "할인/혜택";
    private static final String CATEGORY_EVENT = "이벤트";
    private static final String CATEGORY_NEWS = "뉴스";
    private static final String CATEGORY_WEATHER = "날씨/교통";
    private static final String CATEGORY_OTHERS = "기타";

    private static final String[] FIXED_CATEGORIES = new String[] {
            CATEGORY_ALL,
            CATEGORY_SCHOOL,
            CATEGORY_SCHOLARSHIP,
            CATEGORY_JOB,
            CATEGORY_CONTEST,
            CATEGORY_DISCOUNT,
            CATEGORY_EVENT,
            CATEGORY_NEWS,
            CATEGORY_WEATHER,
            CATEGORY_OTHERS
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_manage);

        // 헤더 뒤로가기
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

        // 상세 보기
        siteAdapter.setOnItemClickListener(site -> {
            SiteDetailLauncher.launch(this, site);
        });

        // 개별 삭제
        siteAdapter.setOnDeleteListener(this::deleteSingleSite);

        btnDeleteMode = findViewById(R.id.btn_delete_mode);
        cbSelectAll = findViewById(R.id.cb_select_all);

        // 초기 상태
        btnDeleteMode.setText("삭제");
        cbSelectAll.setVisibility(View.GONE);
        cbSelectAll.setChecked(false);

        // 🔹 삭제 / 삭제 | 취소 버튼
        btnDeleteMode.setOnClickListener(v -> {
            if (!siteAdapter.isSelectionMode()) {
                // 선택 모드 켜기
                siteAdapter.setSelectionMode(true);
                cbSelectAll.setVisibility(View.VISIBLE);
                cbSelectAll.setChecked(false);
                btnDeleteMode.setText("삭제 | 취소");
            } else {
                // 이미 선택 모드
                List<Site> selected = siteAdapter.getSelectedSites();
                if (selected.isEmpty()) {
                    // 아무것도 선택 안 되어 있으면 → 취소
                    siteAdapter.setSelectionMode(false);
                    cbSelectAll.setVisibility(View.GONE);
                    cbSelectAll.setChecked(false);
                    btnDeleteMode.setText("삭제");
                    Toast.makeText(this, "삭제를 취소했어요.", Toast.LENGTH_SHORT).show();
                } else {
                    // 선택된 것들 삭제
                    deleteSites(selected);
                }
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

    // ----------------- 카테고리 매핑 -----------------
    /** DB에 뭐가 들어있든 화면에서 쓰는 고정 카테고리로 바꿔주는 함수 */
    private String normalizeCategory(String raw) {
        if (raw == null) return CATEGORY_OTHERS;
        String t = raw.trim();

        // 학교 공지 계열
        if (t.equals("학사공지") || t.equals("학교 공지")) {
            return CATEGORY_SCHOOL;
        }

        // 장학금/지원금 계열
        if (t.equals("장학금") || t.equals("장학금/지원금") || t.contains("장학") || t.contains("지원금")) {
            return CATEGORY_SCHOLARSHIP;
        }

        // 채용/인턴십 계열
        if (t.equals("채용") || t.equals("채용/인턴십") || t.contains("인턴") || t.contains("채용공고")) {
            return CATEGORY_JOB;
        }

        // 공모전/대외활동 계열
        if (t.equals("공모전") || t.equals("공모전/대외활동") || t.contains("대외활동") || t.contains("서포터즈")) {
            return CATEGORY_CONTEST;
        }

        // 할인/혜택
        if (t.equals("할인") || t.equals("혜택") || t.equals("할인/혜택") || t.contains("쿠폰") || t.contains("포인트")) {
            return CATEGORY_DISCOUNT;
        }

        // 이벤트
        if (t.equals("행사") || t.equals("이벤트") || t.contains("티켓오픈") || t.contains("이벤트")) {
            return CATEGORY_EVENT;
        }

        // 뉴스
        if (t.equals("뉴스") || t.contains("뉴스")) {
            return CATEGORY_NEWS;
        }

        // 날씨/교통
        if (t.equals("날씨") || t.equals("교통") || t.equals("날씨/교통") || t.contains("기상") || t.contains("교통")) {
            return CATEGORY_WEATHER;
        }

        // 그 외 다 기타
        return CATEGORY_OTHERS;
    }

    private int countForCategory(String label) {
        if (CATEGORY_ALL.equals(label)) {
            return allSites.size();
        }

        int cnt = 0;
        for (Site s : allSites) {
            String norm = normalizeCategory(s.getCategory());
            if (label.equals(norm)) {
                cnt++;
            }
        }
        return cnt;
    }

    // ----------------- 데이터 로딩 -----------------
    private void loadSites() {
        ApiClient.getApiService().getSites().enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    allSites.clear();
                    allSites.addAll(response.body());

                    applyFilter(CATEGORY_ALL);
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

    // ----------------- 태그 칩 -----------------
    private void setupTagChips() {
        chipContainer.removeAllViews();

        boolean first = true;
        for (String label : FIXED_CATEGORIES) {
            int count = countForCategory(label);
            // 전체는 무조건 표시, 나머지는 개수 0이면 안 보여도 됨
            if (!CATEGORY_ALL.equals(label) && count == 0) continue;

            addChip(label, count, first);
            first = false;
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

        if (CATEGORY_ALL.equals(category)) {
            displayedSites.addAll(allSites);
        } else {
            for (Site site : allSites) {
                String norm = normalizeCategory(site.getCategory());
                if (category.equals(norm)) {
                    displayedSites.add(site);
                }
            }
        }

        // 필터 바꾸면 선택 모드 끄기 + 버튼 초기화
        siteAdapter.setSelectionMode(false);
        cbSelectAll.setVisibility(View.GONE);
        cbSelectAll.setChecked(false);
        btnDeleteMode.setText("삭제");

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
                    .enqueue(new retrofit2.Callback<Void>() {
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

        // 선택 모드 종료 + UI 초기화
        siteAdapter.setSelectionMode(false);
        cbSelectAll.setVisibility(View.GONE);
        cbSelectAll.setChecked(false);
        btnDeleteMode.setText("삭제");
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
