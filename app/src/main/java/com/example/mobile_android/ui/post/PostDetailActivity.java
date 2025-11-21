package com.example.mobile_android.ui.post;

import android.content.Intent;
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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.util.DateTimeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostDetailActivity extends AppCompatActivity {

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

    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private LiveData<Post> postLiveData;
    private Post currentPost;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        initViews();
        initDatabase();

        postId = getIntent().getStringExtra("POST_ID");
        if (postId != null) {
            observePost();
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

    private void initDatabase() {
        postDao = AppDatabase.getInstance(this).postDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    private void observePost() {
        postLiveData = postDao.getPostById(postId);
        postLiveData.observe(this, post -> {
            if (post != null) {
                currentPost = post;
                displayPostDetail(post);
            }
        });
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

    private void displayPostDetail(Post post) {
        tvPostTitle.setText(post.getTitle());
        tvPostCategory.setText(resolveCategory(post));
        tvPostContent.setText(post.getContent());
        tvPostCreatedAt.setText(formatDateTime(post.getCreatedAt(), "yyyy년 MM월 dd일 a hh:mm"));

        bindEventSection(post);
        bindLocation(post);
        bindSource(post);
        updateCalendarButtonUI(post.isSaved());
    }

    private void bindEventSection(Post post) {
        String eventLabel = buildEventLabel(post);
        if (TextUtils.isEmpty(eventLabel)) {
            tvPostEventDate.setVisibility(View.GONE);
            tvCalendarInfo.setVisibility(View.GONE);
            btnAddToCalendar.setVisibility(View.GONE);
        } else {
            tvPostEventDate.setVisibility(View.VISIBLE);
            tvPostEventDate.setText("📅 " + eventLabel);
            tvCalendarInfo.setVisibility(View.VISIBLE);
            btnAddToCalendar.setVisibility(View.VISIBLE);
        }
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
        boolean newState = !currentPost.isSaved();
        databaseExecutor.execute(() -> {
            postDao.updateSaveState(currentPost.getId(), newState);
        });
    }

    private void updateCalendarButtonUI(boolean isSaved) {
        if (isSaved) {
            btnAddToCalendar.setText("캘린더에서 삭제");
            btnAddToCalendar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        } else {
            btnAddToCalendar.setText("캘린더에 추가");
            btnAddToCalendar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_purple));
        }
    }

    private String formatDateTime(String dateTimeStr, String format) {
        return DateTimeUtils.formatServerDate(dateTimeStr, format);
    }

    private String resolveCategory(Post post) {
        if (!TextUtils.isEmpty(post.getCategoryName())) {
            return post.getCategoryName();
        } else if (!TextUtils.isEmpty(post.getSiteName())) {
            return post.getSiteName();
        }
        return getString(R.string.app_name);
    }

    private String buildEventLabel(Post post) {
        String eventDateStr = post.getCalendarAnchorDate();
        if (TextUtils.isEmpty(eventDateStr)) {
            return "";
        }
        return formatDateTime(eventDateStr, "yyyy년 MM월 dd일 (E) HH:mm");
    }
}
