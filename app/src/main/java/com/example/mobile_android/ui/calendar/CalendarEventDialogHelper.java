package com.example.mobile_android.ui.calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.mobile_android.R;
import com.example.mobile_android.model.CalendarEvent;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 캘린더 일정 추가/수정 다이얼로그 헬퍼
 * dialog_calendar_event.xml 레이아웃 사용
 */
public class CalendarEventDialogHelper {

    private static final String[] ALARM_OPTIONS = {
            "일정 시작시간",
            "10분 전",
            "1시간 전",
            "1일 전",
            "3일 전",
            "일주일 전"
    };

    public interface OnEventSavedListener {
        void onSaved(CalendarEvent event);
    }

    public interface OnEventDeletedListener {
        void onDeleted(CalendarEvent event);
    }

    /**
     * 일정 추가 다이얼로그 표시
     */
    public static void showAddEventDialog(Context context, String selectedDate,
                                          OnEventSavedListener listener) {
        showEventDialog(context, null, selectedDate, listener, null);
    }

    /**
     * 일정 수정 다이얼로그 표시
     */
    public static void showEditEventDialog(Context context, CalendarEvent event,
                                           OnEventSavedListener saveListener,
                                           OnEventDeletedListener deleteListener) {
        showEventDialog(context, event, null, saveListener, deleteListener);
    }

