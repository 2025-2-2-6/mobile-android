package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {
    @SerializedName("user_id")
    private final String userId;

    @SerializedName("fcm_token")
    private final String fcmToken;

    @SerializedName("platform")
    private final String platform;

    @SerializedName("device_info")
    private final String deviceInfo;

    public FcmTokenRequest(String userId, String fcmToken, String platform, String deviceInfo) {
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.deviceInfo = deviceInfo;
    }
}
