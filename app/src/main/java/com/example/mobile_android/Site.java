package com.example.mobile_android;

import java.sql.Timestamp;

public class Site {
    private String id;
    private String userId;
    private String url;
    private String name;
    private String description;
    private String category;
    private boolean isPublic;
    private Timestamp crawlCycleTime;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int newPosts;

    // Constructor, getters, and setters

    public Site(String name, String category, String url, String lastUpdated, int newPosts) {
        this.name = name;
        this.category = category;
        this.url = url;
        this.updatedAt = Timestamp.valueOf(lastUpdated);
        this.newPosts = newPosts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Timestamp getCrawlCycleTime() {
        return crawlCycleTime;
    }

    public void setCrawlCycleTime(Timestamp crawlCycleTime) {
        this.crawlCycleTime = crawlCycleTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getNewPosts() {
        return newPosts;
    }

    public void setNewPosts(int newPosts) {
        this.newPosts = newPosts;
    }
}
