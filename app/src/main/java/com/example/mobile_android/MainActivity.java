package com.example.mobile_android;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.mobile_android.R;
public class MainActivity extends AppCompatActivity {

    private View notificationView;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnNotification;

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
        // 🔹 목적지(fragment)에 따라 bottom nav, 메뉴 버튼 모양/동작 바꾸기
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();

            if (destId == R.id.nav_site_manage) {
                // 1) 하단 탭 숨기기
                navView.setVisibility(View.GONE);

                // 2) 왼쪽 버튼을 "뒤로가기" 아이콘으로 바꾸고
                btnMenu.setImageResource(R.drawable.ic_arrow_back); // ↤ 적당한 아이콘으로 교체

                // 3) 누르면 홈으로 이동
                btnMenu.setOnClickListener(v -> {
                    controller.navigateUp();     // 또는 controller.popBackStack();
                });

            } else {
                // nav_site_manage가 아닐 때는 원래 상태로 복구

                navView.setVisibility(View.VISIBLE);  // 하단 탭 다시 보이게

                btnMenu.setImageResource(R.drawable.ic_menu);  // 햄버거 메뉴 아이콘
                btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
            }
        });

        btnNotification = toolbar.findViewById(R.id.btn_notification);
        btnNotification.setOnClickListener(v -> showNotificationView());

        notificationView.findViewById(R.id.btn_back).setOnClickListener(v -> hideNotificationView());

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
        manageSiteButton.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);

            NavController navController =
                    Navigation.findNavController(MainActivity.this,
                            R.id.nav_host_fragment_activity_main);

            navController.navigate(R.id.nav_site_manage);  // 🔥 새 화면으로 이동 (탭 안 바뀜)
        });


        // Setup privacy policy button
        TextView privacyPolicyButton = navigationView.findViewById(R.id.privacy_policy_button);
        privacyPolicyButton.setOnClickListener(v -> showPrivacyPolicyDialog());
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
}
