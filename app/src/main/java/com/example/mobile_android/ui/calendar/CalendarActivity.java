package com.example.mobile_android.ui.calendar;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.mobile_android.R;
import com.example.mobile_android.databinding.ActivityCalendarBinding;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private ActivityCalendarBinding binding;
    private LocalDate selectedDate;
    private DayAdapter dayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Header 제어
        View header = findViewById(R.id.headerBar);
        ImageButton btnMenu = header.findViewById(R.id.btn_menu);
        ImageButton btnNotification = header.findViewById(R.id.btn_notification);
        TextView toolbarTitle = header.findViewById(R.id.toolbar_title);

        toolbarTitle.setText("캘린더");
        btnMenu.setOnClickListener(v ->
                Toast.makeText(this, "메뉴 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        );
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "알림 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        );

        // 날짜/어댑터 준비
        selectedDate = LocalDate.now();

        // DayAdapter: (OnDayClick, LocalDate) 시그니처 가정
        dayAdapter = new DayAdapter(day -> {
            // null 안 넘기니 별도 체크 불필요
            selectedDate = day;
            dayAdapter.setSelected(selectedDate);
        }, selectedDate);

        binding.rvDays.setLayoutManager(new GridLayoutManager(this, 7));
        binding.rvDays.setAdapter(dayAdapter);

        // 첫 렌더링
        refreshMonth();

        // 이전/다음 달
        binding.btnPrev.setOnClickListener(v -> {
            selectedDate = selectedDate.minusMonths(1);
            refreshMonth();
        });

        binding.btnNext.setOnClickListener(v -> {
            selectedDate = selectedDate.plusMonths(1);
            refreshMonth();
        });
    }

    /** 현재 달 헤더 갱신 + 그리드 데이터 주입 */
    private void refreshMonth() {
        DateTimeFormatter headerFmt = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN);
        binding.tvMonth.setText(selectedDate.format(headerFmt));

        // ✅ null 없이 실제 날짜만 전달
        List<LocalDate> days = buildDaysOfMonthNoNulls(selectedDate);
        dayAdapter.submit(days);
        dayAdapter.setSelected(selectedDate);
    }

    /** 해당 월의 '실제 날짜'들만 반환 (null 없음) */
    private List<LocalDate> buildDaysOfMonthNoNulls(LocalDate base) {
        List<LocalDate> result = new ArrayList<>();
        YearMonth ym = YearMonth.from(base);
        int daysInMonth = ym.lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            result.add(LocalDate.of(base.getYear(), base.getMonth(), d));
        }
        return result;
    }
}
