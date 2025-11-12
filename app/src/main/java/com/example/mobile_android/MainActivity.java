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
