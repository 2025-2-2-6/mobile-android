package com.example.mobile_android.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.mobile_android.R;
import com.example.mobile_android.databinding.FragmentCalendarBinding;
import com.example.mobile_android.util.DateTimeUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private LocalDate selectedDate;
    private DayAdapter dayAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 날짜/어댑터 준비
        selectedDate = LocalDate.now(DateTimeUtils.getKstZoneId());

        dayAdapter = new DayAdapter(day -> {
            selectedDate = day;
            dayAdapter.setSelected(selectedDate);
        }, selectedDate);

        binding.rvDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
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

    private void refreshMonth() {
        DateTimeFormatter headerFmt = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN);
        binding.tvMonth.setText(selectedDate.format(headerFmt));

        List<LocalDate> days = buildDaysOfMonthNoNulls(selectedDate);
        dayAdapter.submit(days);
        dayAdapter.setSelected(selectedDate);
    }

    private List<LocalDate> buildDaysOfMonthNoNulls(LocalDate base) {
        List<LocalDate> result = new ArrayList<>();
        YearMonth ym = YearMonth.from(base);
        int daysInMonth = ym.lengthOfMonth();
        for (int d = 1; d <= daysInMonth; d++) {
            result.add(LocalDate.of(base.getYear(), base.getMonth(), d));
        }
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}
