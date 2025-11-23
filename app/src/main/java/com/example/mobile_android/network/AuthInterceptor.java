package com.example.mobile_android.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Firebase Auth 토큰을 자동으로 Authorization 헤더에 추가하는 인터셉터
 */
public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No authenticated user, proceeding without token");
            return chain.proceed(originalRequest);
        }

        try {
            // Firebase ID 토큰을 동기적으로 가져오기
            String idToken = com.google.android.gms.tasks.Tasks.await(
                currentUser.getIdToken(false)
            ).getToken();

            if (idToken == null || idToken.isEmpty()) {
                Log.w(TAG, "Failed to get ID token, proceeding without token");
                return chain.proceed(originalRequest);
            }

            // Authorization 헤더 추가
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + idToken)
                    .build();

            Log.d(TAG, "Added Authorization header to request: " + originalRequest.url());
            return chain.proceed(authenticatedRequest);

        } catch (Exception e) {
            Log.e(TAG, "Error getting Firebase ID token", e);
            return chain.proceed(originalRequest);
        }
    }
}
