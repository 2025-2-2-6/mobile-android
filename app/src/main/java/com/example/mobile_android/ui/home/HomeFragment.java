package com.example.mobile_android.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.AddSiteActivity;
import com.example.mobile_android.R;
import com.example.mobile_android.Site;
import com.example.mobile_android.SiteAdapter;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView sitesRecyclerView;
    private SiteAdapter siteAdapter;
    private List<Site> siteList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup RecyclerView
        sitesRecyclerView = view.findViewById(R.id.rv_sites);
        sitesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        siteList = new ArrayList<>();
        // Add sample data
        siteList.add(new Site("컴퓨터학과 공지사항", "학과", "https://cs.university.ac.kr/notice", "2024-05-23 10:00:00", 3));
        siteList.add(new Site("씽굿 공모전", "공모전", "https://thinkgood.co.kr/contest", "2024-05-23 08:00:00", 1));
        siteList.add(new Site("학생지원팀 장학금", "학교", "https://university.ac.kr/scholarship", "2024-05-22 15:30:00", 0));

        siteAdapter = new SiteAdapter(siteList);
        sitesRecyclerView.setAdapter(siteAdapter);

        Button addSiteButton = view.findViewById(R.id.add_site_button);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });
    }
}
