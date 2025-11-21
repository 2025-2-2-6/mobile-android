package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;

import com.example.mobile_android.MainActivity;

import java.util.Map;

public class CalendarReminderHandler extends BaseNotificationHandler {

    @Override
    public Intent getIntent(Context context, Map<String, String> data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("NAVIGATE_TO", "calendar");
        String eventId = data.get("event_id");
        if (eventId != null) {
            intent.putExtra("EVENT_ID", eventId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public String getTitle(Map<String, String> data) {
        return data.getOrDefault("title", "일정 알림");
    }

    @Override
    public String getMessage(Map<String, String> data) {
        return data.getOrDefault("message", "");
    }
}
