package com.example.mobile_android.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.NotificationDao;
import com.example.mobile_android.data.local.NotificationEntity;
import com.example.mobile_android.model.Notification;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Coordinates between Retrofit + Room for notifications.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static volatile NotificationRepository INSTANCE;

    private final NotificationDao notificationDao;
    private final ApiService apiService;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        notificationDao = db.notificationDao();
        apiService = ApiClient.getApiService();
    }

    public static NotificationRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NotificationRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NotificationRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<NotificationEntity>> getNotifications(String userId) {
        return notificationDao.observeNotifications(userId);
    }

    public LiveData<Integer> getUnreadCount(String userId) {
        return notificationDao.observeUnreadCount(userId);
    }

    public void refreshFromServer(String userId) {
        if (TextUtils.isEmpty(userId)) {
            Log.w(TAG, "refreshFromServer: userId is empty");
            return;
        }

        apiService.getNotifications(userId, null, null).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Notification> remote = response.body();
                    ioExecutor.execute(() -> notificationDao.upsert(mapToEntities(remote, userId)));
                } else {
                    Log.w(TAG, "Failed to refresh notifications: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                Log.e(TAG, "refreshFromServer error", t);
            }
        });
    }

    public void markAsRead(String notificationId) {
        if (TextUtils.isEmpty(notificationId)) return;

        ioExecutor.execute(() -> notificationDao.markAsRead(notificationId));
    }

    public void delete(String notificationId) {
        if (TextUtils.isEmpty(notificationId)) return;
        ioExecutor.execute(() -> notificationDao.deleteById(notificationId));
        // 서버 삭제 API가 없다면 로컬 삭제만 수행 (필요 시 구현)
    }

    public void cacheFcmPayload(Map<String, String> data) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userIdFromPayload = data.get("user_id");
        String userId = !TextUtils.isEmpty(userIdFromPayload)
                ? userIdFromPayload
                : (user != null ? user.getUid() : null);
        if (TextUtils.isEmpty(userId)) {
            Log.w(TAG, "cacheFcmPayload: missing user id");
            return;
        }

        String notificationId = data.containsKey("notification_id")
                ? data.get("notification_id")
                : UUID.randomUUID().toString();

        NotificationEntity entity = NotificationEntity.from(
                notificationId,
                userId,
                data.get("type"),
                data.getOrDefault("title", ""),
                data.getOrDefault("message", ""),
                data.get("post_id"),
                data.get("site_id"),
                false,
                data.get("created_at"),
                data.get("event_start_date"),
                data.get("event_end_date"),
                System.currentTimeMillis()
        );

        ioExecutor.execute(() -> notificationDao.upsert(entity));
    }

    private List<NotificationEntity> mapToEntities(List<Notification> remote, String userId) {
        List<NotificationEntity> list = new ArrayList<>();
        if (remote == null) {
            return list;
        }

        for (Notification notification : remote) {
            NotificationEntity entity = NotificationEntity.from(
                    safeId(notification.getId()),
                    userId,
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getPostId(),
                    notification.getSiteId(),
                    notification.isRead(),
                    notification.getCreatedAt(),
                    notification.getEventStartDate(),
                    notification.getEventEndDate(),
                    System.currentTimeMillis()
            );
            list.add(entity);
        }

        return list;
    }

    private String safeId(String id) {
        return TextUtils.isEmpty(id) ? UUID.randomUUID().toString() : id;
    }
}
