package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;

import java.util.Map;

public interface NotificationHandler {
    void handle(Context context, Map<String, String> data);
    Intent getIntent(Context context, Map<String, String> data);
    String getTitle(Map<String, String> data);
    String getMessage(Map<String, String> data);
}
