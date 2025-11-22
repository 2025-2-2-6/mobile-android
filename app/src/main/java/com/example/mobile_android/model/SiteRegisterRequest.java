package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class SiteRegisterRequest {
    @SerializedName("url")
    private String url;
    @SerializedName("name")
    private String name;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("category")
    private String category;

    public SiteRegisterRequest(String url, String name, String userId) {
        this.url = url;
        this.name = name;
        this.userId = userId;
    }

    public SiteRegisterRequest(String url, String name, String userId, String category) {
        this.url = url;
        this.name = name;
        this.userId = userId;
        this.category = category;
    }
}