package com.example.mobile_android.ui.post;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.example.mobile_android.util.CalendarManager;
import com.example.mobile_android.util.DateTimeUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    private static final int REQUEST_CALENDAR_PERMISSION = 1001;

    private TextView tvPostTitle;
    private TextView tvPostCategory;
    private TextView tvPostCreatedAt;
    private TextView tvPostEventDate;
    private TextView tvPostLocation;
    private TextView tvPostContent;
    private TextView tvSourceInfo;
    private Button btnViewSource;
    private TextView tvCalendarInfo;
    private Button btnAddToCalendar;
    private ImageButton btnBack;

    private ApiService apiService;
    private CalendarManager calendarManager;
    private Post currentPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        initViews();
        calendarManager = new CalendarManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        String postId = getIntent().getStringExtra("POST_ID");
        if (postId != null) {
            loadPostDetail(postId);
        } else {
            Toast.makeText(this, "게시물 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupClickListeners();
    }

    private void initViews() {
        tvPostTitle = findViewById(R.id.tv_post_title);
        tvPostCategory = findViewById(R.id.tv_post_category);
        tvPostCreatedAt = findViewById(R.id.tv_post_created_at);
        tvPostEventDate = findViewById(R.id.tv_post_event_date);
        tvPostLocation = findViewById(R.id.tv_post_location);
        tvPostContent = findViewById(R.id.tv_post_content);
        tvSourceInfo = findViewById(R.id.tv_source_info);
        btnViewSource = findViewById(R.id.btn_view_source);
        tvCalendarInfo = findViewById(R.id.tv_calendar_info);
        btnAddToCalendar = findViewById(R.id.btn_add_to_calendar);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnViewSource.setOnClickListener(v -> {
            if (currentPost != null && !TextUtils.isEmpty(currentPost.getSourceUrl())) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentPost.getSourceUrl())));
            }
        });

        btnAddToCalendar.setOnClickListener(v -> handleCalendarToggle());
    }

    private void loadPostDetail(String postId) {
        Call<Post> call = apiService.getPostDetail(postId);
        call.enqueue(new Callback<Post>() {
            @Override
            public void onResponse(@NonNull Call<Post> call, @NonNull Response<Post> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentPost = response.body();
                    displayPostDetail(currentPost);
                } else {
                    Toast.makeText(PostDetailActivity.this,
                            "게시물을 불러오지 못했습니다. (" + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Post> call, @NonNull Throwable t) {
                Toast.makeText(PostDetailActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void displayPostDetail(Post post) {
        tvPostTitle.setText(post.getTitle());
        tvPostCategory.setText(resolveCategory(post));
        tvPostContent.setText(post.getContent());

        tvPostCreatedAt.setText(formatDateTime(post.getCreatedAt(), "yyyy년 MM월 dd일 a hh:mm"));

        bindEventSection(post);
        bindLocation(post);
        bindSource(post);
    }

    private void bindEventSection(Post post) {
        String eventLabel = buildEventLabel(post);
        if (TextUtils.isEmpty(eventLabel)) {
            tvPostEventDate.setVisibility(View.GONE);
            tvCalendarInfo.setVisibility(View.GONE);
            btnAddToCalendar.setVisibility(View.GONE);
            return;
        }

        tvPostEventDate.setVisibility(View.VISIBLE);
        tvPostEventDate.setText("📅 " + eventLabel);
        tvCalendarInfo.setVisibility(View.VISIBLE);
        btnAddToCalendar.setVisibility(View.VISIBLE);
        updateCalendarButtonText();
    }

    private void bindLocation(Post post) {
        if (!TextUtils.isEmpty(post.getLocation())) {
            tvPostLocation.setVisibility(View.VISIBLE);
            tvPostLocation.setText("📍 " + post.getLocation());
        } else {
            tvPostLocation.setVisibility(View.GONE);
        }
    }

    private void bindSource(Post post) {
        if (!TextUtils.isEmpty(post.getSourceUrl())) {
            tvSourceInfo.setVisibility(View.VISIBLE);
            btnViewSource.setVisibility(View.VISIBLE);
        } else {
            tvSourceInfo.setVisibility(View.GONE);
            btnViewSource.setVisibility(View.GONE);
        }
    }

    private void handleCalendarToggle() {
        if (currentPost == null) {
            return;
        }

        String calendarAnchor = resolveCalendarAnchor(currentPost);
        if (TextUtils.isEmpty(calendarAnchor)) {
            Toast.makeText(this, "등록 가능한 이벤트 날짜가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!calendarManager.hasCalendarPermission()) {
            requestCalendarPermission();
            return;
        }

        if (calendarManager.isEventRegistered(currentPost.getId())) {
            calendarManager.removeEventFromCalendar(currentPost.getId());
        } else {
            calendarManager.addEventToCalendar(
                    currentPost.getId(),
                    currentPost.getTitle(),
                    calendarAnchor,
                    currentPost.getLocation()
            );
        }
        updateCalendarButtonText();
    }

    private void updateCalendarButtonText() {
        if (currentPost != null && calendarManager.isEventRegistered(currentPost.getId())) {
            btnAddToCalendar.setText("캘린더에서 삭제");
            btnAddToCalendar.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.holo_red_light)
            );
        } else {
            btnAddToCalendar.setText("캘린더에 추가");
            btnAddToCalendar.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, android.R.color.holo_purple)
            );
        }
    }

    private void requestCalendarPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                },
                REQUEST_CALENDAR_PERMISSION
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALENDAR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleCalendarToggle();
            } else {
                Toast.makeText(this, "캘린더 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String formatDateTime(String dateTimeStr, String format) {
        return DateTimeUtils.formatServerDate(dateTimeStr, format);
    }

    private String resolveCategory(Post post) {
        if (!TextUtils.isEmpty(post.getCategoryName())) {
            return post.getCategoryName();
        }
        if (!TextUtils.isEmpty(post.getSiteName())) {
            return post.getSiteName();
        }
        return getString(R.string.app_name);
    }

    private String buildEventLabel(Post post) {
        String start = formatDateTime(post.getEventStartDate(), "yyyy년 MM월 dd일 (E) HH:mm");
        String end = formatDateTime(post.getEventEndDate(), "yyyy년 MM월 dd일 (E) HH:mm");
        String single = formatDateTime(post.getEventDate(), "yyyy년 MM월 dd일 (E) HH:mm");

        if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
            return start + " ~ " + end;
        }
        if (!TextUtils.isEmpty(start)) {
            return start;
        }
        if (!TextUtils.isEmpty(single)) {
            return single;
        }
        return end;
    }

    private String resolveCalendarAnchor(Post post) {
        if (!TextUtils.isEmpty(post.getEventStartDate())) {
            return post.getEventStartDate();
        }
        if (!TextUtils.isEmpty(post.getEventDate())) {
            return post.getEventDate();
        }
        if (!TextUtils.isEmpty(post.getEventEndDate())) {
            return post.getEventEndDate();
        }
        return null;
    }
}
