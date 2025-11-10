package com.example.mobile_android.retrofit;

import com.example.mobile_android.model.ApiResponse;
import com.example.mobile_android.model.Site;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    // 기존 "/sites" 요청 (사이트 목록)
    @GET("sites")
    Call<List<Site>> getAllSites();

    // 새로운 API 명세에 맞춘 요청 (최신 게시글 가져오기)
    // 예: POST /posts/latest  (Body에 URL을 담아 전송)
    @POST("posts/latest")
    Call<ApiResponse> getLatestPost(@Body SiteUrlRequest request);

    // 위 요청에 사용할 요청 본문(Body) 데이터 클래스
    class SiteUrlRequest {
        private String url;

        public SiteUrlRequest(String url) {
            this.url = url;
        }
    }

    // 테스트용 "/docs" 요청
    @GET("docs")
    Call<ResponseBody> checkDocs();
}
