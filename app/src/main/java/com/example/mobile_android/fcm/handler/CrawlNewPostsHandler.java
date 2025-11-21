package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.fcm.CrawlStatus;
import com.example.mobile_android.ui.post.PostDetailActivity;

import java.util.Map;

public class CrawlNewPostsHandler extends BaseNotificationHandler {

    @Override
    public Intent getIntent(Context context, Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));

        switch (status) {
            case NEW_POST:
                String postId = data.get("post_id");
                if (postId != null) {
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("POST_ID", postId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    return intent;
                }
                break;

            case SUCCESS:
            case FAILED:
            default:
                break;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public String getTitle(Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));

        switch (status) {
            case SUCCESS:
                return "사이트 등록 완료";
            case FAILED:
                return "사이트 등록 실패";
            case NEW_POST:
                return data.getOrDefault("title", "새 게시물");
            default:
                return "알림";
        }
    }

    @Override
    public String getMessage(Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));

        switch (status) {
            case SUCCESS:
                String siteName = data.get("site_name");
                return siteName != null ? siteName + " 사이트가 등록되었습니다." : "사이트가 등록되었습니다.";
            case FAILED:
                return data.getOrDefault("message", "사이트 등록 또는 크롤링에 실패했습니다.");
            case NEW_POST:
                return data.getOrDefault("message", "");
            default:
                return data.getOrDefault("message", "");
        }
    }
}
