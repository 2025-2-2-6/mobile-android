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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private SiteAdapter siteAdapter;
    private List<Site> siteList = new ArrayList<>();

    private TextView tvTotalItems, tvNewItems, tvUpcomingItems, tvSiteCount;
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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- 어댑터 생성 ---
        siteAdapter = new SiteAdapter(siteList);

        // 🔥 삭제 버튼 리스너 추가
        siteAdapter.setOnDeleteListener(site -> {
            deleteSite(site);
        });

        recyclerView.setAdapter(siteAdapter);

        // --- 새 사이트 등록 버튼 ---
        Button addSiteButton = view.findViewById(R.id.add_site_button);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });

        // --- 사이트 목록 로드 ---
        loadSiteList();

        return view;
    }

    // ----------------------
    // ★ 사이트 목록 로딩
    // ----------------------
    private void loadSiteList() {
        ApiClient.getApiService().getSites().enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    siteList.clear();
                    siteList.addAll(response.body());
                    siteAdapter.notifyDataSetChanged();

                    // 요약 UI 갱신
                    updateSummary();
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                Log.e("HomeFragment", "Failed to load sites", t);
            }
        });
    }

    // ----------------------
    // ★ 요약 정보 업데이트
    // ----------------------
    private void updateSummary() {
        int totalSites = siteList.size();

        tvTotalItems.setText(String.valueOf(totalSites));
        tvSiteCount.setText(totalSites + "개");

        // 새 항목 → 현재는 백엔드 구현 전입니다.
        tvNewItems.setText("0");

        // 다가오는 일정 → 추후 기능
        tvUpcomingItems.setText("0");
    }

    // ------------------------
    // ★ 사이트 삭제 기능
    // ------------------------
    private void deleteSite(Site site) {

        ApiClient.getApiService().deleteSite(site.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {

                        if (response.isSuccessful()) {

                            siteList.remove(site);
                            siteAdapter.notifyDataSetChanged();

                            updateSummary();

                            Toast.makeText(getContext(), "사이트 삭제됨", Toast.LENGTH_SHORT).show();

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
}
