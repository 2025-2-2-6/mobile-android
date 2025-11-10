package com.example.mobile_android;

// 새로운 API 응답 구조에 맞춘 예시 데이터 클래스
public class Post {
    private String id;
    private String title;
    private String content;
    private String eventDate;
    private String location;
    private String sourceUrl;

    public Post(String id, String title, String content, String eventDate, String location, String sourceUrl) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.eventDate = eventDate;
        this.location = location;
        this.sourceUrl = sourceUrl;
    }

    // Getter 메서드들
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getLocation() {
        return location;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}
