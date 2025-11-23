package com.example.mobile_android.network;

import com.example.mobile_android.model.UserResponse;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {
    // Google ID Token을 직접 전달 (Firebase ID Token이 아님!)
    // ApiClient.getClientWithoutAuth()와 함께 사용
    @POST("/api/v1/auth/google-login")
    Call<UserResponse> googleLogin(@Header("Authorization") String googleIdToken);
}
