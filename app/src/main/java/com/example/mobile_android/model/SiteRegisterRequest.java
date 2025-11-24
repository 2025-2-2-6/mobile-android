package com.example.mobile_android.model;

public class SiteRegisterRequest {

    private String url;
    private String name;
    private String userId;

    public SiteRegisterRequest(String url, String name, String userId) {
        this.url = url;
        this.name = name;
        this.userId = userId;
    }

    public String getUrl() { return url; }
    public String getName() { return name; }
    public String getUserId() { return userId; }
}
