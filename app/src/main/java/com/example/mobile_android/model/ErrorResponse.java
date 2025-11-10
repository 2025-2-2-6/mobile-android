package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("detail")
    private String detail;

    public String getDetail() {
        return detail;
    }
}