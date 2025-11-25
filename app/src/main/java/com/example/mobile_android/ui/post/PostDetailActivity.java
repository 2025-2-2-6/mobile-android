package com.example.mobile_android.ui.post;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout; // [수정 1] Import 추가
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.example.mobile_android.R;
import com.example.mobile_android.data.CalendarEventRepository;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.util.CalendarEventHelper;
import com.example.mobile_android.util.DateTimeUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostDetailActivity extends AppCompatActivity {

    private TextView tvPostTitle;
    private ChipGroup chipGroupCategories;
    private Chip chipCategory;
    private TextView tvPostCreatedAt;
    private TextView tvPostEventDate;
    private TextView tvPostLocation;

    // AI 요약은 XML에서 CardView로 유지했다면 그대로 둡니다.
    private CardView cardAiSummary;

    private TextView tvAiSummary;
    private TextView tvPostContent;

    // [수정 2] CardView -> LinearLayout으로 변경
    private LinearLayout cardViewSource;

    private Button btnViewSource;
    private TextView tvCalendarInfo;
    private Button btnAddToCalendar;
    private ImageButton btnBack;

    // ... (이하 코드는 변경 없음) ...
    private PostDao postDao;
    private CalendarEventRepository calendarEventRepository;
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
        // ... (다른 뷰 초기화)
        tvPostContent = findViewById(R.id.tv_post_content);

        // [참고] findViewById는 자동으로 타입을 찾아주므로 여기는 코드 변경 불필요
        // 단, 위에서 변수 선언이 LinearLayout으로 되어 있어야 에러가 안 납니다.
        cardViewSource = findViewById(R.id.card_view_source);

        btnViewSource = findViewById(R.id.btn_view_source);
        // ...

        // [추가 팁] 나머지 초기화 코드는 그대로 유지
        tvPostTitle = findViewById(R.id.tv_post_title);
        chipGroupCategories = findViewById(R.id.chip_group_categories);
        chipCategory = findViewById(R.id.chip_category);
        tvPostCreatedAt = findViewById(R.id.tv_post_created_at);
        tvPostEventDate = findViewById(R.id.tv_post_event_date);
        tvPostLocation = findViewById(R.id.tv_post_location);
        cardAiSummary = findViewById(R.id.card_ai_summary);
        tvCalendarInfo = findViewById(R.id.tv_calendar_info);
        btnAddToCalendar = findViewById(R.id.btn_add_to_calendar);
        btnBack = findViewById(R.id.btn_back);
    }

    // ... (나머지 메소드들도 LinearLayout은 setVisibility를 지원하므로 변경 불필요) ...

    private void bindSource(Post post) {
        if (!TextUtils.isEmpty(post.getSourceUrl())) {
            // LinearLayout도 setVisibility 사용 가능
            cardViewSource.setVisibility(View.VISIBLE);
        } else {
            cardViewSource.setVisibility(View.GONE);
        }
    }

    // ... (기타 모든 로직 동일) ...

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
        String category = resolveCategory(post);
        if (!TextUtils.isEmpty(category)) {
            chipCategory.setText(category);
            chipCategory.setVisibility(View.VISIBLE);
        } else {
            chipCategory.setVisibility(View.GONE);
        }
        tvPostContent.setText(post.getContent());
        tvPostCreatedAt.setText(formatDateTime(post.getCreatedAt(), "yyyy년 M월 d일 a h시 mm분"));
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

    private void handleCalendarToggle() {
        if (currentPost == null || TextUtils.isEmpty(currentPost.getCalendarAnchorDate())) {
            Toast.makeText(this, "일정 정보가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean currentlySaved = currentPost.isSaved();
        if (currentlySaved) {
            CalendarEventHelper.removePostFromCalendar(this, currentPost,
                    new CalendarEventHelper.OnCalendarEventCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                currentPost.setSaved(false);
                                updateCalendarButtonUI(false);
                                databaseExecutor.execute(() -> {
                                    postDao.updateSaveState(currentPost.getId(), false);
                                });
                                Toast.makeText(PostDetailActivity.this, "캘린더에서 제거되었습니다", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> Toast.makeText(PostDetailActivity.this, "제거 실패: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
        } else {
            addToCalendar();
        }
    }

    private void addToCalendar() {
        // CalendarEventHelper 사용으로 변경 (중복 체크 및 알람 자동 설정)
        CalendarEventHelper.addPostToCalendar(this, currentPost,
            new CalendarEventHelper.OnCalendarEventCallback() {
                @Override
                public void onSuccess() {
                    currentPost.setSaved(true);
                    updateCalendarButtonUI(true);
                    databaseExecutor.execute(() -> {
                        postDao.updateSaveState(currentPost.getId(), true);
                    });
                    Toast.makeText(PostDetailActivity.this, "캘린더에 추가되었습니다", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(PostDetailActivity.this, "캘린더 추가 실패: " + error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private String convertToISO8601(String dateTimeStr) {
        try {
            SimpleDateFormat inputFormat;
            if (dateTimeStr.contains("T")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            }
            Date date = inputFormat.parse(dateTimeStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    private void updateCalendarButtonUI(boolean isSaved) {
        if (isSaved) {
            btnAddToCalendar.setText("캘린더에서 삭제");
            btnAddToCalendar.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_color));
            btnAddToCalendar.setTextColor(ContextCompat.getColor(this, R.color.accent_color)); // ※ 주의: 텍스트 색상도 accent_color면 안 보일 수 있습니다. 배경이 accent면 텍스트는 흰색을 추천합니다.
        } else {
            btnAddToCalendar.setText("캘린더에 추가");
            btnAddToCalendar.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_color2));
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