package com.example.mobile_android.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobile_android.R;
import com.example.mobile_android.util.NotificationPermissionHelper;

import java.util.List;
import com.example.mobile_android.data.local.NotificationEntity;

public class NotificationFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ImageButton btnGrantPermission;
    private TextView tvUnreadCount;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private NotificationAdapter notificationAdapter;
    private NotificationViewModel notificationViewModel;
    private List<NotificationEntity> notificationList = new java.util.ArrayList<>();

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
        View root = inflater.inflate(R.layout.fragment_notification, container, false);
        tvUnreadCount = root.findViewById(R.id.tv_notification_unread_count);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationViewModel = new androidx.lifecycle.ViewModelProvider(this).get(NotificationViewModel.class);
        notificationViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            tvUnreadCount.setText("읽지 않은 알림 " + count + "개");
        });
        notificationViewModel.getNotifications().observe(getViewLifecycleOwner(), list -> {
            notificationList.clear();
            notificationList.addAll(list);
            notificationAdapter.submitList(new java.util.ArrayList<>(notificationList));
        });
        initViews(view);
        setupPermissionButton();
        updateUIBasedOnPermission();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.notification_swipe_refresh);
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        btnGrantPermission = view.findViewById(R.id.btn_grant_permission);

        notificationAdapter = new NotificationAdapter(requireContext());
        recyclerView.setAdapter(notificationAdapter);
        notificationAdapter.setOnNotificationClickListener(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(com.example.mobile_android.data.local.NotificationEntity notification) {
                if (!notification.isRead()) {
                    notification.setRead(true);
                    // 리스트에서 해당 알림만 갱신
                    notificationAdapter.submitList(new java.util.ArrayList<>(notificationList));
                }
            }
            @Override
            public void onDeleteClick(com.example.mobile_android.data.local.NotificationEntity notification) {
                notificationList.remove(notification);
                notificationAdapter.submitList(new java.util.ArrayList<>(notificationList));
            }
        });
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

        // 권한이 없을 때만 툴바의 버튼 표시
        if (btnGrantPermission != null) {
            btnGrantPermission.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 설정 화면에서 돌아왔을 때 권한 상태 재확인
        updateUIBasedOnPermission();
    }
}
