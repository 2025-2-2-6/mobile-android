package com.example.mobile_android.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.NotificationDao;
import com.example.mobile_android.data.local.NotificationEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room DB 기반 알림 관리 (로컬 전용)
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static volatile NotificationRepository INSTANCE;

    private final NotificationDao notificationDao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        notificationDao = db.notificationDao();
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

    // refreshFromServer 제거: 알림은 Room DB로만 관리

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

        // crawlStatus 추가 설정
        entity.setCrawlStatus(data.get("status"));

        ioExecutor.execute(() -> notificationDao.upsert(entity));
    }

    // mapToEntities 제거: 백엔드 조회를 사용하지 않음

    private String safeId(String id) {
        return TextUtils.isEmpty(id) ? UUID.randomUUID().toString() : id;
    }

    /**
     * 더미 데이터 정리 (테스트용 더미 데이터 삭제)
     * "사이트 등록 완료", "크롤링 실패" 등의 더미 알림을 삭제합니다.
     */
    public void clearDummyData(String userId) {
        if (TextUtils.isEmpty(userId)) return;

        ioExecutor.execute(() -> {
            // 더미 데이터의 특징적인 제목으로 식별하여 삭제
            List<String> dummyTitles = java.util.Arrays.asList(
                "사이트 등록 완료",
                "크롤링 실패",
                "연세대학교 공지사항",
                "서비스 공지",
                "일정 알림"
            );

            // 각 더미 제목에 해당하는 알림 삭제
            for (String title : dummyTitles) {
                notificationDao.deleteByTitle(userId, title);
            }

            Log.d(TAG, "더미 알림 데이터 정리 완료");
        });
    }
}
