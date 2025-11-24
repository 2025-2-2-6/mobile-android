package com.example.mobile_android.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.ui.site.AddSiteActivity;
import com.example.mobile_android.ui.site.SiteAdapter;
import com.example.mobile_android.ui.site.SiteDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SiteAdapter siteAdapter;

    // 전체 사이트 / 화면에 보이는 사이트 분리
    private final List<Site> allSites = new ArrayList<>();
    private final List<Site> displaySites = new ArrayList<>();

    private TextView tvTotalItems, tvNewItems, tvUpcomingItems, tvSiteCount;

    @Override
    public void onResume() {
        super.onResume();
        loadSiteList();  // AddSiteActivity → 돌아올 때 새로고침
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rv_sites);
        tvTotalItems = view.findViewById(R.id.tv_total_items);
        tvNewItems = view.findViewById(R.id.tv_new_items);
        tvUpcomingItems = view.findViewById(R.id.tv_upcoming_items);
        tvSiteCount = view.findViewById(R.id.tv_site_count);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 홈에서는 관리 액션 필요 없음 -> false
        siteAdapter = new SiteAdapter(displaySites, false);
        recyclerView.setAdapter(siteAdapter);

        // 상세보기 연결 (홈에서 아이템 클릭 -> 상세 페이지)
        siteAdapter.setOnItemClickListener(site -> {
            if (getActivity() == null) return;
            Intent intent = new Intent(getActivity(), SiteDetailActivity.class);
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_ID, site.getId());
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_NAME, site.getName());
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_URL, site.getUrl());
            intent.putExtra(SiteDetailActivity.EXTRA_SITE_DESCRIPTION, site.getDescription());
            startActivity(intent);
        });

        // 새 사이트 등록 버튼
        Button addSiteButton = view.findViewById(R.id.add_site_button);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });

        loadSiteList();
        return view;
    }

    // ----------------------
    // 사이트 목록 로딩
    // ----------------------
    private void loadSiteList() {
        ApiClient.getApiService().getSites().enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    allSites.clear();
                    allSites.addAll(response.body());

                    // 화면에는 최대 3개까지만 표시
                    displaySites.clear();
                    if (allSites.size() > 3) {
                        displaySites.addAll(allSites.subList(0, 3));
                    } else {
                        displaySites.addAll(allSites);
                    }
                    siteAdapter.notifyDataSetChanged();

                    updateSummary();
                } else {
                    Toast.makeText(getContext(), "사이트 목록 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e("HomeFragment", "Failed to load sites", t);
                Toast.makeText(getContext(), "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ----------------------
    // 요약 정보 업데이트 (전체 기준)
    // ----------------------
    private void updateSummary() {
        int totalSites = allSites.size();

        tvTotalItems.setText(String.valueOf(totalSites));
        tvSiteCount.setText(totalSites + "개");

        // 새 항목 / 다가오는 일정은 아직 백엔드 미구현 → 0으로 고정
        tvNewItems.setText("0");
        tvUpcomingItems.setText("0");
    }
}
