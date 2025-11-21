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
