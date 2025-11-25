package com.example.mobile_android.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.example.mobile_android.data.CalendarEventRepository;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.CalendarEventDao;
import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.model.Post;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Post와 CalendarEvent 간의 연동을 처리하는 헬퍼 클래스
 * PostDetailActivity와 PostAdapter에서 공통으로 사용
 */
public class CalendarEventHelper {

    private static final String TAG = "CalendarEventHelper";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Post를 캘린더에 추가 (중복 체크 포함)
     */
    public static void addPostToCalendar(Context context, Post post, OnCalendarEventCallback callback) {
        if (context == null || post == null) {
            if (callback != null) callback.onFailure("Invalid context or post");
            return;
        }

        if (TextUtils.isEmpty(post.getCalendarAnchorDate())) {
            if (callback != null) callback.onFailure("일정 정보가 없습니다");
            return;
        }

        CalendarEventRepository repository = CalendarEventRepository.getInstance(context);
        CalendarEventDao calendarEventDao = AppDatabase.getInstance(context).calendarEventDao();

        // 백그라운드에서 중복 체크 후 생성
        executor.execute(() -> {
            try {
                // 중복 체크: 이미 해당 post_id로 캘린더 이벤트가 존재하는지 확인
                CalendarEvent existingEvent = calendarEventDao.getEventByPostId(post.getId());

                if (existingEvent != null) {
                    Log.w(TAG, "Calendar event already exists for post: " + post.getId());
                    // 메인 스레드에서 콜백 호출
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFailure("이미 캘린더에 추가된 일정입니다"));
                    }
                    return;
                }

                // CalendarEvent 생성
                CalendarEvent event = new CalendarEvent();
                event.setId(UUID.randomUUID().toString());
                event.setPostId(post.getId());
                event.setTitle(post.getTitle());
                event.setCategory(resolveCategory(context, post));
                event.setDescription(post.getContent());

                // calendarAnchorDate를 ISO 8601 형식으로 변환
                String startTime = convertToISO8601(post.getCalendarAnchorDate());
                event.setStartTime(startTime);
                event.setEndTime(null);

                // Post에서 생성되는 일정은 자동으로 알림 활성화 (일정 시작 날짜 08:00)
                event.setNotifyEnabled(true);
                event.setNotifyTime(calculateNotifyTime(startTime));

                // 백엔드에 저장
                repository.createEvent(event, new CalendarEventRepository.OnEventCallback() {
                    @Override
                    public void onSuccess(CalendarEvent createdEvent) {
                        Log.d(TAG, "Post added to calendar: " + post.getId());
                        // 메인 스레드에서 콜백 호출
                        if (callback != null) {
                            mainHandler.post(() -> callback.onSuccess());
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to add post to calendar: " + error);
                        // 메인 스레드에서 콜백 호출
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFailure(error));
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking duplicate calendar event", e);
                // 메인 스레드에서 콜백 호출
                if (callback != null) {
                    mainHandler.post(() -> callback.onFailure("일정 추가 중 오류 발생"));
                }
            }
        });
    }

    /**
     * Post의 캘린더 이벤트를 제거
     */
    public static void removePostFromCalendar(Context context, Post post, OnCalendarEventCallback callback) {
        if (context == null || post == null) {
            if (callback != null) callback.onFailure("Invalid context or post");
            return;
        }

        CalendarEventRepository repository = CalendarEventRepository.getInstance(context);
        CalendarEventDao calendarEventDao = AppDatabase.getInstance(context).calendarEventDao();

        // 백그라운드에서 post_id로 이벤트 찾기
        executor.execute(() -> {
            try {
                CalendarEvent event = calendarEventDao.getEventByPostId(post.getId());

                if (event == null) {
                    Log.w(TAG, "No calendar event found for post: " + post.getId());
                    // 메인 스레드에서 콜백 호출
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFailure("캘린더에 저장된 일정이 없습니다"));
                    }
                    return;
                }

                Log.d(TAG, "Found calendar event to delete - ID: " + event.getId() + ", Post ID: " + post.getId());

                // 백엔드에서 삭제
                repository.deleteEvent(event.getId(), new CalendarEventRepository.OnRefreshCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Post removed from calendar: " + post.getId());
                        // 메인 스레드에서 콜백 호출
                        if (callback != null) {
                            mainHandler.post(() -> callback.onSuccess());
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to remove post from calendar: " + error);
                        // 메인 스레드에서 콜백 호출
                        if (callback != null) {
                            mainHandler.post(() -> callback.onFailure(error));
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error removing post from calendar", e);
                // 메인 스레드에서 콜백 호출
                if (callback != null) {
                    mainHandler.post(() -> callback.onFailure("일정 제거 중 오류 발생"));
                }
            }
        });
    }

    /**
     * Post에서 카테고리 추출
     */
    private static String resolveCategory(Context context, Post post) {
        if (!TextUtils.isEmpty(post.getCategoryName())) {
            return post.getCategoryName();
        } else if (!TextUtils.isEmpty(post.getSiteName())) {
            return post.getSiteName();
        }
        return "기타";
    }

    /**
     * 날짜를 ISO 8601 형식으로 변환
     */
    private static String convertToISO8601(String dateTimeStr) {
        try {
            // 서버에서 오는 형식을 파싱
            SimpleDateFormat inputFormat;
            if (dateTimeStr.contains("T")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            }
            Date date = inputFormat.parse(dateTimeStr);

            // ISO 8601 형식으로 변환
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.w(TAG, "Failed to convert date format: " + dateTimeStr, e);
            return dateTimeStr;
        }
    }

    /**
     * 알림 시간 계산: 일정 시작 날짜의 08:00
     */
    private static String calculateNotifyTime(String startTime) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = format.parse(startTime);

            // 날짜 부분만 추출하여 08:00으로 설정
            SimpleDateFormat dateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String dateStr = dateOnly.format(date);

            return dateStr + "T08:00:00";
        } catch (Exception e) {
            Log.w(TAG, "Failed to calculate notify time: " + startTime, e);
            // 파싱 실패 시 원본 시간 반환
            return startTime;
        }
    }

    /**
     * 콜백 인터페이스
     */
    public interface OnCalendarEventCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
