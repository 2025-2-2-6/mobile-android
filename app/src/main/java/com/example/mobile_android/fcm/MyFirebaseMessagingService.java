package com.example.mobile_android.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mobile_android.R;
import com.example.mobile_android.config.AppConfig;
import com.example.mobile_android.data.NotificationRepository;
import com.example.mobile_android.fcm.handler.NotificationHandler;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = AppConfig.FCM_NOTIFICATION_CHANNEL_ID;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            NotificationRepository.getInstance(getApplicationContext())
                    .cacheFcmPayload(remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // notification.title이 null이면 data에서 생성
            if (title == null || title.isEmpty()) {
                NotificationHandler handler = NotificationHandlerFactory.getHandler(remoteMessage.getData().get("type"));
                title = handler.getTitle(remoteMessage.getData());
            }

            showNotification(title, body, remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        FcmTokenManager.handleNewToken(this, token);
    }

    private void handleDataMessage(Map<String, String> data) {
        String typeString = data.get("type");
        NotificationHandler handler = NotificationHandlerFactory.getHandler(typeString);

        handler.handle(this, data);
        showNotification(handler.getTitle(data), handler.getMessage(data), data);
    }

    private void showNotification(String title, String message, Map<String, String> data) {
        createNotificationChannel();

        NotificationHandler handler = NotificationHandlerFactory.getHandler(data.get("type"));
        Intent intent = handler.getIntent(this, data);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = AppConfig.FCM_NOTIFICATION_CHANNEL_NAME;
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
}
