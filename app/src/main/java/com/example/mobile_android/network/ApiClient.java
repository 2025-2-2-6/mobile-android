package com.example.mobile_android.network;

import com.example.mobile_android.config.AppConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    /**
     * Retrofit 인스턴스 반환
     * @deprecated Use {@link #getClient()} and call {@code create(ApiService.class)} 직접 사용하세요.
     */
    @Deprecated
    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    /**
     * Retrofit 인스턴스 생성 및 반환 (싱글톤)
     * Google ID Token을 각 API에서 직접 전달
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // 로깅 인터셉터 설정 (디버그 모드에서만 활성화)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            if (AppConfig.enableLogging()) {
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            } else {
                logging.setLevel(HttpLoggingInterceptor.Level.NONE);
            }

            // OkHttpClient 설정 (AuthInterceptor 제거 - Google ID Token 직접 전달)
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(AppConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(AppConfig.READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(AppConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .build();

            // Retrofit 인스턴스 생성
            retrofit = new Retrofit.Builder()
                    .baseUrl(AppConfig.getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    /**
     * Retrofit 인스턴스 초기화 (Base URL 변경 시 사용)
     */
    public static void resetClient() {
        retrofit = null;
    }
}
