package com.example.mobile_android.fcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.mobile_android.network.ApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FCM 토큰 관리 클래스
 * - 토큰 생성, 저장, 서버 전송 담당
 */
public class FcmTokenManager {
    private static final String TAG = "FcmTokenManager";
    private static final String PREFS_NAME = "fcm_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * FCM 토큰을 가져오고 서버로 전송
     */
    public static void initializeFcmToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "FCM 토큰 가져오기 실패", task.getException());
                    return;
                }

                // 토큰 획득
                String token = task.getResult();
                Log.d(TAG, "FCM 토큰: " + token);

                // SharedPreferences에 저장
                saveTokenLocally(context, token);

                // 서버로 전송
                sendTokenToServer(context, token);
            });
    }

    /**
     * FCM 토큰을 로컬에 저장
     */
    private static void saveTokenLocally(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        Log.d(TAG, "FCM 토큰 로컬 저장 완료");
    }

    /**
     * 로컬에 저장된 FCM 토큰 가져오기
     */
    public static String getTokenLocally(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * FCM 토큰을 서버로 전송
     */
    public static void sendTokenToServer(Context context, String token) {
        // Firebase 인증된 사용자 확인
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "사용자가 로그인되지 않음. 토큰 전송 보류");
            return;
        }

        String userId = user.getUid();

        // 백그라운드에서 서버로 전송
        executor.execute(() -> {
            try {
                // API 엔드포인트 (서버 URL은 실제 환경에 맞게 수정 필요)
                String url = ApiClient.BASE_URL + "/api/v1/fcm/register";

                // JSON 데이터 생성
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("user_id", userId);
                jsonBody.put("fcm_token", token);
                jsonBody.put("platform", "android");
                jsonBody.put("device_info", Build.MODEL); // 디바이스 모델명

                RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
                );

                // HTTP 요청 생성
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                // 요청 실행
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "FCM 토큰 서버 전송 성공");
                } else {
                    Log.e(TAG, "FCM 토큰 서버 전송 실패: " + response.code());
                    Log.e(TAG, "응답 본문: " + response.body().string());
                }

                response.close();

            } catch (Exception e) {
                Log.e(TAG, "FCM 토큰 서버 전송 중 오류", e);
            }
        });
    }

    /**
     * FCM 토큰 삭제 (로그아웃 시 호출)
     */
    public static void deleteToken(Context context) {
        FirebaseMessaging.getInstance().deleteToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "FCM 토큰 삭제 완료");
                    // 로컬 저장소에서도 제거
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().remove(KEY_FCM_TOKEN).apply();
                } else {
                    Log.w(TAG, "FCM 토큰 삭제 실패", task.getException());
                }
            });
    }
}
