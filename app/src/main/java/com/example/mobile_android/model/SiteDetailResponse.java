// app/src/main/java/com/example/mobile_android/model/SiteDetailResponse.java
package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class SiteDetailResponse {

    // 서버 JSON: { "id": "uuid", "name": "...", "description": "...", "category": "...", "url": "..." }

    @SerializedName("id")      // JSON의 "id" -> 자바의 site_id 에 매핑
    public String site_id;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;

    @SerializedName("category")
    public String category;

    @SerializedName("url")
    public String url;
}
