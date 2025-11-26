package com.example.mobile_android.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.data.local.SiteDao;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.ui.site.AddSiteActivity;
import com.example.mobile_android.ui.site.SiteAdapter;
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

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SiteAdapter siteAdapter;
    private List<Site> allSitesList = new ArrayList<>(); // 전체 사이트 목록
    private List<Site> filteredSitesList = new ArrayList<>(); // 필터링된 사이트 목록
    private PostDao postDao;
    private SiteDao siteDao;
    private ExecutorService executor;

    private TextView tvTotalItems, tvNewItems, tvUpcomingItems, tvSiteCount;
    private View layoutEmptySites;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private String searchQuery = "";
    private String categoryFilter = ""; // 선택된 카테고리 필터
    private List<String> currentCategories = new ArrayList<>(); // 현재 표시중인 카테고리 목록 (중복 업데이트 방지용)

    @Override
    public void onResume() {
        super.onResume();
        loadSiteList();  // 🔥 AddSiteActivity → 뒤로 오면 자동 새로고침
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // --- UI 요소 찾기 ---
        recyclerView = view.findViewById(R.id.rv_sites);
        tvTotalItems = view.findViewById(R.id.tv_total_items);
        tvNewItems = view.findViewById(R.id.tv_new_items);
        tvUpcomingItems = view.findViewById(R.id.tv_upcoming_items);
        tvSiteCount = view.findViewById(R.id.tv_site_count);
        layoutEmptySites = view.findViewById(R.id.layout_empty_sites);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- 데이터베이스 초기화 ---
        postDao = AppDatabase.getInstance(requireContext()).postDao();
        siteDao = AppDatabase.getInstance(requireContext()).siteDao();
        executor = Executors.newSingleThreadExecutor();

        // --- 어댑터 생성 ---
        siteAdapter = new SiteAdapter(filteredSitesList);

        // 🔥 삭제 버튼 리스너 추가
        siteAdapter.setOnDeleteListener(site -> {
            deleteSite(site);
        });

        recyclerView.setAdapter(siteAdapter);

        // --- SwipeRefreshLayout 설정 ---
        setupSwipeRefresh();

        // --- 검색 기능 설정 ---
        setupSearch();

        // --- 카테고리 필터 설정 ---
        setupCategoryFilter(view);

        // --- 새 사이트 등록 버튼 ---
        Button addSiteButton = view.findViewById(R.id.add_site_button);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });

        // --- Room DB에서 사이트 목록 관찰 (오프라인 대응) ---
        observeSites();

        // --- 서버에서 사이트 목록 로드 및 DB 동기화 ---
        loadSiteList();

        // --- 새 게시물 개수 관찰 ---
        observeNewPostCount();

        // --- 알림 설정된 일정 개수 관찰 ---
        observeUpcomingEventCount();

        return view;
    }

    // ----------------------
    // ★ SwipeRefreshLayout 설정
    // ----------------------
    private void setupSwipeRefresh() {
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    getResources().getColor(android.R.color.holo_blue_bright),
                    getResources().getColor(android.R.color.holo_green_light),
                    getResources().getColor(android.R.color.holo_orange_light)
            );
            swipeRefresh.setOnRefreshListener(() -> {
                loadSiteList();
            });
        }
    }

    // ----------------------
    // ★ 검색 기능 설정
    // ----------------------
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterSites();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.clearFocus();
        });
    }

    // ----------------------
    // ★ 카테고리 필터 설정
    // ----------------------
    private void setupCategoryFilter(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_category_filters);
        if (chipGroup == null) return;

        // 칩 클릭 리스너 설정
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
            filterSites();
        });

        // DB에서 고유 카테고리 목록을 관찰하고 동적으로 칩 생성
        siteDao.observeDistinctCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                updateCategoryChips(chipGroup, categories);
            }
        });
    }

    /**
     * 카테고리 칩을 동적으로 업데이트합니다.
     * DB의 카테고리가 변경될 때만 칩을 재생성하여 성능을 최적화합니다.
     */
    private void updateCategoryChips(ChipGroup chipGroup, List<String> categories) {
        // 카테고리 목록이 변경되지 않았으면 스킵
        if (categories.equals(currentCategories)) {
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

            // InstagramFilterChip 스타일 프로그래밍 방식으로 적용
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

    // ----------------------
    // ★ 사이트 필터링
    // ----------------------
    private void filterSites() {
        Log.d("HomeFragment", "🔄 filterSites 시작 - allSites: " + allSitesList.size());
        filteredSitesList.clear();

        filteredSitesList.addAll(
                allSitesList.stream()
                        .filter(site -> {
                            // 카테고리 필터링
                            if (!categoryFilter.isEmpty()) {
                                String siteCategory = site.getCategory() != null ? site.getCategory() : "";
                                if (!siteCategory.equals(categoryFilter)) {
                                    return false;
                                }
                            }

                            // 검색어 필터링
                            if (!searchQuery.isEmpty()) {
                                String lowerQuery = searchQuery.toLowerCase();
                                String name = site.getName() != null ? site.getName().toLowerCase() : "";
                                String url = site.getUrl() != null ? site.getUrl().toLowerCase() : "";
                                String category = site.getCategory() != null ? site.getCategory().toLowerCase() : "";
                                return name.contains(lowerQuery) || url.contains(lowerQuery) || category.contains(lowerQuery);
                            }

                            return true;
                        })
                        .collect(Collectors.toList())
        );

        Log.d("HomeFragment", "✅ 필터링 완료 - filteredSites: " + filteredSitesList.size());
        Log.d("HomeFragment", "🔄 Adapter.notifyDataSetChanged() 호출");
        siteAdapter.notifyDataSetChanged();
        Log.d("HomeFragment", "📊 updateSummary() 호출");
        updateSummary();
    }

    // ----------------------
    // ★ Room DB에서 사이트 목록 관찰 (오프라인 대응)
    // ----------------------
    private void observeSites() {
        Log.d("HomeFragment", "🔍 LiveData observe 시작");
        siteDao.observeAll().observe(getViewLifecycleOwner(), sites -> {
            Log.d("HomeFragment", "📢 LiveData 변경 감지! Sites: " + (sites != null ? sites.size() : "null") + "개");
            if (sites != null) {
                allSitesList.clear();
                allSitesList.addAll(sites);
                Log.d("HomeFragment", "🔄 filterSites() 호출");
                filterSites();
            }
        });
    }

    // ----------------------
    // ★ 서버에서 사이트 목록 로딩 및 DB 동기화
    // ----------------------
    private void loadSiteList() {
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        String token = TokenManager.getBearerToken(requireContext());
        Log.d("HomeFragment", "📤 getSites 호출 - Token: " + (token != null ? token.substring(0, Math.min(30, token.length())) + "..." : "NULL"));
        ApiClient.getApiService().getSites(token).enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                Log.d("HomeFragment", "📥 getSites 응답 - Code: " + response.code());
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                if (response.isSuccessful() && response.body() != null && isAdded()) {
                    List<Site> sites = response.body();
                    Log.d("HomeFragment", "✅ Sites 받음: " + sites.size() + "개");
                    // Executor가 종료되지 않았는지 확인
                    if (executor != null && !executor.isShutdown()) {
                        executor.execute(() -> {
                            Log.d("HomeFragment", "💾 DB에 저장 시작: " + sites.size() + "개");
                            siteDao.replaceAll(sites);
                            Log.d("HomeFragment", "✅ DB 저장 완료");
                        });
                    } else {
                        Log.e("HomeFragment", "❌ Executor가 종료됨!");
                    }
                } else {
                    Log.e("HomeFragment", "❌ 응답 실패 - Success: " + response.isSuccessful() + ", Body: " + (response.body() != null) + ", Added: " + isAdded());
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                if (swipeRefresh != null) {
                    swipeRefresh.setRefreshing(false);
                }
                Log.e("HomeFragment", "Failed to load sites from server (offline mode OK)", t);
                // 오프라인 상태에서는 Room DB의 캐시된 데이터를 사용
            }
        });
    }

    // ----------------------
    // ★ 요약 정보 업데이트
    // ----------------------
    private void updateSummary() {
        int totalSites = filteredSitesList.size();

        tvTotalItems.setText(String.valueOf(totalSites));
        tvSiteCount.setText(totalSites + "개");

        // 빈 상태 UI 표시/숨김
        if (totalSites == 0) {
            layoutEmptySites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmptySites.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ----------------------
    // ★ 새 게시물 개수 관찰 (is_new = true인 Post 개수)
    // ----------------------
    private void observeNewPostCount() {
        // is_new는 DB 필드, 실제 "새 게시물"은 Post.isActuallyNew()로 판단
        // 여기서는 getAllPostsForNewFilter로 전체 가져와서 클라이언트에서 필터링
        postDao.getAllPostsForNewFilter().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                long count = posts.stream().filter(post -> post.isActuallyNew()).count();
                tvNewItems.setText(String.valueOf(count));
            } else {
                tvNewItems.setText("0");
            }
        });
    }

    // ----------------------
    // ★ 알림 설정된 일정 개수 관찰 (notify_enabled = true인 CalendarEvent 개수)
    // ----------------------
    private void observeUpcomingEventCount() {
        AppDatabase.getInstance(requireContext())
                .calendarEventDao()
                .getAllEvents()
                .observe(getViewLifecycleOwner(), events -> {
                    if (events != null) {
                        long count = events.stream()
                                .filter(event -> event.isNotifyEnabled())
                                .count();
                        tvUpcomingItems.setText(String.valueOf(count));
                    } else {
                        tvUpcomingItems.setText("0");
                    }
                });
    }

    // ------------------------
    // ★ 사이트 삭제 기능
    // ------------------------
    private void deleteSite(Site site) {
        String token = TokenManager.getBearerToken(requireContext());
        ApiClient.getApiService().deleteSite(token, site.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "사이트 삭제됨", Toast.LENGTH_SHORT).show();
                            // 서버에서 삭제 성공 시 전체 목록 다시 로드하여 DB 동기화
                            loadSiteList();
                        } else {
                            Toast.makeText(getContext(),
                                    "삭제 실패 (" + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
