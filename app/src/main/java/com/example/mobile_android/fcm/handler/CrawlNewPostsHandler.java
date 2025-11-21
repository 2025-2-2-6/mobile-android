package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.fcm.CrawlStatus;

import java.util.Map;

public class CrawlNewPostsHandler extends BaseNotificationHandler {

    @Override
    public Intent getIntent(Context context, Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));
        String siteId = data.get("site_id");

        Intent intent = new Intent(context, MainActivity.class);

        // new_post 또는 success일 때 사이트 상세로 이동 가능
        if ((status == CrawlStatus.NEW_POST || status == CrawlStatus.SUCCESS)
                && siteId != null && !siteId.isEmpty()) {
            intent.putExtra("NAVIGATE_TO", "site");
            intent.putExtra("SITE_ID", siteId);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public String getTitle(Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));
        String siteName = data.get("site_name");

        switch (status) {
            case SUCCESS:
                return "사이트 등록 완료";
            case FAILED:
                return "크롤링 실패";
            case NEW_POST:
                return siteName != null ? siteName : "새 게시물";
            default:
                return "알림";
        }
    }

    @Override
    public String getMessage(Map<String, String> data) {
        // 백엔드에서 자동 생성된 message 사용
        return data.getOrDefault("message", "");
    }
}
