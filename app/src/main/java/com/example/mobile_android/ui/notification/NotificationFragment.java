package com.example.mobile_android.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobile_android.R;
import com.example.mobile_android.util.NotificationPermissionHelper;
import com.example.mobile_android.util.NotificationTopicManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.content.Intent;
import com.example.mobile_android.fcm.NotificationType;
import com.example.mobile_android.fcm.CrawlStatus;

import java.util.List;
import com.example.mobile_android.data.local.NotificationEntity;

public class NotificationFragment extends Fragment {

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NEW_POST_NOTIFICATION = "new_post_notification";
    private static final String KEY_CALENDAR_NOTIFICATION = "calendar_notification";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView tvUnreadCount;
    private com.google.android.material.card.MaterialCardView cardPermissionTip;
    private android.widget.Button btnGoToSettings;

    private NotificationAdapter notificationAdapter;
    private NotificationViewModel notificationViewModel;
    private List<NotificationEntity> notificationList = new java.util.ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // 권한 안내 카드 설정
        cardPermissionTip = view.findViewById(R.id.card_permission_tip);
        btnGoToSettings = view.findViewById(R.id.btn_go_to_settings);

        btnGoToSettings.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.mobile_android.MainActivity) {
                ((com.example.mobile_android.MainActivity) getActivity()).navigateToMyPage();
            }
        });

        updatePermissionTipVisibility();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.notification_swipe_refresh);
        recyclerView = view.findViewById(R.id.notifications_recycler_view);

        notificationAdapter = new NotificationAdapter(requireContext());
        recyclerView.setAdapter(notificationAdapter);
        notificationAdapter.setOnNotificationClickListener(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(com.example.mobile_android.data.local.NotificationEntity notification) {
                // 읽음 처리는 Adapter에서 이미 처리됨
                // 화면 이동 로직 제거
            }
            @Override
            public void onDeleteClick(com.example.mobile_android.data.local.NotificationEntity notification) {
                notificationList.remove(notification);
                notificationAdapter.submitList(new java.util.ArrayList<>(notificationList));
            }
        });
    }

    private void updatePermissionTipVisibility() {
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(requireContext());
        cardPermissionTip.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionTipVisibility();
    }
}
