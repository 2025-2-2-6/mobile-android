package com.example.mobile_android.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.data.NotificationRepository;
import com.example.mobile_android.R;
import com.example.mobile_android.ui.post.PostDetailActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = com.example.mobile_android.config.AppConfig.FCM_NOTIFICATION_CHANNEL_ID;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // 데이터 페이로드 확인
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            NotificationRepository.getInstance(getApplicationContext())
                    .cacheFcmPayload(remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // 알림 페이로드 확인
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            showNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Refresh token is immediately synced to the backend
        sendTokenToServer(token);
    }

    /**
     * 데이터 메시지 처리
     */
    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        String title = data.getOrDefault("title", "새 알림");
        String message = data.getOrDefault("message", "");

        if (type != null) {
            switch (type) {
                case "new_post":
                    String postId = data.get("post_id");
                    String postTitle = data.get("post_title");
                    showNotification(title, message, data);
                    break;

                case "site_registered":
                    String siteId = data.get("site_id");
                    String siteName = data.get("site_name");
                    showNotification(title, message, data);
                    break;

                case "event_reminder":
                case "deadline":
                case "crawling_complete":
                case "schedule_reminder":
                    showNotification(title, message, data);
                    break;

                default:
                    showNotification(title, message, data);
                    break;
            }
        }
    }

    /**
     * 알림 표시
     */
    private void showNotification(String title, String message, Map<String, String> data) {
        createNotificationChannel();

        // 알림 클릭 시 이동할 Intent 생성
        Intent intent = getIntentForNotification(data);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_new)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * 알림 타입에 따라 적절한 Intent 반환
     */
    private Intent getIntentForNotification(Map<String, String> data) {
        String type = data.get("type");
        String postId = data.get("post_id");
        String siteId = data.get("site_id");

        if ("new_post".equals(type) && postId != null) {
            // 게시물 상세 페이지로 이동
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("POST_ID", postId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        } else if ("site_registered".equals(type) && siteId != null) {
            // 메인 액티비티로 이동 (사이트 상세 페이지 구현 시 변경)
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("SITE_ID", siteId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        } else {
            // 기본: 메인 액티비티로 이동
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        }
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = com.example.mobile_android.config.AppConfig.FCM_NOTIFICATION_CHANNEL_NAME;
            String description = "앱 알림 채널";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * FCM 토큰을 서버로 전송
     */
    private void sendTokenToServer(String token) {
        FcmTokenManager.handleNewToken(this, token);
    }
}

