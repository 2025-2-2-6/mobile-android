package com.example.mobile_android.fcm;

public enum CrawlStatus {
    SUCCESS("success"),
    FAILED("failed"),
    NEW_POST("new_post"),
    UNKNOWN("unknown");

    private final String value;

    CrawlStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CrawlStatus fromString(String status) {
        if (status == null) {
            return UNKNOWN;
        }
        for (CrawlStatus s : values()) {
            if (s.value.equals(status)) {
                return s;
            }
        }
        return UNKNOWN;
    }
}
