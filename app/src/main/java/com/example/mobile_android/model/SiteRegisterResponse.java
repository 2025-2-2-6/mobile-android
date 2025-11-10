package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SiteRegisterResponse {
    @SerializedName("site_id")
    private String siteId;
    @SerializedName("posts")
    private List<Post> posts;
    @SerializedName("message")
    private String message;

    // Getters
    public String getSiteId() { return siteId; }
    public List<Post> getPosts() { return posts; }
    public String getMessage() { return message; }
}