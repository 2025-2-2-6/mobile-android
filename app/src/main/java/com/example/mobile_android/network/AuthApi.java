package com.example.mobile_android.network;

import com.example.mobile_android.model.UserResponse;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("/api/v1/auth/google-login")
    Call<UserResponse> googleLogin(@Header("Authorization") String token);
}
