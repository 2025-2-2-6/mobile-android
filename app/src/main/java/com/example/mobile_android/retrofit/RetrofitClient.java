package com.example.mobile_android.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // FastAPI 서버의 기본 주소입니다.
    // 안드로이드 에뮬레이터에서 localhost에 접속하려면 10.0.2.2를 사용해야 합니다.
    private static final String BASE_URL = "http://10.0.2.2:8000/";

    private static Retrofit retrofit = null;

    // Retrofit 인스턴스를 반환하는 static 메서드 (Singleton 패턴)
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // 서버 기본 주소 설정
                    .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기 설정
                    .build();
        }
        return retrofit;
    }
}
