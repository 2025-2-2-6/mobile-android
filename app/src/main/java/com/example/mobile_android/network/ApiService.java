package com.example.mobile_android.network;

import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;
import com.example.mobile_android.model.SiteDetailResponse;
import com.example.mobile_android.model.UpdateSiteRequest;
import com.example.mobile_android.model.SiteSummary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface ApiService {

    // 사이트 등록 (기존 그대로)
    @Headers("Content-Type: application/json")
    @POST("/api/v1/sites/register")
    Call<SiteRegisterResponse> registerSite(@Body SiteRegisterRequest request);

    // 게시글 리스트 (기존 그대로)
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

    @GET("/api/v1/sites/")
    Call<List<SiteSummary>> getSites(
            @Query("only_public") Boolean onlyPublic
    );

    @GET("/api/v1/sites/{site_id}")
    Call<SiteDetailResponse> getSiteDetail(
            @Path("site_id") String siteId   // snake_case로 맞춰주기
    );

    @Headers("Content-Type: application/json")
    @PATCH("/api/v1/sites/{site_id}")
    Call<SiteDetailResponse> updateSite(
            @Path("site_id") String siteId,
            @Body UpdateSiteRequest body
    );

}
