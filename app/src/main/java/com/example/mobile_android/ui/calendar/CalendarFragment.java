package com.example.mobile_android.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.mobile_android.data.CalendarEventRepository;
import com.example.mobile_android.databinding.FragmentCalendarBinding;
import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.util.CalendarEventAlarmManager;
import com.example.mobile_android.util.DateTimeUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private LocalDate selectedDate;
    private DayAdapter dayAdapter;
    private CalendarEventAdapter eventListAdapter;
    private CalendarEventRepository repository;
    private List<CalendarEvent> allEvents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        repository = CalendarEventRepository.getInstance(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면으로 돌아올 때마다 자동 새로고침 (HomeFragment와 동일)
        refreshFromServer();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedDate = LocalDate.now(DateTimeUtils.getKstZoneId());

        setupCalendarView();
        setupEventListView();
        observeEvents();

        // SwipeRefreshLayout 설정
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshFromServer();
        });

        // 백엔드에서 최신 데이터 가져오기
        refreshFromServer();

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
            CalendarEventDialogHelper.showAddEventDialog(
                requireContext(),
                selectedDate.toString(),
                event -> saveEvent(event)
            );
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
        eventListAdapter = new CalendarEventAdapter(
            event -> {
                // 수정 버튼 클릭
                CalendarEventDialogHelper.showEditEventDialog(
                    requireContext(),
                    event,
                    updatedEvent -> updateEvent(updatedEvent),
                    deletedEvent -> deleteEvent(deletedEvent)
                );
            },
            event -> {
                // 삭제 버튼 클릭
                deleteEvent(event);
            }
        );
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEvents.setAdapter(eventListAdapter);
    }

    private void observeEvents() {
        repository.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            allEvents = events;

            // 이벤트가 있는 날짜 목록 생성 (null 안전 처리)
            List<LocalDate> eventDates = events.stream()
                    .filter(event -> event.getEventDate() != null && !event.getEventDate().isEmpty())
                    .map(event -> {
                        try {
                            return LocalDate.parse(event.getEventDate());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(date -> date != null)
                    .distinct()
                    .collect(Collectors.toList());

            dayAdapter.setEventDates(eventDates);
            filterEventsForSelectedDate();
        });
    }

    private void refreshFromServer() {
        if (binding.swipeRefresh != null) {
            binding.swipeRefresh.setRefreshing(true);
        }

        repository.refreshFromServer(new CalendarEventRepository.OnRefreshCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (binding.swipeRefresh != null) {
                            binding.swipeRefresh.setRefreshing(false);
                        }
                        // LiveData가 자동으로 UI 업데이트
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        if (binding.swipeRefresh != null) {
                            binding.swipeRefresh.setRefreshing(false);
                        }
                        Toast.makeText(requireContext(), "새로고침 실패: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void filterEventsForSelectedDate() {
        String dateStr = selectedDate.toString();

        List<CalendarEvent> eventsForDay = allEvents.stream()
                .filter(event -> event.getEventDate().equals(dateStr))
                .collect(Collectors.toList());

        eventListAdapter.submitList(eventsForDay);

        // 선택된 날짜 표시 업데이트
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M월 d일 일정", Locale.KOREAN);
        binding.tvSelectedDate.setText(selectedDate.format(dateFmt));

        binding.tvNoEvents.setVisibility(eventsForDay.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveEvent(CalendarEvent event) {
        // UUID 생성
        event.setId(UUID.randomUUID().toString());

        // 백엔드에 저장 (성공 시 자동으로 로컬 DB에도 저장됨)
        repository.createEvent(event, new CalendarEventRepository.OnEventCallback() {
            @Override
            public void onSuccess(CalendarEvent createdEvent) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "일정이 추가되었습니다", Toast.LENGTH_SHORT).show();

                        // 알람 스케줄링
                        if (createdEvent.isNotifyEnabled()) {
                            CalendarEventAlarmManager.scheduleAlarm(requireContext(), createdEvent);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "일정 추가 실패: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateEvent(CalendarEvent event) {
        // 백엔드 업데이트 (성공 시 자동으로 로컬 DB에도 업데이트됨)
        repository.updateEvent(event.getId(), event, new CalendarEventRepository.OnEventCallback() {
            @Override
            public void onSuccess(CalendarEvent updatedEvent) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "일정이 수정되었습니다", Toast.LENGTH_SHORT).show();

                        // 알람 업데이트 (기존 취소 후 재스케줄링)
                        CalendarEventAlarmManager.updateAlarm(requireContext(), updatedEvent);
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "일정 수정 실패: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void deleteEvent(CalendarEvent event) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("일정 삭제")
            .setMessage("이 일정을 삭제하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> {
                // 백엔드에서 삭제 (성공 시 자동으로 로컬 DB에서도 삭제됨)
                repository.deleteEvent(event.getId(), new CalendarEventRepository.OnRefreshCallback() {
                    @Override
                    public void onSuccess() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "일정이 삭제되었습니다", Toast.LENGTH_SHORT).show();

                                // 알람 취소
                                CalendarEventAlarmManager.cancelAlarm(requireContext(), event.getId());
                            });
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "일정 삭제 실패: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            })
            .setNegativeButton("취소", null)
            .show();
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
        binding = null;
    }
}
