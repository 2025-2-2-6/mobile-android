package com.example.mobile_android.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "calendar_events")
public class CalendarEvent {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    @SerializedName("id")
    private String id;  // UUID from backend

    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    private String userId;

    @ColumnInfo(name = "post_id")
    @SerializedName("post_id")
    private String postId;

    @ColumnInfo(name = "title")
    @SerializedName("title")
    private String title;

    @ColumnInfo(name = "category")
    @SerializedName("category")
    private String category;

    @ColumnInfo(name = "description")
    @SerializedName("description")
    private String description;

    @ColumnInfo(name = "start_time")
    @SerializedName("start_time")
    private String startTime;  // ISO 8601 format from backend

    @ColumnInfo(name = "end_time")
    @SerializedName("end_time")
    private String endTime;  // ISO 8601 format from backend

    @ColumnInfo(name = "notify_enabled")
    @SerializedName("notify_enabled")
    private boolean notifyEnabled;

    @ColumnInfo(name = "notify_time")
    @SerializedName("notify_time")
    private String notifyTime;  // ISO 8601 format from backend

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    private String updatedAt;

    // Getters and Setters
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

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isNotifyEnabled() {
        return notifyEnabled;
    }

    public void setNotifyEnabled(boolean notifyEnabled) {
        this.notifyEnabled = notifyEnabled;
    }

    public String getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(String notifyTime) {
        this.notifyTime = notifyTime;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods for UI compatibility with old schema
    public String getEventDate() {
        if (startTime == null) return null;
        // Extract YYYY-MM-DD from ISO 8601
        return startTime.substring(0, 10);
    }

    public String getEventTime() {
        if (startTime == null) return null;
        // Extract HH:mm from ISO 8601 (assuming format: YYYY-MM-DDTHH:mm:ss)
        try {
            int timeStart = startTime.indexOf('T') + 1;
            return startTime.substring(timeStart, timeStart + 5);
        } catch (Exception e) {
            return null;
        }
    }

    public String getMemo() {
        return description;
    }

    public boolean isAlarmEnabled() {
        return notifyEnabled;
    }

    public String getAlarmTime() {
        return notifyTime;
    }
}
