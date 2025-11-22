package com.example.mobile_android.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobile_android.R;
import com.example.mobile_android.util.NotificationPermissionHelper;
import com.google.android.material.button.MaterialButton;

public class NotificationFragment extends Fragment {

    private LinearLayout layoutPermissionRequired;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private MaterialButton btnGrantPermission;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 알림 권한 요청 런처 등록
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // 권한 결과에 따라 UI 업데이트
                    updateUIBasedOnPermission();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupPermissionButton();
        updateUIBasedOnPermission();
    }

    private void initViews(View view) {
        layoutPermissionRequired = view.findViewById(R.id.layout_permission_required);
        swipeRefreshLayout = view.findViewById(R.id.notification_swipe_refresh);
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        btnGrantPermission = view.findViewById(R.id.btn_grant_permission);
    }

    private void setupPermissionButton() {
        btnGrantPermission.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 권한 요청
                NotificationPermissionHelper.requestNotificationPermission(notificationPermissionLauncher);
            } else {
                // Android 12 이하는 설정 화면으로 이동
                NotificationPermissionHelper.openNotificationSettings(requireContext());
            }
        });
    }

    private void updateUIBasedOnPermission() {
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(requireContext());

        if (hasPermission) {
            // 권한이 있으면 알림 리스트 표시
            layoutPermissionRequired.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.VISIBLE);
            // TODO: 알림 목록 로드
        } else {
            // 권한이 없으면 권한 요청 화면 표시
            layoutPermissionRequired.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 설정 화면에서 돌아왔을 때 권한 상태 재확인
        updateUIBasedOnPermission();
    }
}
