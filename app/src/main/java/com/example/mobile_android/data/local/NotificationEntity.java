package com.example.mobile_android.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Local cache for notifications. Allows offline access and badge counts across restarts.
 */
@Entity(tableName = "notifications")
public class NotificationEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    private String type;
    private String title;
    private String message;

    @ColumnInfo(name = "post_id")
    private String postId;

    @ColumnInfo(name = "site_id")
    private String siteId;

    @ColumnInfo(name = "is_read")
    private boolean isRead;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "event_start_date")
    private String eventStartDate;

    @ColumnInfo(name = "event_end_date")
    private String eventEndDate;

    @ColumnInfo(name = "received_at")
    private long receivedAt;

    @ColumnInfo(name = "crawl_status")
    private String crawlStatus; // success, failed, new_post, unknown

    @Ignore
    public NotificationEntity(@NonNull String id) {
        this.id = id;
    }

    @Ignore
    public NotificationEntity(@NonNull String id, String type, String title, String message, long receivedAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.receivedAt = receivedAt;
    }

    // Room에서 사용할 기본 생성자
    public NotificationEntity() {
        this.id = "";
    }

    public static NotificationEntity from(
            @NonNull String id,
            String userId,
            String type,
            String title,
            String message,
            String postId,
            String siteId,
            boolean isRead,
            String createdAt,
            String eventStartDate,
            String eventEndDate,
            long receivedAt
    ) {
        NotificationEntity entity = new NotificationEntity(id);
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(title);
        entity.setMessage(message);
        entity.setPostId(postId);
        entity.setSiteId(siteId);
        entity.setRead(isRead);
        entity.setCreatedAt(createdAt);
        entity.setEventStartDate(eventStartDate);
        entity.setEventEndDate(eventEndDate);
        entity.setReceivedAt(receivedAt);
        return entity;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getEventStartDate() {
        return eventStartDate;
    }

    public void setEventStartDate(String eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public String getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(String eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getCrawlStatus() {
        return crawlStatus;
    }

    public void setCrawlStatus(String crawlStatus) {
        this.crawlStatus = crawlStatus;
    }
}
