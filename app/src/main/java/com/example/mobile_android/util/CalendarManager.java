package com.example.mobile_android.util;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Date;

public class CalendarManager {

    private static final String PREFS_NAME = com.example.mobile_android.config.AppConfig.PREF_CALENDAR;
    private static final String KEY_CALENDAR_EVENT_PREFIX = "event_";

    private final Context context;
    private final ContentResolver contentResolver;

    public CalendarManager(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    /**
     * 캘린더 권한 체크
     */
    public boolean hasCalendarPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 게시물을 캘린더에 일정으로 추가
     */
    public long addEventToCalendar(String postId, String title, String eventDateStr, String location) {
        if (!hasCalendarPermission()) {
            Toast.makeText(context, "캘린더 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return -1;
        }

        try {
            Date eventDate = DateTimeUtils.parseServerDate(eventDateStr);
            if (eventDate == null) {
                Toast.makeText(context, "날짜 형식 오류", Toast.LENGTH_SHORT).show();
                return -1;
            }

            long calendarId = getDefaultCalendarId();
            if (calendarId == -1) {
                Toast.makeText(context, "사용 가능한 캘린더가 없습니다", Toast.LENGTH_SHORT).show();
                return -1;
            }

            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, "게시물에서 추가된 일정");
            values.put(CalendarContract.Events.DTSTART, eventDate.getTime());
            values.put(CalendarContract.Events.DTEND, eventDate.getTime() + (60 * 60 * 1000)); // 1시간 후
            values.put(CalendarContract.Events.EVENT_TIMEZONE, DateTimeUtils.getKstTimeZone().getID());

            if (location != null && !location.isEmpty()) {
                values.put(CalendarContract.Events.EVENT_LOCATION, location);
            }

            values.put(CalendarContract.Events.HAS_ALARM, 1);

            Uri eventUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
            if (eventUri != null) {
                long eventId = Long.parseLong(eventUri.getLastPathSegment());

                addReminder(eventId, 1440); // 1일 전
                addReminder(eventId, 60);   // 1시간 전

                saveEventId(postId, eventId);

                Toast.makeText(context, "캘린더에 일정이 추가되었습니다", Toast.LENGTH_SHORT).show();
                return eventId;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "일정 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return -1;
    }    /**
     * 캘린더에서 일정 삭제
     */
    public boolean removeEventFromCalendar(String postId) {
        if (!hasCalendarPermission()) {
            Toast.makeText(context, "캘린더 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        long eventId = getEventId(postId);
        if (eventId == -1) {
            Toast.makeText(context, "등록된 일정이 없습니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
            int rows = contentResolver.delete(deleteUri, null, null);

            if (rows > 0) {
                removeEventId(postId);
                Toast.makeText(context, "캘린더에서 일정이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "일정 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    /**
     * 게시물이 캘린더에 등록되어 있는지 확인
     */
    public boolean isEventRegistered(String postId) {
        return getEventId(postId) != -1;
    }

    /**
     * 기본 캘린더 ID 가져오기
     */
    private long getDefaultCalendarId() {
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
        };

        Cursor cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1",
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            long calendarId = cursor.getLong(0);
            cursor.close();
            return calendarId;
        }

        if (cursor != null) {
            cursor.close();
        }

        return -1;
    }

    /**
     * 알림 추가
     */
    private void addReminder(long eventId, int minutes) {
        ContentValues reminderValues = new ContentValues();
        reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
        reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
        reminderValues.put(CalendarContract.Reminders.MINUTES, minutes);

        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);
    }

    /**
     * SharedPreferences에 이벤트 ID 저장
     */
    private void saveEventId(String postId, long eventId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_CALENDAR_EVENT_PREFIX + postId, eventId).apply();
    }

    /**
     * SharedPreferences에서 이벤트 ID 가져오기
     */
    private long getEventId(String postId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_CALENDAR_EVENT_PREFIX + postId, -1);
    }

    /**
     * SharedPreferences에서 이벤트 ID 삭제
     */
    private void removeEventId(String postId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CALENDAR_EVENT_PREFIX + postId).apply();
    }
}

