package com.example.mobile_android.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    // TODO: 에뮬레이터는 10.0.2.2:8000, 실제 기기는 서버 IP 사용
    public static final String BASE_URL = "http://10.0.2.2:8000"; 

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(120, TimeUnit.SECONDS) // 연결 타임아웃 120초
                    .readTimeout(120, TimeUnit.SECONDS)    // 읽기 타임아웃 120초
                    .writeTimeout(120, TimeUnit.SECONDS)   // 쓰기 타임아웃 120초
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}