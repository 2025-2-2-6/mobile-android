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
    private SwitchMaterial toggleDeviceNotification;
    private TextView tvUnreadCount;
    private SharedPreferences prefs;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private boolean isProgrammaticChange = false;

    private NotificationAdapter notificationAdapter;
    private NotificationViewModel notificationViewModel;
    private List<NotificationEntity> notificationList = new java.util.ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        // 알림 권한 요청 런처 등록
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // 권한 승인 시 토글 ON, MyPage 스위치들도 ON
                        isProgrammaticChange = true;
                        toggleDeviceNotification.setChecked(true);
                        isProgrammaticChange = false;

                        // MyPage 알림 설정도 ON으로 변경
                        prefs.edit()
                            .putBoolean(KEY_NEW_POST_NOTIFICATION, true)
                            .putBoolean(KEY_CALENDAR_NOTIFICATION, true)
                            .apply();

                        // 토픽 구독
                        NotificationTopicManager.updateTopic(NotificationTopicManager.TOPIC_CRAWL_NEW_POSTS, true, success -> {});
                        NotificationTopicManager.updateTopic(NotificationTopicManager.TOPIC_CALENDAR_REMINDER, true, success -> {});

                        Toast.makeText(requireContext(), "알림이 활성화되었습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        isProgrammaticChange = true;
                        toggleDeviceNotification.setChecked(false);
                        isProgrammaticChange = false;
                    }
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
        toggleDeviceNotification = view.findViewById(R.id.toggle_device_notification);

        notificationAdapter = new NotificationAdapter(requireContext());
        recyclerView.setAdapter(notificationAdapter);
        notificationAdapter.setOnNotificationClickListener(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(com.example.mobile_android.data.local.NotificationEntity notification) {
                // 읽음 처리는 Adapter에서 이미 처리됨
                // 타입별 네비게이션 처리만 수행
                handleNotificationNavigation(notification);
            }
            @Override
            public void onDeleteClick(com.example.mobile_android.data.local.NotificationEntity notification) {
                notificationList.remove(notification);
                notificationAdapter.submitList(new java.util.ArrayList<>(notificationList));
            }
        });
    }

    private void setupPermissionButton() {
        // 초기 상태 설정: 권한 있고 MyPage 스위치 중 하나라도 ON이면 ON
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(requireContext());
        boolean newPostEnabled = prefs.getBoolean(KEY_NEW_POST_NOTIFICATION, false);
        boolean calendarEnabled = prefs.getBoolean(KEY_CALENDAR_NOTIFICATION, false);
        boolean shouldBeChecked = hasPermission && (newPostEnabled || calendarEnabled);

        isProgrammaticChange = true;
        toggleDeviceNotification.setChecked(shouldBeChecked);
        isProgrammaticChange = false;

        toggleDeviceNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;

            if (isChecked) {
                // 토글 ON -> 권한 요청
                if (!NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        NotificationPermissionHelper.requestNotificationPermission(notificationPermissionLauncher);
                    } else {
                        NotificationPermissionHelper.openNotificationSettings(requireContext());
                    }
                } else {
                    // 이미 권한이 있으면 MyPage 스위치들을 ON으로
                    prefs.edit()
                        .putBoolean(KEY_NEW_POST_NOTIFICATION, true)
                        .putBoolean(KEY_CALENDAR_NOTIFICATION, true)
                        .apply();

                    // 토픽 구독
                    NotificationTopicManager.updateTopic(NotificationTopicManager.TOPIC_CRAWL_NEW_POSTS, true, success -> {});
                    NotificationTopicManager.updateTopic(NotificationTopicManager.TOPIC_CALENDAR_REMINDER, true, success -> {});

                    Toast.makeText(requireContext(), "알림이 활성화되었습니다", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 토글 OFF -> MyPage 스위치들도 OFF
                prefs.edit()
                    .putBoolean(KEY_NEW_POST_NOTIFICATION, false)
                    .putBoolean(KEY_CALENDAR_NOTIFICATION, false)
                    .apply();

                // 토픽 구독 해제
                NotificationTopicManager.updateTopic(NotificationTopicManager.TOPIC_CRAWL_NEW_POSTS, false, success -> {});
                NotificationTopicManager.updateTopic(NotificationTopicManager.TOPIC_CALENDAR_REMINDER, false, success -> {});

                Toast.makeText(requireContext(), "알림이 비활성화되었습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIBasedOnPermission() {
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(requireContext());
        boolean newPostEnabled = prefs.getBoolean(KEY_NEW_POST_NOTIFICATION, false);
        boolean calendarEnabled = prefs.getBoolean(KEY_CALENDAR_NOTIFICATION, false);
        boolean shouldBeChecked = hasPermission && (newPostEnabled || calendarEnabled);

        isProgrammaticChange = true;
        if (toggleDeviceNotification != null) {
            toggleDeviceNotification.setChecked(shouldBeChecked);
        }
        isProgrammaticChange = false;
    }

    private void handleNotificationNavigation(NotificationEntity notification) {
        NotificationType type = NotificationType.fromString(notification.getType());

        switch (type) {
            case CALENDAR_REMINDER:
                // 일정 알림 -> 캘린더 페이지로 이동 (해당 날짜)
                navigateToCalendar(notification);
                break;

            case CRAWL_NEW_POSTS:
                // 크롤링 알림 -> status 상관없이 사이트 상세 페이지로 이동
                // (new_post, success, failed 모두 사이트 상세 보기)
                navigateToSiteDetail(notification);
                break;

            case UNKNOWN:
            default:
                // 알 수 없음 -> 읽음 처리만 (이미 위에서 처리됨)
                break;
        }
    }

    private void navigateToCalendar(NotificationEntity notification) {
        // CalendarFragment로 이동 (MainActivity의 BottomNavigationView 이용)
        if (getActivity() instanceof com.example.mobile_android.MainActivity) {
            // TODO: 특정 날짜로 이동하는 로직 추가 필요
            Toast.makeText(requireContext(), "캘린더로 이동", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToSiteDetail(NotificationEntity notification) {
        if (notification.getSiteId() != null && !notification.getSiteId().isEmpty()) {
            // SiteDetailActivity로 이동
            Intent intent = new Intent(requireContext(), com.example.mobile_android.ui.site.SiteDetailActivity.class);
            intent.putExtra("SITE_ID", notification.getSiteId());
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "사이트 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 설정 화면에서 돌아왔을 때 권한 상태 재확인
        updateUIBasedOnPermission();
    }
}
