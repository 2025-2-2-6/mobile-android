package com.example.mobile_android.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.CalendarEventDao;
import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.example.mobile_android.util.TokenManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 캘린더 이벤트 Repository
 * - 백엔드 API와 Room DB를 동기화
 * - Site/Post와 동일한 패턴: 항상 서버에서 최신 데이터를 가져와 로컬 DB에 덮어쓰기
 */
public class CalendarEventRepository {

    private static final String TAG = "CalendarEventRepo";
    private static volatile CalendarEventRepository INSTANCE;

    private final CalendarEventDao calendarEventDao;
    private final ApiService apiService;
    private final Context context;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private CalendarEventRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        calendarEventDao = db.calendarEventDao();
        apiService = ApiClient.getApiService();
    }

    public static CalendarEventRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CalendarEventRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CalendarEventRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Room DB에서 모든 이벤트를 LiveData로 관찰
     */
    public LiveData<List<CalendarEvent>> getAllEvents() {
        return calendarEventDao.getAllEvents();
    }

    /**
     * Room DB에서 특정 날짜의 이벤트를 LiveData로 관찰
     */
    public LiveData<List<CalendarEvent>> getEventsByDate(String date) {
        return calendarEventDao.getEventsByDate(date);
    }

    /**
     * Room DB에서 이벤트가 있는 날짜 목록을 LiveData로 관찰
     */
    public LiveData<List<String>> getEventDates() {
        return calendarEventDao.getEventDates();
    }

    /**
     * 백엔드에서 최신 일정 목록을 가져와 로컬 DB에 덮어쓰기
     * - Site/Post 패턴과 동일: 기존 데이터 삭제 후 새 데이터 삽입
     * - AuthInterceptor가 자동으로 Authorization 헤더 추가
     */
    public void refreshFromServer(OnRefreshCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "refreshFromServer: user not authenticated");
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        String userId = user.getUid();
        String token = TokenManager.getBearerToken(context);

        if (token == null || token.isEmpty()) {
            Log.w(TAG, "refreshFromServer: token is null");
            if (callback != null) callback.onFailure("Token is null");
            return;
        }

        // 백엔드 API 호출
        // user_id, start_date, end_date는 생략하여 모든 일정 조회
        apiService.getEvents(token, null, null, null).enqueue(new Callback<List<CalendarEvent>>() {
            @Override
            public void onResponse(Call<List<CalendarEvent>> call, Response<List<CalendarEvent>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CalendarEvent> events = response.body();
                    Log.d(TAG, "Fetched " + events.size() + " events from server");

                    // 백그라운드 스레드에서 로컬 DB 업데이트
                    ioExecutor.execute(() -> {
                        // 1. 기존 사용자의 모든 일정 삭제
                        calendarEventDao.deleteByUserId(userId);

                        // 2. 서버에서 가져온 데이터 삽입
                        if (!events.isEmpty()) {
                            calendarEventDao.insertAll(events);
                        }

                        Log.d(TAG, "Local DB updated with " + events.size() + " events");
                    });

                    if (callback != null) callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to fetch events: " + response.code());
                    if (callback != null) callback.onFailure("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<CalendarEvent>> call, Throwable t) {
                Log.e(TAG, "Network error while fetching events", t);
                if (callback != null) callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * 새 일정 생성 (백엔드 + 로컬 DB)
     */
    public void createEvent(CalendarEvent event, OnEventCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "createEvent: user not authenticated");
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        String token = TokenManager.getBearerToken(context);
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "createEvent: token is null");
            if (callback != null) callback.onFailure("Token is null");
            return;
        }

        // 백엔드 API 호출
        apiService.createEvent(token, event).enqueue(new Callback<CalendarEvent>() {
            @Override
            public void onResponse(Call<CalendarEvent> call, Response<CalendarEvent> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CalendarEvent createdEvent = response.body();
                    Log.d(TAG, "Event created on server: " + createdEvent.getId());

                    // 로컬 DB에 삽입
                    ioExecutor.execute(() -> calendarEventDao.insert(createdEvent));

                    if (callback != null) callback.onSuccess(createdEvent);
                } else {
                    Log.e(TAG, "Failed to create event: " + response.code());
                    if (callback != null) callback.onFailure("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CalendarEvent> call, Throwable t) {
                Log.e(TAG, "Network error while creating event", t);
                if (callback != null) callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * 일정 수정 (백엔드 + 로컬 DB)
     */
    public void updateEvent(String eventId, CalendarEvent event, OnEventCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "updateEvent: user not authenticated");
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        String token = TokenManager.getBearerToken(context);
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "updateEvent: token is null");
            if (callback != null) callback.onFailure("Token is null");
            return;
        }

        // 백엔드 API 호출
        apiService.updateEvent(token, eventId, event).enqueue(new Callback<CalendarEvent>() {
            @Override
            public void onResponse(Call<CalendarEvent> call, Response<CalendarEvent> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CalendarEvent updatedEvent = response.body();
                    Log.d(TAG, "Event updated on server: " + updatedEvent.getId());

                    // 로컬 DB 업데이트
                    ioExecutor.execute(() -> calendarEventDao.update(updatedEvent));

                    if (callback != null) callback.onSuccess(updatedEvent);
                } else {
                    Log.e(TAG, "Failed to update event: " + response.code());
                    if (callback != null) callback.onFailure("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CalendarEvent> call, Throwable t) {
                Log.e(TAG, "Network error while updating event", t);
                if (callback != null) callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * 일정 삭제 (백엔드 + 로컬 DB + Post isSaved 업데이트)
     */
    public void deleteEvent(String eventId, OnRefreshCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "deleteEvent: user not authenticated");
            if (callback != null) callback.onFailure("User not authenticated");
            return;
        }

        String token = TokenManager.getBearerToken(context);
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "deleteEvent: token is null");
            if (callback != null) callback.onFailure("Token is null");
            return;
        }

        // 백엔드 API 호출
        apiService.deleteEvent(token, eventId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Event deleted on server: " + eventId);

                    // 로컬 DB에서 삭제 및 Post isSaved 업데이트
                    ioExecutor.execute(() -> {
                        // 삭제하기 전에 post_id 가져오기
                        CalendarEvent event = calendarEventDao.getEventById(eventId);
                        String postId = (event != null) ? event.getPostId() : null;

                        // 일정 삭제
                        calendarEventDao.deleteById(eventId);

                        // post_id가 있으면 해당 Post의 isSaved를 false로 업데이트
                        if (!TextUtils.isEmpty(postId)) {
                            AppDatabase.getInstance(context).postDao().updateSaveState(postId, false);
                            Log.d(TAG, "Updated post isSaved=false for postId: " + postId);
                        }
                    });

                    if (callback != null) callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete event: " + response.code());
                    if (callback != null) callback.onFailure("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Network error while deleting event", t);
                if (callback != null) callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * 콜백 인터페이스
     */
    public interface OnRefreshCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnEventCallback {
        void onSuccess(CalendarEvent event);
        void onFailure(String error);
    }
}
