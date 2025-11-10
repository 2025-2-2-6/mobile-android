package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

// 전체 API 응답 구조를 위한 데이터 클래스
public class ApiResponse {

    @SerializedName("site_id")
    private String siteId;

    @SerializedName("message")
    private String message;

    @SerializedName("post")
    private Post post;

    // Getter 메서드들
    public String getSiteId() {
        return siteId;
    }

    public String getMessage() {
        return message;
    }

    public Post getPost() {
        return post;
    }
}
