package com.example.mobile_android;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
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

import com.bumptech.glide.Glide;
import com.example.mobile_android.data.local.NotificationEntity;
import com.example.mobile_android.model.Notification;
import com.example.mobile_android.ui.login.Login;
import com.example.mobile_android.ui.notification.NotificationAdapter;
import com.example.mobile_android.ui.notification.NotificationViewModel;
import com.example.mobile_android.ui.post.PostDetailActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private View notificationView;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnNotification;
    private View notificationBadge;
    private GoogleSignInClient googleSignInClient;
    private NotificationAdapter notificationAdapter;
    private NotificationViewModel notificationViewModel;
    private SwipeRefreshLayout notificationSwipeRefresh;
    private TextView notificationEmptyView;
    private TextView notificationUnreadCount;
    private MaterialButton notificationFilterButton;
    private ChipGroup notificationFilterChips;
    private final List<NotificationEntity> cachedNotifications = new ArrayList<>();
    private NotificationFilter currentNotificationFilter = NotificationFilter.ALL;

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
        BottomNavigationView navView = findViewById(R.id.nav_view);
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

        setupNavigationView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Room DB LiveData가 자동으로 관찰하므로 별도 refresh 불필요
    }

    private void setupNavigationView() {
        NavigationView navigationView = findViewById(R.id.navigation_drawer);

        // Setup user profile
        ImageView profileImage = navigationView.findViewById(R.id.profile_image);
        TextView userName = navigationView.findViewById(R.id.user_name);
        TextView userEmail = navigationView.findViewById(R.id.user_email);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                userName.setText(currentUser.getDisplayName());
            }
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                userEmail.setText(currentUser.getEmail());
            }
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(profileImage);
            }
        }
        TextView manageSiteButton = navigationView.findViewById(R.id.manage_site_button);

        // Setup privacy policy button
        TextView privacyPolicyButton = navigationView.findViewById(R.id.privacy_policy_button);
        privacyPolicyButton.setOnClickListener(v -> showPrivacyPolicyDialog());

        // Setup logout button
        TextView logoutButton = navigationView.findViewById(R.id.logout_button);

        // Configure Google Sign In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        logoutButton.setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();
            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Toast.makeText(MainActivity.this, "로그아웃 하였습니다", Toast.LENGTH_SHORT).show();
                // Go back to Login activity
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
                finish();
            });

        });
        if (manageSiteButton != null) {
            manageSiteButton.setOnClickListener(v -> {
                // 드로어 먼저 닫고
                drawerLayout.closeDrawer(GravityCompat.START);
                // 새 액티비티 열기
                Intent intent = new Intent(MainActivity.this, com.example.mobile_android.ui.site.SiteManageActivity.class);
                startActivity(intent);
            });
        }


    }


    private void showPrivacyPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("개인정보 처리방침");

        // Create a ScrollView to contain the long text
        ScrollView scrollView = new ScrollView(this);

        // Create a TextView for the message
        TextView message = new TextView(this);
        message.setText(R.string.privacy_policy_text);

        // Add padding
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        message.setPadding(padding, padding, padding, padding);

        scrollView.addView(message);

        builder.setView(scrollView);
        builder.setPositiveButton("확인", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showNotificationView() {
        // Room DB LiveData가 자동으로 관찰하므로 별도 refresh 불필요
        notificationView.setVisibility(View.VISIBLE);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        notificationView.startAnimation(slideIn);
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
        notificationFilterButton = notificationView.findViewById(R.id.btn_notification_filter);
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

        if (notificationFilterButton != null) {
            notificationFilterButton.setOnClickListener(v ->
                    Toast.makeText(this, "세부 필터는 곧 제공될 예정입니다.", Toast.LENGTH_SHORT).show()
            );
        }

        if (notificationFilterChips != null) {
            notificationFilterChips.check(R.id.chip_filter_all);
            notificationFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int selectedId = checkedIds.isEmpty() ? R.id.chip_filter_all : checkedIds.get(0);
                if (selectedId == R.id.chip_filter_deadline) {
                    currentNotificationFilter = NotificationFilter.DEADLINE;
                } else if (selectedId == R.id.chip_filter_new) {
                    currentNotificationFilter = NotificationFilter.NEW;
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
        if (notificationEmptyView != null) {
            notificationEmptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean matchesFilter(NotificationEntity entity) {
        if (entity == null) return false;
        String type = entity.getType();
        if (currentNotificationFilter == NotificationFilter.DEADLINE) {
            return Notification.Type.SCHEDULE_REMINDER.equals(type);
        } else if (currentNotificationFilter == NotificationFilter.NEW) {
            return Notification.Type.NEW_POST.equals(type);
        }
        return true;
    }

    private enum NotificationFilter {
        ALL,
        DEADLINE,
        NEW
    }
}