    private static void showEventDialog(Context context, CalendarEvent existingEvent,
                                        String selectedDate,
                                        OnEventSavedListener saveListener,
                                        OnEventDeletedListener deleteListener) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_calendar_event, null);

        // Views
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etEventTitle = dialogView.findViewById(R.id.et_event_title);
        TextView tvDatePicker = dialogView.findViewById(R.id.tv_date_picker);
        TextView tvTimePicker = dialogView.findViewById(R.id.tv_time_picker);
        EditText etMemo = dialogView.findViewById(R.id.et_memo);
        SwitchMaterial switchAlarm = dialogView.findViewById(R.id.switch_alarm);
        Spinner spinnerAlarmTime = dialogView.findViewById(R.id.spinner_alarm_time);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // 알림 시간 Spinner 설정
        ArrayAdapter<String> alarmAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, ALARM_OPTIONS);
        alarmAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAlarmTime.setAdapter(alarmAdapter);

        // 현재 시간 초기화
        Calendar calendar = Calendar.getInstance();
        final String[] eventDate = {selectedDate != null ? selectedDate : formatDate(calendar)};
        final String[] eventTime = {formatTime(calendar)};

        // 수정 모드인 경우 기존 데이터 로드
        boolean isEditMode = existingEvent != null;
        if (isEditMode) {
            tvTitle.setText("일정 수정");
            btnSave.setText("수정하기");

            etEventTitle.setText(existingEvent.getTitle());
            etMemo.setText(existingEvent.getDescription());
            eventDate[0] = existingEvent.getEventDate();
            eventTime[0] = existingEvent.getEventTime();
            switchAlarm.setChecked(existingEvent.isNotifyEnabled());
            spinnerAlarmTime.setVisibility(existingEvent.isNotifyEnabled() ? View.VISIBLE : View.GONE);

            // 알림 시간 선택
            if (existingEvent.getNotifyTime() != null) {
                // notifyTime은 ISO 8601 형식이므로 여기서는 기본값 사용
                spinnerAlarmTime.setSelection(0);
            }
        } else {
            tvTitle.setText("일정 추가");
            btnSave.setText("추가하기");
        }

        // 날짜/시간 표시 업데이트
        updateDateTimeDisplay(tvDatePicker, eventDate[0]);
        updateTimeDisplay(tvTimePicker, eventTime[0]);

        // 날짜 선택 다이얼로그
        tvDatePicker.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (eventDate[0] != null) {
                try {
                    String[] parts = eventDate[0].split("-");
                    cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                } catch (Exception e) {
                    // 파싱 실패 시 현재 날짜 사용
                }
            }

            new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
                eventDate[0] = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                updateDateTimeDisplay(tvDatePicker, eventDate[0]);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 시간 선택 다이얼로그
        tvTimePicker.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (eventTime[0] != null) {
                try {
                    String[] parts = eventTime[0].split(":");
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                    cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
                } catch (Exception e) {
                    // 파싱 실패 시 현재 시간 사용
                }
            }

            new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                eventTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                updateTimeDisplay(tvTimePicker, eventTime[0]);
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
        });

        // 알림 스위치
        switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spinnerAlarmTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // 다이얼로그 생성
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 저장 버튼
        btnSave.setOnClickListener(v -> {
            String title = etEventTitle.getText().toString().trim();
            if (title.isEmpty()) {
                etEventTitle.setError("일정 제목을 입력해주세요");
                return;
            }

            CalendarEvent event;
            if (isEditMode) {
                event = existingEvent;
            } else {
                event = new CalendarEvent();
            }

            event.setTitle(title);
            event.setCategory(null); // 카테고리는 더 이상 사용하지 않음
            event.setDescription(etMemo.getText().toString().trim());

            // startTime을 ISO 8601 형식으로 변환
            String startTimeISO = convertToISO8601(eventDate[0], eventTime[0]);
            event.setStartTime(startTimeISO);
            event.setEndTime(null); // endTime은 선택사항이므로 null

            event.setNotifyEnabled(switchAlarm.isChecked());
            if (switchAlarm.isChecked()) {
                // notifyTime 계산 (alarmTime 옵션에 따라)
                String notifyTimeISO = calculateNotifyTime(startTimeISO, spinnerAlarmTime.getSelectedItem().toString());
                event.setNotifyTime(notifyTimeISO);
            } else {
                event.setNotifyTime(null);
            }

            // 현재 시간 저장 (ISO 8601 형식)
            String currentTime = getCurrentTimestamp();
            if (!isEditMode) {
                event.setCreatedAt(currentTime);
            }
            event.setUpdatedAt(currentTime);

            if (saveListener != null) {
                saveListener.onSaved(event);
            }

            dialog.dismiss();
        });

        // 취소 버튼
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static void updateDateTimeDisplay(TextView tv, String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN);
            tv.setText(outputFormat.format(inputFormat.parse(dateStr)));
            tv.setTextColor(0xFF222222);
        } catch (Exception e) {
            tv.setText(dateStr);
        }
    }

    private static void updateTimeDisplay(TextView tv, String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String amPm = hour < 12 ? "오전" : "오후";
            int displayHour = hour % 12;
            if (displayHour == 0) displayHour = 12;
            tv.setText(String.format(Locale.KOREAN, "%s %02d:%02d", amPm, displayHour, minute));
            tv.setTextColor(0xFF222222);
        } catch (Exception e) {
            tv.setText(timeStr);
        }
    }

    private static String formatDate(Calendar calendar) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static String formatTime(Calendar calendar) {
        return String.format(Locale.getDefault(), "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    /**
     * YYYY-MM-DD와 HH:mm를 ISO 8601 형식으로 변환
     * 예: 2024-01-15, 14:30 -> 2024-01-15T14:30:00
     */
    private static String convertToISO8601(String date, String time) {
        return date + "T" + time + ":00";
    }

    /**
     * startTime과 알림 옵션을 기반으로 notifyTime 계산
     */
    private static String calculateNotifyTime(String startTimeISO, String alarmOption) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startTimeISO));

            switch (alarmOption) {
                case "10분 전":
                    cal.add(Calendar.MINUTE, -10);
                    break;
                case "1시간 전":
                    cal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1일 전":
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
                case "3일 전":
                    cal.add(Calendar.DAY_OF_MONTH, -3);
                    break;
                case "일주일 전":
                    cal.add(Calendar.DAY_OF_MONTH, -7);
                    break;
                case "일정 시작시간":
                default:
                    // 그대로 사용
                    break;
            }

            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return startTimeISO;
        }
    }
}
