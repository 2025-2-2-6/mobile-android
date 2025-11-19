package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Notification implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("type")
    private String type; // "new_post", "site_registered", "event_reminder", "deadline"

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("post_id")
    private String postId;

    @SerializedName("site_id")
    private String siteId;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("event_start_date")
    private String eventStartDate;

    @SerializedName("event_end_date")
    private String eventEndDate;

    // Constructor
    public Notification(String id, String userId, String type, String title, String message,
                       String postId, String siteId, boolean isRead, String createdAt,
                       String eventStartDate, String eventEndDate) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.postId = postId;
        this.siteId = siteId;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getPostId() { return postId; }
    public String getSiteId() { return siteId; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
    public String getEventStartDate() { return eventStartDate; }
    public String getEventEndDate() { return eventEndDate; }

    // Setters
    public void setRead(boolean read) { isRead = read; }

    // Notification Type Constants
    public static class Type {
        public static final String NEW_POST = "new_post";
        public static final String SITE_REGISTERED = "site_registered";
        public static final String EVENT_REMINDER = "event_reminder";
        public static final String DEADLINE = "deadline";
        public static final String CRAWLING_COMPLETE = "crawling_complete";
        public static final String SCHEDULE_REMINDER = "schedule_reminder";
    }
}
