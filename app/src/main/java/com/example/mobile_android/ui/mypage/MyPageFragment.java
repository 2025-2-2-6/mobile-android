package com.example.mobile_android.ui.mypage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mobile_android.R;

public class MyPageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 🔹 실제로 마이페이지에 쓸 레이아웃 넣어줘
        //   "등록 사이트 관리"가 들어있는 레이아웃이 mypage_drawer면 그걸로,
        //   profile.xml 이면 R.layout.profile 로 바꿔도 됨.
        return inflater.inflate(R.layout.mypage_drawer, container, false);
        // return inflater.inflate(R.layout.profile, container, false);  // 이렇게 쓸 수도 있음
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔹 "등록 사이트 관리" 버튼 찾기
        TextView manageSite = view.findViewById(R.id.manage_site_button);

        // 🔹 클릭 시 검색 탭(= nav_search)으로 이동
        manageSite.setOnClickListener(v -> {
            NavHostFragment.findNavController(MyPageFragment.this)
                    .navigate(R.id.nav_search);  // mobile_navigation.xml 에서 본 id
        });
    }
}
