package com.example.mobile_android;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobile_android.data.local.NotificationEntity;
import com.example.mobile_android.model.Notification;
import com.example.mobile_android.ui.notification.NotificationAdapter;
import com.example.mobile_android.ui.notification.NotificationViewModel;
import com.example.mobile_android.ui.post.PostDetailActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.example.mobile_android.util.NotificationPermissionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private View notificationView;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnNotification;
    private View notificationBadge;
    private BottomNavigationView navView;
    private NotificationAdapter notificationAdapter;
    private NotificationViewModel notificationViewModel;
    private SwipeRefreshLayout notificationSwipeRefresh;
    private TextView notificationEmptyView;
    private TextView notificationUnreadCount;
    private ChipGroup notificationFilterChips;
    private final List<NotificationEntity> cachedNotifications = new ArrayList<>();
    private NotificationFilter currentNotificationFilter = NotificationFilter.ALL;
    private View permissionRequiredLayout;
    private MaterialButton btnGrantPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT);

        notificationView = findViewById(R.id.notification_view);
        Toolbar toolbar = findViewById(R.id.header);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        navView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navView, navController);

        btnMenu = toolbar.findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        notificationBadge = toolbar.findViewById(R.id.notification_badge);
        btnNotification = toolbar.findViewById(R.id.btn_notification);
        btnNotification.setOnClickListener(v -> showNotificationView());

        notificationView.findViewById(R.id.btn_back).setOnClickListener(v -> hideNotificationView());

        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        setupNotificationPanel();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (notificationView.getVisibility() == View.VISIBLE) {
                    hideNotificationView();
                } else {
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Room DB LiveData가 자동으로 관찰하므로 별도 refresh 불필요
        // 설정 화면에서 돌아왔을 때 알림 권한 상태 재확인
        if (notificationView.getVisibility() == View.VISIBLE) {
            updateNotificationPermissionUI();
        }
    }

    private void showNotificationView() {
        // Room DB LiveData가 자동으로 관찰하므로 별도 refresh 불필요
        notificationView.setVisibility(View.VISIBLE);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        notificationView.startAnimation(slideIn);

        // 알림 권한 체크 및 UI 업데이트
        updateNotificationPermissionUI();
    }

    private void updateNotificationPermissionUI() {
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(this);

        if (permissionRequiredLayout != null) {
            permissionRequiredLayout.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
        }
        if (notificationSwipeRefresh != null) {
            notificationSwipeRefresh.setVisibility(hasPermission ? View.VISIBLE : View.GONE);
        }
        if (notificationEmptyView != null && hasPermission) {
            // 권한이 있을 때만 empty view 표시 로직 적용
        }
    }

    private void hideNotificationView() {
        Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                notificationView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        notificationView.startAnimation(slideOut);
    }

    private void setupNotificationPanel() {
        RecyclerView recyclerView = notificationView.findViewById(R.id.notifications_recycler_view);
        notificationEmptyView = notificationView.findViewById(R.id.tv_notification_empty);
        notificationSwipeRefresh = notificationView.findViewById(R.id.notification_swipe_refresh);
        notificationUnreadCount = notificationView.findViewById(R.id.tv_notification_unread_count);
        notificationFilterChips = notificationView.findViewById(R.id.chip_notification_filters);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(this);
        recyclerView.setAdapter(notificationAdapter);

        notificationAdapter.setOnNotificationClickListener(new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationEntity notification) {
                if (notificationViewModel != null) {
                    notificationViewModel.markAsRead(notification.getId());
                }
                routeNotification(notification);
            }

            @Override
            public void onDeleteClick(NotificationEntity notification) {
                if (notificationViewModel != null) {
                    notificationViewModel.delete(notification.getId());
                }
            }
        });

        if (notificationFilterChips != null) {
            notificationFilterChips.check(R.id.chip_filter_all);
            notificationFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int selectedId = checkedIds.isEmpty() ? R.id.chip_filter_all : checkedIds.get(0);
                if (selectedId == R.id.chip_filter_deadline) {
                    currentNotificationFilter = NotificationFilter.CALENDAR;
                } else if (selectedId == R.id.chip_filter_new) {
                    currentNotificationFilter = NotificationFilter.CRAWL;
                } else {
                    currentNotificationFilter = NotificationFilter.ALL;
                }
                applyNotificationFilter();
            });
        }

        if (notificationSwipeRefresh != null) {
            notificationSwipeRefresh.setOnRefreshListener(() -> {
                // Room DB는 LiveData로 자동 관찰되므로 별도 새로고침 불필요
                // 스와이프 제스처에 대한 피드백만 제공
                if (notificationSwipeRefresh != null) {
                    notificationSwipeRefresh.setRefreshing(false);
                }
            });
        }

        // 알림 권한 관련 UI 설정
        permissionRequiredLayout = notificationView.findViewById(R.id.layout_permission_required);
        btnGrantPermission = notificationView.findViewById(R.id.btn_grant_permission);
        if (btnGrantPermission != null) {
            btnGrantPermission.setOnClickListener(v ->
                    NotificationPermissionHelper.openNotificationSettings(this)
            );
        }

        if (notificationViewModel != null) {
            notificationViewModel.getNotifications().observe(this, entities -> {
                cachedNotifications.clear();
                if (entities != null) {
                    cachedNotifications.addAll(entities);
                }
                applyNotificationFilter();
                if (notificationSwipeRefresh != null) {
                    notificationSwipeRefresh.setRefreshing(false);
                }
            });

            notificationViewModel.getUnreadCount().observe(this, count -> {
                int unread = count != null ? count : 0;
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                }
                if (notificationUnreadCount != null) {
                    notificationUnreadCount.setText(
                            getString(R.string.notification_unread_count, unread)
                    );
                }
            });
        }
    }

    private void routeNotification(NotificationEntity notification) {
        if (notification == null) return;

        if ("new_post".equals(notification.getType()) && notification.getPostId() != null) {
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("POST_ID", notification.getPostId());
            startActivity(intent);
        } else if ("site_registered".equals(notification.getType())) {
            hideNotificationView();
            Toast.makeText(this, getString(R.string.notifications_title), Toast.LENGTH_SHORT).show();
        } else {
            hideNotificationView();
        }
    }

    private void applyNotificationFilter() {
        List<NotificationEntity> source = cachedNotifications != null
                ? cachedNotifications
                : Collections.emptyList();
        List<NotificationEntity> filtered = new ArrayList<>();
        for (NotificationEntity entity : source) {
            if (matchesFilter(entity)) {
                filtered.add(entity);
            }
        }
        notificationAdapter.submitList(filtered);

        // 권한이 있을 때만 empty view 표시
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(this);
        if (notificationEmptyView != null) {
            notificationEmptyView.setVisibility(
                    (hasPermission && filtered.isEmpty()) ? View.VISIBLE : View.GONE
            );
        }
    }

    private boolean matchesFilter(NotificationEntity entity) {
        if (entity == null) return false;
        String type = entity.getType();
        if (currentNotificationFilter == NotificationFilter.CALENDAR) {
            return isCalendarNotification(type);
        } else if (currentNotificationFilter == NotificationFilter.CRAWL) {
            return isCrawlNotification(type);
        }
        return true;
    }

    private boolean isCalendarNotification(String rawType) {
        if (rawType == null) {
            return false;
        }
        String type = rawType.toLowerCase();
        return type.contains("calendar")
                || type.contains("schedule")
                || Notification.Type.SCHEDULE_REMINDER.equals(rawType)
                || Notification.Type.EVENT_REMINDER.equals(rawType)
                || Notification.Type.DEADLINE.equals(rawType);
    }

    private boolean isCrawlNotification(String rawType) {
        if (rawType == null) {
            return false;
        }
        String type = rawType.toLowerCase();
        if (type.contains("crawl_new_posts")) {
            return true;
        }
        return Notification.Type.NEW_POST.equals(rawType)
                || Notification.Type.CRAWLING_COMPLETE.equals(rawType);
    }

    private enum NotificationFilter {
        ALL,
        CALENDAR,
        CRAWL
    }

    /**
     * 드로어 닫기
     */
    public void closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}
