package com.example.mobile_android.model;

public class UpdateSiteRequest {
    public String name;
    public String description;   // 지금은 null 보내도 상관 X

    public UpdateSiteRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
