package com.example.mobile_android.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.R;

/**
 * 캘린더 일정 알림을 받아서 Notification을 표시하는 BroadcastReceiver
 */
public class CalendarEventAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "CalendarEventAlarmReceiver";
    private static final String CHANNEL_ID = "calendar_event_channel";
    private static final String CHANNEL_NAME = "캘린더 일정 알림";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");

        int eventId = intent.getIntExtra(CalendarEventAlarmManager.EXTRA_EVENT_ID, -1);
        String title = intent.getStringExtra(CalendarEventAlarmManager.EXTRA_EVENT_TITLE);
        String category = intent.getStringExtra(CalendarEventAlarmManager.EXTRA_EVENT_CATEGORY);
        String memo = intent.getStringExtra(CalendarEventAlarmManager.EXTRA_EVENT_MEMO);

        if (title == null || title.isEmpty()) {
            Log.e(TAG, "Event title is null or empty");
            return;
        }

        showNotification(context, eventId, title, category, memo);
    }

    /**
     * 알림을 표시합니다.
     */
    private void showNotification(Context context, int eventId, String title, String category, String memo) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        // Android 8.0+ (API 26+)에서는 Notification Channel 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("캘린더에 저장된 일정에 대한 알림입니다");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // 알림 클릭 시 앱 실행 (캘린더 탭으로 이동)
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("OPEN_TAB", "calendar"); // MainActivity에서 처리
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                eventId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알림 내용 구성
        String contentText = buildContentText(category, memo);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_calendar)
                .setContentTitle("\uD83D\uDCC5 " + title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500});

        notificationManager.notify(eventId, builder.build());

        Log.d(TAG, "Notification shown for event: " + title);
    }

    /**
     * 알림 내용 텍스트 구성
     */
    private String buildContentText(String category, String memo) {
        StringBuilder sb = new StringBuilder();

        if (category != null && !category.isEmpty()) {
            sb.append("[").append(category).append("] ");
        }

        if (memo != null && !memo.isEmpty()) {
            sb.append(memo);
        } else {
            sb.append("일정 시간이 되었습니다");
        }

        return sb.toString();
    }
}
