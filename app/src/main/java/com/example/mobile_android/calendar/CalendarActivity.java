package com.example.mobile_android.calendar;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.mobile_android.databinding.ActivityCalendarBinding;
import com.example.mobile_android.R;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {
    private ActivityCalendarBinding b;
    private YearMonth currentMonth = YearMonth.of(2025, 4);
    private LocalDate selected = LocalDate.of(2025, 4, 20);
    private final LocalDate today = LocalDate.now();
    private DayAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.rvDays.setLayoutManager(new GridLayoutManager(this, 7));
        adapter = new DayAdapter(day -> {
            if (day.getMonth() == currentMonth.getMonth()) {
                selected = day;
                adapter.submit(buildMonthCells(currentMonth));
                adapter.setSelected(selected);
            }
        }, today);
        b.rvDays.setAdapter(adapter);

        b.btnPrev.setOnClickListener(v -> { currentMonth = currentMonth.minusMonths(1); refresh(); });
        b.btnNext.setOnClickListener(v -> { currentMonth = currentMonth.plusMonths(1); refresh(); });

        b.bottomNav.setSelectedItemId(R.id.tab_calendar);
        refresh();
    }

    private void refresh() {
        b.tvMonth.setText(currentMonth.getMonth().name().substring(0,1)
                + currentMonth.getMonth().name().substring(1).toLowerCase() + " " + currentMonth.getYear());
        adapter.submit(buildMonthCells(currentMonth));
        adapter.setSelected(selected);
    }

    private List<LocalDate> buildMonthCells(YearMonth ym) {
        List<LocalDate> out = new ArrayList<>(42);
        LocalDate first = ym.atDay(1);
        int shift = first.getDayOfWeek().getValue() % 7; // Sun=0
        LocalDate start = first.minusDays(shift);
        LocalDate d = start;
        for (int i = 0; i < 42; i++) { out.add(d); d = d.plusDays(1); }
        return out;
    }
}