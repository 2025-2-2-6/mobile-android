package com.example.mobile_android.fcm;

public enum NotificationType {
    CALENDAR_REMINDER("calendar_reminder"),
    CRAWL_NEW_POSTS("crawl_new_posts"),
    UNKNOWN("unknown");

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
