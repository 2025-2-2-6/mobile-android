package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 사용자 활동 통계 모델
 */
public class UserStatistics {

    @SerializedName("registered_sites_count")
    private int registeredSitesCount;

    @SerializedName("new_posts_count")
    private int newPostsCount;

    @SerializedName("saved_events_count")
    private int savedEventsCount;

    @SerializedName("total_notifications_count")
    private int totalNotificationsCount;

    @SerializedName("unread_notifications_count")
    private int unreadNotificationsCount;

    // Getters and Setters

    public int getRegisteredSitesCount() {
        return registeredSitesCount;
    }

    public void setRegisteredSitesCount(int registeredSitesCount) {
        this.registeredSitesCount = registeredSitesCount;
    }

    public int getNewPostsCount() {
        return newPostsCount;
    }

    public void setNewPostsCount(int newPostsCount) {
        this.newPostsCount = newPostsCount;
    }

    public int getSavedEventsCount() {
        return savedEventsCount;
    }

    public void setSavedEventsCount(int savedEventsCount) {
        this.savedEventsCount = savedEventsCount;
    }

    public int getTotalNotificationsCount() {
        return totalNotificationsCount;
    }

    public void setTotalNotificationsCount(int totalNotificationsCount) {
        this.totalNotificationsCount = totalNotificationsCount;
    }

    public int getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(int unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }
}
