package com.example.mobile_android.fcm;

public enum NotificationType {
    CALENDAR_REMINDER("calendar_reminder"),        // 일정 알림 (파란색)
    CRAWL_NEW_POSTS("crawl_new_posts"),           // 크롤링 알림 (status로 세분화)
    UNKNOWN("unknown");                            // 알 수 없는 상태 (회색)

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationType fromString(String type) {
        if (type == null) {
            return UNKNOWN;
        }
        for (NotificationType t : values()) {
            if (t.value.equals(type)) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
