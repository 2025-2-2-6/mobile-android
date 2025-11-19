package com.example.mobile_android.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.rv_sites);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        siteAdapter = new SiteAdapter(siteList);
        recyclerView.setAdapter(siteAdapter);
        tvTotalItems = view.findViewById(R.id.tv_total_items);
        tvNewItems = view.findViewById(R.id.tv_new_items);
        tvUpcomingItems = view.findViewById(R.id.tv_upcoming_items);
        tvSiteCount = view.findViewById(R.id.tv_site_count);

        // ★★★ 새 사이트 등록 버튼 클릭 리스너 추가 ★★★
        Button addSiteButton = view.findViewById(R.id.add_site_button);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });
        loadSiteList();

        return view;
    }

    private void loadSiteList() {
        ApiClient.getApiService().getSites().enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    siteList.clear();
                    siteList.addAll(response.body());
                    siteAdapter.notifyDataSetChanged();

                    // ★ 요약 정보 업데이트 ★
                    updateSummary();
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                Log.e("HomeFragment", "Failed to load sites", t);
            }
        });
    }
    private void updateSummary() {
        int totalSites = siteList.size();

        tvTotalItems.setText(String.valueOf(totalSites));
        tvSiteCount.setText(totalSites + "개");

        // 새 항목 (백엔드에서 new_posts 받아올 경우 사용)
        int newPosts = 0;
//        for (Site site : siteList) {
//            if (site.getNewPosts() != null) {
//                newPosts += site.getNewPosts();
//            }
//        }
        tvNewItems.setText(String.valueOf(newPosts));

        // 다가오는 일정(백엔드 구현 전까지 0으로 처리)
        tvUpcomingItems.setText("0");
    }

}
