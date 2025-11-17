package com.example.mobile_android.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * FCM 메시지를 수신하고 알림을 표시하는 서비스
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    // 알림 채널 ID
    private static final String CHANNEL_CRAWLING = "channel_crawling";
    private static final String CHANNEL_NEW_POST = "channel_new_post";
    private static final String CHANNEL_SCHEDULE = "channel_schedule";

    @Override
    public void onCreate() {
        super.onCreate();
        // 알림 채널 생성 (Android 8.0 이상)
        createNotificationChannels();
    }

    /**
     * FCM 토큰이 갱신될 때 호출됨
     * 새 토큰을 서버로 전송해야 함
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "새로운 FCM 토큰: " + token);

        // 토큰을 서버로 전송
        FcmTokenManager.sendTokenToServer(this, token);
    }

    /**
     * FCM 메시지를 수신했을 때 호출됨
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Log.d(TAG, "FCM 메시지 수신: " + message.getFrom());

        // 메시지 데이터 추출
        Map<String, String> data = message.getData();

        if (data.isEmpty()) {
            Log.w(TAG, "데이터가 없는 메시지");
            return;
        }

        // 알림 타입 확인
        String notificationType = data.get("type");

        if (notificationType == null) {
            Log.w(TAG, "알림 타입이 없음");
            return;
        }

        // 타입별로 처리
        switch (notificationType) {
            case "crawling_complete":
                handleCrawlingComplete(data);
                break;
            case "new_post":
                handleNewPost(data);
                break;
            case "schedule_reminder":
                handleScheduleReminder(data);
                break;
            default:
                Log.w(TAG, "알 수 없는 알림 타입: " + notificationType);
                break;
        }
    }

    /**
     * 크롤링 완료 알림 처리
     */
    private void handleCrawlingComplete(Map<String, String> data) {
        String siteName = data.getOrDefault("site_name", "사이트");
        String title = siteName + " 크롤링 완료";
        String body = "새로운 정보가 업데이트되었습니다.";

        showNotification(CHANNEL_CRAWLING, title, body, data);
    }

    /**
     * 새 게시글 알림 처리
     */
    private void handleNewPost(Map<String, String> data) {
        String siteName = data.getOrDefault("site_name", "사이트");
        String postTitle = data.getOrDefault("post_title", "새 게시글");

        String title = siteName + " 새 게시글";
        String body = postTitle;

        // is_new 플래그는 앱에서 /posts/list API 응답으로 처리
        showNotification(CHANNEL_NEW_POST, title, body, data);
    }

    /**
     * 일정 알림 처리
     */
    private void handleScheduleReminder(Map<String, String> data) {
        String eventTitle = data.getOrDefault("event_title", "일정");
        String eventTime = data.getOrDefault("event_time", "");

        String title = "일정 알림";
        String body = eventTitle;
        if (!eventTime.isEmpty()) {
            body += " (" + eventTime + ")";
        }

        showNotification(CHANNEL_SCHEDULE, title, body, data);
    }

    /**
     * 알림 표시
     */
    private void showNotification(String channelId, String title, String body, Map<String, String> data) {
        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        // MainActivity로 이동하는 Intent
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // 데이터 전달 (필요시)
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘 사용
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // 클릭 시 자동 제거
                .setContentIntent(pendingIntent);

        // 알림 ID는 타임스탬프 사용 (각 알림마다 고유)
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());

        Log.d(TAG, "알림 표시 완료: " + title);
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상 필수)
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }

            // 크롤링 완료 채널
            NotificationChannel crawlingChannel = new NotificationChannel(
                CHANNEL_CRAWLING,
                "크롤링 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            crawlingChannel.setDescription("사이트 크롤링 완료 알림");
            manager.createNotificationChannel(crawlingChannel);

            // 새 게시글 채널
            NotificationChannel newPostChannel = new NotificationChannel(
                CHANNEL_NEW_POST,
                "새 게시글 알림",
                NotificationManager.IMPORTANCE_HIGH
            );
            newPostChannel.setDescription("새로운 게시글 알림");
            manager.createNotificationChannel(newPostChannel);

            // 일정 알림 채널
            NotificationChannel scheduleChannel = new NotificationChannel(
                CHANNEL_SCHEDULE,
                "일정 알림",
                NotificationManager.IMPORTANCE_HIGH
            );
            scheduleChannel.setDescription("캘린더 일정 알림");
            manager.createNotificationChannel(scheduleChannel);

            Log.d(TAG, "알림 채널 생성 완료");
        }
    }
}
