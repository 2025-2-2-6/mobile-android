package com.example.mobile_android.ui.mypage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mobile_android.MainActivity;
import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.model.UserStatistics;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.ui.login.Login;
import com.example.mobile_android.ui.post.PostListActivity;
import com.example.mobile_android.util.NotificationPermissionHelper;
import com.example.mobile_android.util.NotificationTopicManager;
import com.example.mobile_android.util.TokenManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MyPageFragment extends Fragment {

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NEW_POST_NOTIFICATION = "new_post_notification";
    private static final String KEY_CALENDAR_NOTIFICATION = "calendar_notification";

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences prefs;

    // Views
    private ImageView profileImage;
    private TextView userName;
    private TextView userEmail;
    private TextView registeredSitesCount;
    private TextView newPostsCount;
    private TextView savedEventsCount;
    private SwitchMaterial switchNewPostNotification;
    private SwitchMaterial switchCalendarNotification;
    private LinearLayout menuRegisteredSites;
    private LinearLayout menuNewPosts;
    private LinearLayout menuSavedEvents;
    private TextView menuPrivacyPolicy;
    private TextView logoutButton;

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private SwitchMaterial pendingSwitch; // 권한 요청 중인 스위치 추적
    private boolean isProgrammaticChange = false; // 프로그래밍 방식 변경 플래그

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        // 알림 권한 요청 런처 등록
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(requireContext(), "알림이 활성화되었습니다", Toast.LENGTH_SHORT).show();
                        // 권한 승인 시 pending 스위치 활성화 및 저장
                        if (pendingSwitch != null) {
                            isProgrammaticChange = true;
                            pendingSwitch.setChecked(true);
                            isProgrammaticChange = false;

                            String key = (pendingSwitch == switchNewPostNotification)
                                    ? KEY_NEW_POST_NOTIFICATION : KEY_CALENDAR_NOTIFICATION;
                            prefs.edit().putBoolean(key, true).apply();
                        }
                    } else {
                        // 권한 거부 시 스위치 off (이미 off 상태이므로 불필요하지만 명시적으로)
                        showNotificationPermissionDialog();
                    }
                    pendingSwitch = null;
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        initViews(view);
        setupAuth();
        loadUserProfile();
        setupNotificationSwitches();
        setupMenuClickListeners();
        observeActivityCounts(); // LiveData로 실시간 관찰

        return view;
    }

    private void initViews(View view) {
        android.util.Log.d("MyPageFragment", "initViews called");

        profileImage = view.findViewById(R.id.profile_image);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);

        registeredSitesCount = view.findViewById(R.id.registered_sites_count);
        newPostsCount = view.findViewById(R.id.new_posts_count);
        savedEventsCount = view.findViewById(R.id.saved_events_count);

        menuRegisteredSites = view.findViewById(R.id.menu_registered_sites);
        menuNewPosts = view.findViewById(R.id.menu_new_posts);
        menuSavedEvents = view.findViewById(R.id.menu_saved_events);

        switchNewPostNotification = view.findViewById(R.id.switch_new_post_notification);
        switchCalendarNotification = view.findViewById(R.id.switch_calendar_notification);

        android.util.Log.d("MyPageFragment", "switchNewPostNotification: " + switchNewPostNotification);
        android.util.Log.d("MyPageFragment", "switchCalendarNotification: " + switchCalendarNotification);

        if (switchNewPostNotification == null) {
            android.util.Log.e("MyPageFragment", "ERROR: switchNewPostNotification is NULL!");
        } else {
            android.util.Log.d("MyPageFragment", "Switch found and clickable: " + switchNewPostNotification.isClickable());
        }

        menuPrivacyPolicy = view.findViewById(R.id.menu_privacy_policy);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void setupAuth() {
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), options);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(profileImage);
            }

            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                userName.setText(currentUser.getDisplayName());
            }

            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                userEmail.setText(currentUser.getEmail());
            }
        }
    }

    private void setupNotificationSwitches() {
        android.util.Log.e("SWITCH_DEBUG", "====== setupNotificationSwitches START ======");

        android.util.Log.e("SWITCH_DEBUG", "switchNewPostNotification: " + switchNewPostNotification);
        android.util.Log.e("SWITCH_DEBUG", "switchCalendarNotification: " + switchCalendarNotification);

        if (switchNewPostNotification == null) {
            android.util.Log.e("SWITCH_DEBUG", "ERROR: switchNewPostNotification is NULL!");
            Toast.makeText(requireContext(), "ERROR: 새 게시물 스위치 초기화 실패", Toast.LENGTH_LONG).show();
            return;
        }

        if (switchCalendarNotification == null) {
            android.util.Log.e("SWITCH_DEBUG", "ERROR: switchCalendarNotification is NULL!");
            Toast.makeText(requireContext(), "ERROR: 일정 스위치 초기화 실패", Toast.LENGTH_LONG).show();
            return;
        }

        android.util.Log.e("SWITCH_DEBUG", "Both switches found successfully");

        final NotificationToggleMeta newPostMeta = new NotificationToggleMeta(
                KEY_NEW_POST_NOTIFICATION,
                getString(R.string.notification_topic_new_posts),
                NotificationTopicManager.TOPIC_CRAWL_NEW_POSTS
        );
        final NotificationToggleMeta calendarMeta = new NotificationToggleMeta(
                KEY_CALENDAR_NOTIFICATION,
                getString(R.string.notification_topic_calendar),
                NotificationTopicManager.TOPIC_CALENDAR_REMINDER
        );
        switchNewPostNotification.setTag(newPostMeta);
        switchCalendarNotification.setTag(calendarMeta);

        // 터치 이벤트 리스너 추가 (디버깅용)
        switchNewPostNotification.setOnTouchListener((v, event) -> {
            android.util.Log.e("SWITCH_DEBUG", "===== NEW POST SWITCH TOUCHED =====");
            android.util.Log.e("SWITCH_DEBUG", "Action: " + event.getAction());
            android.util.Log.e("SWITCH_DEBUG", "X: " + event.getX() + ", Y: " + event.getY());
            return false; // 이벤트 계속 전파
        });

        switchCalendarNotification.setOnTouchListener((v, event) -> {
            android.util.Log.e("SWITCH_DEBUG", "===== CALENDAR SWITCH TOUCHED =====");
            android.util.Log.e("SWITCH_DEBUG", "Action: " + event.getAction());
            return false;
        });

        // 저장된 상태 로드 (프로그래밍 방식 변경)
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(requireContext());
        boolean newPostEnabled = hasPermission && prefs.getBoolean(KEY_NEW_POST_NOTIFICATION, false);
        boolean calendarEnabled = hasPermission && prefs.getBoolean(KEY_CALENDAR_NOTIFICATION, false);

        android.util.Log.e("SWITCH_DEBUG", "Setting initial states...");
        android.util.Log.e("SWITCH_DEBUG", "hasPermission: " + hasPermission);
        android.util.Log.e("SWITCH_DEBUG", "newPostEnabled: " + newPostEnabled);
        android.util.Log.e("SWITCH_DEBUG", "calendarEnabled: " + calendarEnabled);

        isProgrammaticChange = true;
        switchNewPostNotification.setChecked(newPostEnabled);
        switchCalendarNotification.setChecked(calendarEnabled);
        isProgrammaticChange = false;

        android.util.Log.e("SWITCH_DEBUG", "Initial states set, now registering listeners...");

        // 스위치 상태 변경 리스너
        switchNewPostNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            android.util.Log.e("SWITCH_DEBUG", "====== NEW POST LISTENER CALLED ======");
            android.util.Log.e("SWITCH_DEBUG", "isChecked: " + isChecked);
            android.util.Log.e("SWITCH_DEBUG", "isProgrammaticChange: " + isProgrammaticChange);
            android.util.Log.e("SWITCH_DEBUG", "buttonView: " + buttonView);

            if (isProgrammaticChange) {
                android.util.Log.e("SWITCH_DEBUG", "IGNORING - programmatic change");
                return;
            }

            android.util.Log.e("SWITCH_DEBUG", "PROCESSING - user clicked");
            handleSwitchChange(switchNewPostNotification, isChecked, newPostMeta);
        });

        switchCalendarNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            android.util.Log.e("SWITCH_DEBUG", "====== CALENDAR LISTENER CALLED ======");
            android.util.Log.e("SWITCH_DEBUG", "isChecked: " + isChecked);
            android.util.Log.e("SWITCH_DEBUG", "isProgrammaticChange: " + isProgrammaticChange);

            if (isProgrammaticChange) {
                android.util.Log.e("SWITCH_DEBUG", "IGNORING - programmatic change");
                return;
            }

            android.util.Log.e("SWITCH_DEBUG", "PROCESSING - user clicked");
            handleSwitchChange(switchCalendarNotification, isChecked, calendarMeta);
        });

        android.util.Log.e("SWITCH_DEBUG", "====== Listeners registered successfully ======");
        android.util.Log.e("SWITCH_DEBUG", "====== setupNotificationSwitches END ======");
    }

    private void handleSwitchChange(SwitchMaterial switchView, boolean isChecked, NotificationToggleMeta meta) {
        if (meta == null) return;

        android.util.Log.e("SWITCH_DEBUG", "====== handleSwitchChange CALLED ======");
        android.util.Log.e("SWITCH_DEBUG", "label: " + meta.label);
        android.util.Log.e("SWITCH_DEBUG", "isChecked: " + isChecked);

        if (isChecked && !NotificationPermissionHelper.hasNotificationPermission(requireContext())) {
            android.util.Log.d("MyPageFragment", "No permission, requesting...");

            isProgrammaticChange = true;
            switchView.setChecked(false);
            isProgrammaticChange = false;

            pendingSwitch = switchView;
            NotificationPermissionHelper.requestNotificationPermission(notificationPermissionLauncher);
            Toast.makeText(requireContext(), "알림 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        switchView.setEnabled(false);
        NotificationTopicManager.updateTopic(meta.topic, isChecked, success -> {
            switchView.setEnabled(true);
            if (!success) {
                isProgrammaticChange = true;
                switchView.setChecked(!isChecked);
                isProgrammaticChange = false;
                Toast.makeText(requireContext(), getString(R.string.notification_topic_update_failed), Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putBoolean(meta.prefKey, isChecked).apply();
            String message = meta.label + "이 " + (isChecked ? "활성화" : "비활성화") + "되었습니다";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            android.util.Log.d("MyPageFragment", message);
        });
    }

    @Nullable
    private NotificationToggleMeta getToggleMeta(@Nullable SwitchMaterial switchMaterial) {
        if (switchMaterial == null) {
            return null;
        }
        Object tag = switchMaterial.getTag();
        if (tag instanceof NotificationToggleMeta) {
            return (NotificationToggleMeta) tag;
        }
        return null;
    }

    private void setupMenuClickListeners() {
        // 등록 사이트 클릭 → 사이트 관리 페이지 (SearchFragment)
        menuRegisteredSites.setOnClickListener(v -> navigateToSiteManagement());

        // 새 게시물 클릭 → 게시물 목록 (새 게시물만)
        menuNewPosts.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PostListActivity.class);
            intent.putExtra("FILTER_TYPE", "new_posts");
            startActivity(intent);
        });

        // 저장된 일정 클릭 → 게시물 목록 (저장됨)
        menuSavedEvents.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PostListActivity.class);
            intent.putExtra("FILTER_TYPE", "saved_events");
            startActivity(intent);
        });

        // 개인정보 처리방침
        menuPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());

        // 로그아웃
        logoutButton.setOnClickListener(v -> showLogoutDialog());
    }

    private void navigateToSiteManagement() {
        // 사이트 관리 기능 제거됨
        // TODO: 필요시 다른 기능으로 대체
    }

    /**
     * 활동 카운팅 실시간 관찰 (LiveData 기반)
     * HomeFragment와 동일한 방식으로 로컬 DB에서 직접 계산
     */
    private void observeActivityCounts() {
        AppDatabase db = AppDatabase.getInstance(requireContext());

        // 1. 등록 사이트 개수 (LiveData)
        db.siteDao().observeAll().observe(getViewLifecycleOwner(), sites -> {
            if (sites != null) {
                registeredSitesCount.setText(String.valueOf(sites.size()));
            } else {
                registeredSitesCount.setText("0");
            }
        });

        // 2. 새 게시물 개수 (is_new = true, Post.isActuallyNew() 기준)
        db.postDao().getAllPostsForNewFilter().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                long count = posts.stream().filter(post -> post.isActuallyNew()).count();
                newPostsCount.setText(String.valueOf(count));
            } else {
                newPostsCount.setText("0");
            }
        });

        // 3. 알림 설정된 일정 개수 (notify_enabled = true)
        db.calendarEventDao().getAllEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                long count = events.stream()
                        .filter(event -> event.isNotifyEnabled())
                        .count();
                savedEventsCount.setText(String.valueOf(count));
            } else {
                savedEventsCount.setText("0");
            }
        });
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("알림 권한 필요")
                .setMessage("알림을 받으려면 알림 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    NotificationPermissionHelper.openNotificationSettings(requireContext());
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("개인정보 처리방침")
                .setMessage(getString(R.string.privacy_policy_text))
                .setPositiveButton("확인", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> performLogout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void performLogout() {
        auth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Toast.makeText(requireContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireActivity(), Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // observeActivityCounts()가 LiveData로 자동 업데이트하므로 별도 호출 불필요
        // 설정 화면에서 돌아왔을 때 권한 상태 재확인
        updateNotificationSwitchesBasedOnPermission();
    }

    private void updateNotificationSwitchesBasedOnPermission() {
        boolean hasPermission = NotificationPermissionHelper.hasNotificationPermission(requireContext());

        isProgrammaticChange = true;

        // 권한이 없으면 모든 스위치를 off
        if (!hasPermission) {
            switchNewPostNotification.setChecked(false);
            switchCalendarNotification.setChecked(false);
            prefs.edit()
                    .putBoolean(KEY_NEW_POST_NOTIFICATION, false)
                    .putBoolean(KEY_CALENDAR_NOTIFICATION, false)
                    .apply();
        } else {
            // 권한이 있으면 저장된 상태대로 복원
            boolean newPostEnabled = prefs.getBoolean(KEY_NEW_POST_NOTIFICATION, false);
            boolean calendarEnabled = prefs.getBoolean(KEY_CALENDAR_NOTIFICATION, false);
            switchNewPostNotification.setChecked(newPostEnabled);
            switchCalendarNotification.setChecked(calendarEnabled);
        }

        isProgrammaticChange = false;
    }

    private static class NotificationToggleMeta {
        final String prefKey;
        final String label;
        final String topic;

        NotificationToggleMeta(String prefKey, String label, String topic) {
            this.prefKey = prefKey;
            this.label = label;
            this.topic = topic;
        }
    }
}
