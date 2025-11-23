package com.example.mobile_android.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.databinding.FragmentCalendarBinding;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.util.DateTimeUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private LocalDate selectedDate;
    private DayAdapter dayAdapter;
    private EventListAdapter eventListAdapter;
    private PostDao postDao;
    private List<Post> allSavedPosts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        postDao = AppDatabase.getInstance(requireContext()).postDao();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedDate = LocalDate.now(DateTimeUtils.getKstZoneId());

        setupCalendarView();
        setupEventListView();
        observeSavedPosts();

        refreshMonth();

        binding.btnPrev.setOnClickListener(v -> {
            selectedDate = selectedDate.minusMonths(1);
            refreshMonth();
        });

        binding.btnNext.setOnClickListener(v -> {
            selectedDate = selectedDate.plusMonths(1);
            refreshMonth();
        });

        binding.btnAddEvent.setOnClickListener(v -> {
            android.widget.Toast.makeText(requireContext(),
                "일정 추가 기능은 게시물을 저장하여 캘린더에 표시할 수 있습니다",
                android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCalendarView() {
        dayAdapter = new DayAdapter(day -> {
            selectedDate = day;
            dayAdapter.setSelected(selectedDate);
            filterEventsForSelectedDate();
        }, selectedDate);

        binding.rvDays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        binding.rvDays.setAdapter(dayAdapter);
    }

    private void setupEventListView() {
        eventListAdapter = new EventListAdapter();
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEvents.setAdapter(eventListAdapter);
    }

    private void observeSavedPosts() {
        postDao.getSavedPosts().observe(getViewLifecycleOwner(), posts -> {
            allSavedPosts = posts;
            // 이제 getCalendarAnchorDate()를 사용하여 대표 날짜를 가져옵니다.
            List<LocalDate> eventDates = posts.stream()
                    .map(post -> DateTimeUtils.parseServerDateToLocalDate(post.getCalendarAnchorDate()))
                    .collect(Collectors.toList());
            dayAdapter.setEventDates(eventDates);
            filterEventsForSelectedDate();
        });
    }

    private void filterEventsForSelectedDate() {
        List<Post> eventsForDay = allSavedPosts.stream()
                .filter(post -> {
                    // 여기도 getCalendarAnchorDate()를 사용합니다.
                    LocalDate eventDate = DateTimeUtils.parseServerDateToLocalDate(post.getCalendarAnchorDate());
                    return eventDate != null && eventDate.equals(selectedDate);
                })
                .collect(Collectors.toList());
        eventListAdapter.submitList(eventsForDay);

        // 선택된 날짜 표시 업데이트
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M월 d일 일정", Locale.KOREAN);
        binding.tvSelectedDate.setText(selectedDate.format(dateFmt));

        binding.tvNoEvents.setVisibility(eventsForDay.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshMonth() {
        DateTimeFormatter headerFmt = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN);
        binding.tvMonth.setText(selectedDate.format(headerFmt));

        List<LocalDate> days = buildDaysOfMonthNoNulls(selectedDate);
        dayAdapter.submit(days);
        dayAdapter.setSelected(selectedDate);
        filterEventsForSelectedDate();
    }

    /**
     * 실제 달력처럼 요일에 맞춰 날짜를 배치합니다.
     * 이전 달과 다음 달의 날짜를 null로 채워서 그리드가 올바르게 표시되도록 합니다.
     */
    private List<LocalDate> buildDaysOfMonthNoNulls(LocalDate base) {
        List<LocalDate> result = new ArrayList<>();
        YearMonth ym = YearMonth.from(base);
        LocalDate firstDayOfMonth = LocalDate.of(base.getYear(), base.getMonth(), 1);

        // 월의 첫날이 무슨 요일인지 확인 (1 = 월요일, 7 = 일요일)
        // getDayOfWeek().getValue()는 1(월) ~ 7(일)을 반환
        // 우리는 일요일(0) ~ 토요일(6)으로 변환
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7; // 일요일=0, 월요일=1, ..., 토요일=6

        // 첫 주의 빈 칸을 null로 채우기
        for (int i = 0; i < firstDayOfWeek; i++) {
            result.add(null);
        }

        // 실제 날짜 채우기
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
