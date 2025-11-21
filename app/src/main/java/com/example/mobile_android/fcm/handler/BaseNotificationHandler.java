package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;

import com.example.mobile_android.MainActivity;

import java.util.Map;

public abstract class BaseNotificationHandler implements NotificationHandler {

    @Override
    public void handle(Context context, Map<String, String> data) {
        // 기본 처리 로직 (필요시 오버라이드)
    }

    @Override
    public Intent getIntent(Context context, Map<String, String> data) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public String getTitle(Map<String, String> data) {
        return data.getOrDefault("title", "새 알림");
    }

    @Override
    public String getMessage(Map<String, String> data) {
        return data.getOrDefault("message", "");
    }
}
