package com.example.mobile_android.fcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.mobile_android.config.AppConfig;
import com.example.mobile_android.model.FcmTokenRequest;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles caching and syncing FCM tokens with the backend.
 */
public final class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";
    private static final String KEY_FCM_TOKEN = "cached_fcm_token";

    private FcmTokenManager() {
    }

    /**
     * Cache token locally and try to register it if the user is already logged in.
     */
    public static void handleNewToken(Context context, String token) {
        if (context == null || TextUtils.isEmpty(token)) {
            return;
        }
        cacheToken(context.getApplicationContext(), token);
        registerTokenIfPossible(context.getApplicationContext());
    }

    /**
     * Attempt to register the cached token if both user & token exist.
     */
    public static void registerTokenIfPossible(Context context) {
        Context appContext = context.getApplicationContext();
        String cachedToken = getCachedToken(appContext);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.d(TAG, "Firebase user is null. Skip registering token.");
            return;
        }
        if (TextUtils.isEmpty(cachedToken)) {
            Log.d(TAG, "No cached FCM token. Nothing to register.");
            return;
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE;
        FcmTokenRequest request = new FcmTokenRequest(user.getUid(), cachedToken, "android", deviceInfo);

        apiService.registerFcmToken(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM token registered successfully.");
                } else {
                    Log.w(TAG, "Failed to register FCM token. code=" + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error registering FCM token", t);
            }
        });
    }

    private static void cacheToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(AppConfig.PREF_FCM, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    }

    @Nullable
    private static String getCachedToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppConfig.PREF_FCM, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }
}
