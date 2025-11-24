package com.example.mobile_android.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.mobile_android.model.CalendarEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 캘린더 일정 알림을 관리하는 클래스
 * AlarmManager를 사용하여 지정된 시간에 알림을 트리거합니다.
 */
public class CalendarEventAlarmManager {

    private static final String TAG = "CalendarEventAlarm";

    // Intent extras
    public static final String EXTRA_EVENT_ID = "event_id";
    public static final String EXTRA_EVENT_TITLE = "event_title";
    public static final String EXTRA_EVENT_CATEGORY = "event_category";
    public static final String EXTRA_EVENT_MEMO = "event_memo";

    /**
     * 일정 알림을 스케줄링합니다.
     *
     * @param context Context
     * @param event   CalendarEvent 객체
     */
    public static void scheduleAlarm(Context context, CalendarEvent event) {
        if (event == null || !event.isAlarmEnabled()) {
            Log.d(TAG, "Alarm not enabled for event: " + (event != null ? event.getTitle() : "null"));
            return;
        }

        if (event.getEventDate() == null || event.getEventDate().isEmpty()) {
            Log.e(TAG, "Event date is null or empty for event: " + event.getTitle());
            return;
        }

        long alarmTimeMillis = calculateAlarmTime(event);
        if (alarmTimeMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Alarm time is in the past, not scheduling: " + event.getTitle());
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        Intent intent = new Intent(context, CalendarEventAlarmReceiver.class);
        intent.putExtra(EXTRA_EVENT_ID, event.getId());
        intent.putExtra(EXTRA_EVENT_TITLE, event.getTitle());
        intent.putExtra(EXTRA_EVENT_CATEGORY, event.getCategory());
        intent.putExtra(EXTRA_EVENT_MEMO, event.getMemo());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                event.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Android 12+ (API 31+)에서는 SCHEDULE_EXACT_ALARM 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Exact alarm scheduled for: " + event.getTitle() + " at " + new Date(alarmTimeMillis));
            } else {
                Log.e(TAG, "Cannot schedule exact alarms. Permission not granted.");
                // Fallback: inexact alarm
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Inexact alarm scheduled for: " + event.getTitle());
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTimeMillis,
                    pendingIntent
            );
            Log.d(TAG, "Alarm scheduled for: " + event.getTitle() + " at " + new Date(alarmTimeMillis));
        }
    }

    /**
     * 일정 알림을 취소합니다.
     *
     * @param context Context
     * @param eventId 일정 ID
     */
    public static void cancelAlarm(Context context, int eventId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        Intent intent = new Intent(context, CalendarEventAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();

        Log.d(TAG, "Alarm cancelled for event ID: " + eventId);
    }

    /**
     * 알림 시간을 계산합니다.
     *
     * @param event CalendarEvent 객체
     * @return 알림 시간 (밀리초)
     */
    private static long calculateAlarmTime(CalendarEvent event) {
        try {
            String eventDate = event.getEventDate(); // "2024-01-15"
            String eventTime = event.getEventTime(); // "15:00" or null

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dateFormat.parse(eventDate);

            if (date == null) {
                Log.e(TAG, "Failed to parse event date: " + eventDate);
                return 0;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            // 시간이 지정되어 있으면 설정, 없으면 09:00으로 기본 설정
            if (eventTime != null && !eventTime.isEmpty()) {
                String[] timeParts = eventTime.split(":");
                if (timeParts.length == 2) {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                }
            } else {
                // 시간 미지정 시 오전 9시
                calendar.set(Calendar.HOUR_OF_DAY, 9);
                calendar.set(Calendar.MINUTE, 0);
            }

            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long eventTimeMillis = calendar.getTimeInMillis();

            // alarmTime 옵션에 따라 알림 시간 계산
            String alarmTime = event.getAlarmTime();
            if (alarmTime == null || alarmTime.isEmpty() || alarmTime.equals("일정 시작시간")) {
                return eventTimeMillis;
            }

            switch (alarmTime) {
                case "10분 전":
                    return eventTimeMillis - (10 * 60 * 1000);
                case "1시간 전":
                    return eventTimeMillis - (60 * 60 * 1000);
                case "1일 전":
                    return eventTimeMillis - (24 * 60 * 60 * 1000L);
                case "3일 전":
                    return eventTimeMillis - (3 * 24 * 60 * 60 * 1000L);
                case "일주일 전":
                    return eventTimeMillis - (7 * 24 * 60 * 60 * 1000L);
                default:
                    return eventTimeMillis;
            }

        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse event date: " + event.getEventDate(), e);
            return 0;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse event time: " + event.getEventTime(), e);
            return 0;
        }
    }

    /**
     * 알림 시간을 다시 계산하여 업데이트합니다.
     *
     * @param context Context
     * @param event   수정된 CalendarEvent 객체
     */
    public static void updateAlarm(Context context, CalendarEvent event) {
        // 기존 알림 취소 후 새로 스케줄링
        cancelAlarm(context, event.getId());
        scheduleAlarm(context, event);
    }
}
