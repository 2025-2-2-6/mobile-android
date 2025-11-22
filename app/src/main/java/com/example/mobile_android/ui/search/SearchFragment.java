package com.example.mobile_android.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.SiteDao;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.ui.site.AddSiteActivity;
import com.example.mobile_android.ui.site.SiteAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvCategoryChips;
    private RecyclerView rvSites;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    private TextView tvSiteCount;
    private TextView tvSort;
    private FloatingActionButton fabAddSite;

    private SiteAdapter siteAdapter;
    private TagChipAdapter categoryAdapter;
    private List<Site> siteList = new ArrayList<>();
    private List<Site> filteredList = new ArrayList<>();
    private String currentCategory = "전체";
    private String currentSearchQuery = "";
    private SiteDao siteDao;
    private ExecutorService siteDbExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initSiteCache();
        setupCategoryChips();
        setupSiteList();
        setupListeners();
        observeSites();
        loadSites();
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        rvCategoryChips = view.findViewById(R.id.rv_category_chips);
        rvSites = view.findViewById(R.id.rv_sites);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        emptyView = view.findViewById(R.id.empty_view);
        tvSiteCount = view.findViewById(R.id.tv_site_count);
        tvSort = view.findViewById(R.id.tv_sort);
        fabAddSite = view.findViewById(R.id.fab_add_site);
    }

    private void setupCategoryChips() {
        rvCategoryChips.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvCategoryChips.addItemDecoration(new SpaceItemDecoration(dp(8)));

        List<TagChip> categories = new ArrayList<>();
        categories.add(new TagChip("전체", 0, true));
        categories.add(new TagChip("학교", 0, false));
        categories.add(new TagChip("장학금", 0, false));
        categories.add(new TagChip("공모전", 0, false));
        categories.add(new TagChip("취업", 0, false));
        categories.add(new TagChip("기타", 0, false));

        categoryAdapter = new TagChipAdapter(categories, (position, item) -> {
            currentCategory = item.label;
            for (int i = 0; i < categories.size(); i++) {
                categories.get(i).selected = (i == position);
            }
            categoryAdapter.notifyDataSetChanged();
            filterSites();
        });
        rvCategoryChips.setAdapter(categoryAdapter);
    }

    private void setupSiteList() {
        rvSites.setLayoutManager(new LinearLayoutManager(getContext()));
        siteAdapter = new SiteAdapter(filteredList);

        // 삭제 리스너
        siteAdapter.setOnDeleteListener(this::showDeleteConfirmDialog);

        // 수정 리스너
        siteAdapter.setOnEditListener(site -> {
            Intent intent = new Intent(requireContext(), AddSiteActivity.class);
            intent.putExtra("EDIT_MODE", true);
            intent.putExtra("SITE_ID", site.getId());
            intent.putExtra("SITE_NAME", site.getName());
            intent.putExtra("SITE_URL", site.getUrl());
            intent.putExtra("SITE_CATEGORY", site.getCategory());
            startActivity(intent);
        });

        rvSites.setAdapter(siteAdapter);
    }

    private void showDeleteConfirmDialog(Site site) {
        new AlertDialog.Builder(requireContext())
                .setTitle("사이트 삭제")
                .setMessage("'" + site.getName() + "' 사이트를 삭제하시겠습니까?\n수집된 게시물도 함께 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> deleteSite(site))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSite(Site site) {
        ApiClient.getApiService().deleteSite(site.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    siteList.remove(site);
                    filterSites();
                    Toast.makeText(requireContext(), "사이트가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "삭제 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(), "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // 검색
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                filterSites();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 새로고침
        swipeRefresh.setOnRefreshListener(this::loadSites);

        // 사이트 추가
        fabAddSite.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddSiteActivity.class);
            startActivity(intent);
        });

        // 정렬
        tvSort.setOnClickListener(v -> {
            // TODO: 정렬 옵션 다이얼로그
        });
    }

    private void loadSites() {
        swipeRefresh.setRefreshing(true);

        ApiClient.getApiService().getSites().enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Site> remoteSites = response.body();
                    siteDbExecutor.execute(() -> siteDao.replaceAll(remoteSites));
                } else {
                    Log.e("SearchFragment", "사이트 목록 로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Log.e("SearchFragment", "네트워크 오류", t);
            }
        });
    }

    private void filterSites() {
        filteredList.clear();

        for (Site site : siteList) {
            boolean matchesCategory = currentCategory.equals("전체") ||
                    (site.getCategory() != null && site.getCategory().equals(currentCategory));
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (site.getName() != null && site.getName().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                    (site.getUrl() != null && site.getUrl().toLowerCase().contains(currentSearchQuery.toLowerCase()));

            if (matchesCategory && matchesSearch) {
                filteredList.add(site);
            }
        }

        siteAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void initSiteCache() {
        AppDatabase database = AppDatabase.getInstance(requireContext());
        siteDao = database.siteDao();
        siteDbExecutor = Executors.newSingleThreadExecutor();
    }

    private void observeSites() {
        siteDao.observeAll().observe(getViewLifecycleOwner(), sites -> {
            siteList.clear();
            if (sites != null) {
                siteList.addAll(sites);
            }
            filterSites();
        });
    }

    private void updateUI() {
        tvSiteCount.setText(filteredList.size() + "개의 사이트");

        if (filteredList.isEmpty()) {
            rvSites.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rvSites.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSites();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (siteDbExecutor != null) {
            siteDbExecutor.shutdown();
        }
    }
}
