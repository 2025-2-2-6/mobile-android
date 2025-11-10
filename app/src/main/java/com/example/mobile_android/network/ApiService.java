package com.example.mobile_android.network;

import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("/api/v1/sites/register")
    Call<SiteRegisterResponse> registerSite(@Body SiteRegisterRequest request);

    @GET("/api/v1/posts/list")
    Call<PostListResponse> getPosts(
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("q") String query,
            @Query("site_id") String siteId,
            @Query("since") String since,
            @Query("until") String until,
            @Query("order_by") String orderBy,
            @Query("order") String order
    );
}