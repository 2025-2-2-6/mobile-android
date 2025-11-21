package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.ui.post.PostDetailActivity;

import java.util.Map;

public class CalendarReminderHandler extends BaseNotificationHandler {

    @Override
    public Intent getIntent(Context context, Map<String, String> data) {
        String postId = data.get("post_id");

        // post_id가 있으면 게시물 상세로 이동
        if (postId != null && !postId.isEmpty()) {
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("POST_ID", postId);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return intent;
        }

        // 없으면 캘린더로 이동
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("NAVIGATE_TO", "calendar");
        String eventId = data.get("event_id");
        if (eventId != null && !eventId.isEmpty()) {
            intent.putExtra("EVENT_ID", eventId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public String getTitle(Map<String, String> data) {
        return "일정 알림";
    }

    @Override
    public String getMessage(Map<String, String> data) {
        // event_title 또는 message 사용
        String eventTitle = data.get("event_title");
        if (eventTitle != null && !eventTitle.isEmpty()) {
            return eventTitle;
        }
        return data.getOrDefault("message", "");
    }
}
