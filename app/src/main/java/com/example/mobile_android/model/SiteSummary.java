// app/src/main/java/com/example/mobile_android/model/SiteSummary.java
package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class SiteSummary {

    // 이건 user_sites 테이블 id 같은 거면, 서버에서 실제로 내려주지 않으면 그냥 null 이어도 됨
    public String user_site_id;     // user_sites.id (옵션)

    @SerializedName("id")          // 서버의 "id" -> 여기에 들어오게
    public String site_id;         // sites.id

    @SerializedName("name")
    public String name;            // 표시 이름

    @SerializedName("description")
    public String description;     // 한 줄 설명

    @SerializedName("category")
    public String category;        // "학교", "장학금" 등

    // 이 두 개는 서버에서 안 주면, 클라에서 따로 세팅해서 쓰는 비즈니스 필드라고 보면 됨
    public String subscriber_text;  // "1,250명 구독 중" 같은 문자열
    public boolean subscribed;      // 구독 여부
}
