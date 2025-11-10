package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class Site {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("category")
    private String category;

    @SerializedName("description")
    private String description;

    @SerializedName("created_at")
    private String createdAt;

    // 필요한 모든 필드에 대해 getter를 추가할 수 있습니다.
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCategory() {
        return category;
    }
}
