package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

// JSON의 중첩된 'post' 객체를 위한 데이터 클래스
public class Post {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("event_date")
    private String eventDate;

    @SerializedName("location")
    private String location;

    @SerializedName("source_url")
    private String sourceUrl;

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
}
