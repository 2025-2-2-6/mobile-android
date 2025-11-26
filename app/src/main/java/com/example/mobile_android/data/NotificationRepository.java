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
     * 임시 더미 데이터 삽입 (테스트용)
     * 5가지 타입: 캘린더 일정, 크롤링 성공, 크롤링 실패, 새 게시물, unknown
     */
    public void insertDummyNotifications(String userId) {
        if (TextUtils.isEmpty(userId)) {
            Log.w(TAG, "insertDummyNotifications: missing user id");
            return;
        }

        ioExecutor.execute(() -> {
            long now = System.currentTimeMillis();

            // 1. 캘린더 일정 알림 (calendar_reminder)
            NotificationEntity calendarReminder = NotificationEntity.from(
                    UUID.randomUUID().toString(),
                    userId,
                    "calendar_reminder",
                    "일정 알림",
                    "내일 오전 9시 '2025 창업 아이디어 경진대회' 일정이 있습니다.",
                    "post_calendar_001",
                    "site_calendar",
                    false,
                    "2025-11-24T10:00:00Z",
                    "2025-11-25T09:00:00Z",
                    "2025-11-25T12:00:00Z",
                    now - 3600000
            );

            // 2. 크롤링 성공 (crawl_new_posts - success)
            NotificationEntity crawlSuccess = NotificationEntity.from(
                    UUID.randomUUID().toString(),
                    userId,
                    "crawl_new_posts",
                    "사이트 등록 완료",
                    "서울대학교 공지사항 크롤링이 완료되었습니다. 새로운 게시물 5개가 추가되었습니다.",
                    null,
                    "site_success_001",
                    false,
                    "2025-11-24T09:30:00Z",
                    null,
                    null,
                    now - 7200000
            );
            crawlSuccess.setCrawlStatus("success");

            // 3. 크롤링 실패 (crawl_new_posts - failed)
            NotificationEntity crawlFailed = NotificationEntity.from(
                    UUID.randomUUID().toString(),
                    userId,
                    "crawl_new_posts",
                    "크롤링 실패",
                    "고려대학교 공지사항 크롤링 중 오류가 발생했습니다. 사이트 접근이 차단되었습니다.",
                    null,
                    "site_failed_001",
                    false,
                    "2025-11-24T08:15:00Z",
                    null,
                    null,
                    now - 10800000
            );
            crawlFailed.setCrawlStatus("failed");

            // 4. 새 게시물 업데이트 (crawl_new_posts - new_post) - 스케줄링 도중
            NotificationEntity newPost = NotificationEntity.from(
                    UUID.randomUUID().toString(),
                    userId,
                    "crawl_new_posts",
                    "연세대학교 공지사항",
                    "[긴급] 2025-1학기 수강신청 일정 변경 안내",
                    "post_newpost_001",
                    "site_newpost_001",
                    false,
                    "2025-11-24T11:45:00Z",
                    null,
                    null,
                    now - 1800000
            );
            newPost.setCrawlStatus("new_post");

            // 5. unknown (회색) - 공지사항 등
            NotificationEntity unknownNotice = NotificationEntity.from(
                    UUID.randomUUID().toString(),
                    userId,
                    "unknown",
                    "서비스 공지",
                    "새로운 기능이 추가되었습니다! 캘린더에서 일정을 관리해보세요.",
                    null,
                    null,
                    false,
                    "2025-11-23T18:00:00Z",
                    null,
                    null,
                    now - 86400000
            );

            notificationDao.upsert(calendarReminder);
            notificationDao.upsert(crawlSuccess);
            notificationDao.upsert(crawlFailed);
            notificationDao.upsert(newPost);
            notificationDao.upsert(unknownNotice);

            Log.d(TAG, "더미 알림 데이터 삽입 완료 (5가지 타입)");
        });
    }
}
