package com.example.mobile_android.ui.post;

// 새로운 API 응답 구조에 맞춘 예시 데이터 클래스
// Note: This is a simplified version. Use com.example.mobile_android.model.Post for full functionality.
public class Post {
    private String id;
    private String siteId;
    private String title;
    private String content;
    private String sourceUrl;
    private String eventDate;
    private String eventStartDate;
    private String eventEndDate;
    private String location;
    private String category;
    private String createdAt;
    private String updatedAt;

    public Post(String id, String siteId, String title, String content, String sourceUrl,
                String eventDate, String eventStartDate, String eventEndDate,
                String location, String category, String createdAt, String updatedAt) {
        this.id = id;
        this.siteId = siteId;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.eventDate = eventDate;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
        this.location = location;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter 메서드들
    public String getId() {
        return id;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getEventStartDate() {
        return eventStartDate;
    }

    public String getEventEndDate() {
        return eventEndDate;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
