package com.example.mobile_android.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mobile_android.util.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Google Client ID Token을 자동으로 Authorization 헤더에 추가하는 인터셉터
 * TokenManager에서 저장된 Bearer Token을 사용
 */
public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // TokenManager에서 저장된 Bearer Token 가져오기
        String bearerToken = TokenManager.getBearerToken(context);

        if (bearerToken == null || bearerToken.isEmpty()) {
            Log.w(TAG, "No bearer token found, proceeding without token");
            return chain.proceed(originalRequest);
        }

        // Authorization 헤더 추가 (이미 "Bearer " 포함되어 있음)
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", bearerToken)
                .build();

        Log.d(TAG, "Added Authorization header to request: " + originalRequest.url());
        return chain.proceed(authenticatedRequest);
    }
}
