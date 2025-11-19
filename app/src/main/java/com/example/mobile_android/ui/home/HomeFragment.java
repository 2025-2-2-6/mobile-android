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
        // 이 화면의 레이아웃 파일을 지정합니다.
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // RecyclerView를 레이아웃에서 찾아 초기화합니다.
        sitesRecyclerView = view.findViewById(R.id.rv_sites);
        sitesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // [복원] 예시 데이터를 저장할 리스트를 생성합니다.
        siteList = new ArrayList<>();


        // 어댑터를 생성하고 RecyclerView에 연결합니다.
        siteAdapter = new SiteAdapter(siteList);
        sitesRecyclerView.setAdapter(siteAdapter);

        // "새 사이트 등록하기" 버튼을 찾아 클릭 이벤트를 설정합니다.
        Button addSiteButton = view.findViewById(R.id.add_site_button);
        addSiteButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddSiteActivity.class);
            startActivity(intent);
        });
    }
}
