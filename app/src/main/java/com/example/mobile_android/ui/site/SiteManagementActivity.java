package com.example.mobile_android.ui.site;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.mobile_android.R;
import com.example.mobile_android.ui.search.SearchFragment;

/**
 * 사이트 관리 화면 (모달 다이얼로그 스타일)
 *
 * 주요 기능:
 * - SearchFragment를 호스팅하는 모달 Activity
 * - 가운데 정렬된 모달 창
 * - 어두운 배경 오버레이 (클릭 시 닫힘)
 * - 부드러운 슬라이드 애니메이션
 */
public class SiteManagementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_management);

        // 액션바 숨기기
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 배경 클릭 시 닫기 (모달 외부 클릭)
        findViewById(android.R.id.content).setOnClickListener(v -> {
            // 모달 창 외부를 클릭한 경우에만 닫기
            finish();
        });

        // 모달 창 내부 클릭 시 이벤트 전파 차단
        findViewById(R.id.fragment_container).setOnClickListener(v -> {
            // 아무 동작도 하지 않음 (이벤트 전파 차단)
        });

        // 닫기 버튼 설정
        ImageButton btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());

        // SearchFragment 추가
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new SearchFragment());
            transaction.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 버튼도 정상 동작
        super.onBackPressed();
    }
}
