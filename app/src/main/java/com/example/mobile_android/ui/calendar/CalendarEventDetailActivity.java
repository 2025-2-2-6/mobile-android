package com.example.mobile_android.ui.calendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.example.mobile_android.R;
import com.example.mobile_android.data.CalendarEventRepository;
import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.util.DateTimeUtils;
import com.google.android.material.chip.Chip;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarEventDetailActivity extends AppCompatActivity {

    private String eventId;
    private CalendarEvent currentEvent;
    private CalendarEventRepository repository;
    private LiveData<CalendarEvent> eventLiveData;
    private ExecutorService executor;

    // Views
    private ImageButton btnBack;
    private ImageView btnEdit;
    private ImageView btnDelete;
    private TextView tvEventTitle;
    private Chip chipCategory;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private LinearLayout layoutEndTime;
    private TextView tvDescription;
    private TextView labelDescription;
    private ImageView ivNotifyIcon;
    private TextView tvNotifyStatus;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_event_detail);

        initViews();
        repository = CalendarEventRepository.getInstance(this);
        executor = Executors.newSingleThreadExecutor();

        eventId = getIntent().getStringExtra("EVENT_ID");
        if (eventId != null) {
            loadEventDetail();
        } else {
            Toast.makeText(this, "일정 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        tvEventTitle = findViewById(R.id.tv_event_title);
        chipCategory = findViewById(R.id.chip_category);
        tvStartTime = findViewById(R.id.tv_start_time);
        tvEndTime = findViewById(R.id.tv_end_time);
        layoutEndTime = findViewById(R.id.layout_end_time);
        tvDescription = findViewById(R.id.tv_description);
        labelDescription = findViewById(R.id.label_description);
        ivNotifyIcon = findViewById(R.id.iv_notify_icon);
        tvNotifyStatus = findViewById(R.id.tv_notify_status);
        tvCreatedAt = findViewById(R.id.tv_created_at);
        tvUpdatedAt = findViewById(R.id.tv_updated_at);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void loadEventDetail() {
        eventLiveData = repository.getEventById(eventId);
        eventLiveData.observe(this, event -> {
            if (event != null) {
                currentEvent = event;
                displayEventDetail(event);
            } else {
                Toast.makeText(this, "일정을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayEventDetail(CalendarEvent event) {
        // 제목
        tvEventTitle.setText(event.getTitle());

        // 카테고리
        if (!TextUtils.isEmpty(event.getCategory())) {
            chipCategory.setText(event.getCategory());
            chipCategory.setChipBackgroundColorResource(android.R.color.transparent);
            chipCategory.setChipStrokeWidth(2f);
            chipCategory.setTextColor(getCategoryColor(event.getCategory()));
            chipCategory.setChipStrokeColor(android.content.res.ColorStateList.valueOf(getCategoryColor(event.getCategory())));
            chipCategory.setVisibility(View.VISIBLE);
        } else {
            chipCategory.setVisibility(View.GONE);
        }

        // 시작 시간
        if (!TextUtils.isEmpty(event.getStartTime())) {
            String formattedStart = DateTimeUtils.formatServerDate(event.getStartTime(), "yyyy년 MM월 dd일 (E) a h:mm");
            tvStartTime.setText(formattedStart);
        }

        // 종료 시간
        if (!TextUtils.isEmpty(event.getEndTime())) {
            String formattedEnd = DateTimeUtils.formatServerDate(event.getEndTime(), "yyyy년 MM월 dd일 (E) a h:mm");
            tvEndTime.setText(formattedEnd);
            layoutEndTime.setVisibility(View.VISIBLE);
        } else {
            layoutEndTime.setVisibility(View.GONE);
        }

        // 메모/설명
        if (!TextUtils.isEmpty(event.getDescription())) {
            tvDescription.setText(event.getDescription());
            labelDescription.setVisibility(View.VISIBLE);
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            labelDescription.setVisibility(View.GONE);
            tvDescription.setVisibility(View.GONE);
        }

        // 알림 상태
        if (event.isNotifyEnabled()) {
            ivNotifyIcon.setImageResource(R.drawable.ic_bell_enabled);
            String notifyText = "알림 켜짐";
            if (!TextUtils.isEmpty(event.getNotifyTime())) {
                notifyText += " (알림 설정됨)";
            }
            tvNotifyStatus.setText(notifyText);
            tvNotifyStatus.setTextColor(0xFF2E7D32); // 초록색
        } else {
            ivNotifyIcon.setImageResource(R.drawable.ic_bell_disabled);
            tvNotifyStatus.setText("알림 꺼짐");
            tvNotifyStatus.setTextColor(0xFF9E9E9E); // 회색
        }

        // 생성 시간
        if (!TextUtils.isEmpty(event.getCreatedAt())) {
            String formattedCreated = DateTimeUtils.formatServerDate(event.getCreatedAt(), "생성: yyyy년 MM월 dd일 a h:mm");
            tvCreatedAt.setText(formattedCreated);
        }

        // 수정 시간
        if (!TextUtils.isEmpty(event.getUpdatedAt()) && !event.getUpdatedAt().equals(event.getCreatedAt())) {
            String formattedUpdated = DateTimeUtils.formatServerDate(event.getUpdatedAt(), "수정: yyyy년 MM월 dd일 a h:mm");
            tvUpdatedAt.setText(formattedUpdated);
            tvUpdatedAt.setVisibility(View.VISIBLE);
        } else {
            tvUpdatedAt.setVisibility(View.GONE);
        }
    }

    private int getCategoryColor(String category) {
        switch (category) {
            case "창업":
                return 0xFFFF4458;  // 빨강
            case "공모전":
                return 0xFF4C84FF;  // 파랑
            case "교육":
                return 0xFF2AA876;  // 초록
            case "기타":
            default:
                return 0xFF999999;  // 회색
        }
    }

    private void showEditDialog() {
        if (currentEvent == null) {
            Toast.makeText(this, "일정 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        CalendarEventDialogHelper.showEditEventDialog(
                this,
                currentEvent,
                event -> {
                    // 수정 성공 시 콜백
                    // LiveData가 자동으로 UI를 업데이트하므로 별도 처리 불필요
                    Toast.makeText(this, "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                },
                event -> {
                    // 삭제 성공 시 콜백 (다이얼로그 내에서 삭제 가능)
                    Toast.makeText(this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
        );
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("일정 삭제")
                .setMessage("이 일정을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteEvent())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteEvent() {
        if (currentEvent == null) {
            Toast.makeText(this, "일정 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.deleteEvent(currentEvent.getId(), new CalendarEventRepository.OnRefreshCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(CalendarEventDetailActivity.this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CalendarEventDetailActivity.this, "삭제 실패: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
