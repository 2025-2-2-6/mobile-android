package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {
    @SerializedName("fcm_token")
    private final String fcmToken;

    @SerializedName("platform")
    private final String platform;

    @SerializedName("device_info")
    private final String deviceInfo;

    public FcmTokenRequest(String fcmToken, String platform, String deviceInfo) {
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.deviceInfo = deviceInfo;
    }
}
