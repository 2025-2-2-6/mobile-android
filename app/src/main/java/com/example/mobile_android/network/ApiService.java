package com.example.mobile_android.network;

import com.example.mobile_android.model.FcmTokenRequest;
import com.example.mobile_android.model.Notification;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.Site;

import java.util.List;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("/api/v1/sites/register")
    Call<SiteRegisterResponse> registerSite(@Body SiteRegisterRequest request);

    // 📌 추가: 등록한 사이트 전체 불러오기
    @GET("/api/v1/sites")
    Call<List<Site>> getSites();
    @DELETE("/api/v1/sites/{siteId}")
    Call<Void> deleteSite(@Path("siteId") String siteId);

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

    @GET("/api/v1/posts/{post_id}")
    Call<Post> getPostDetail(@Path("post_id") String postId);

    @GET("/api/v1/notifications")
    Call<List<Notification>> getNotifications(
            @Query("user_id") String userId,
            @Query("type") String type,
            @Query("is_read") Boolean isRead
    );
    @PUT("/api/v1/sites/{siteId}")
    Call<Site> updateSite(
            @Path("siteId") String siteId,
            @Body SiteRegisterRequest request
    );


    @POST("/api/v1/notifications/{notification_id}/read")
    Call<Void> markNotificationAsRead(@Path("notification_id") String notificationId);

    @POST("/api/v1/fcm/register")
    Call<Void> registerFcmToken(@Body FcmTokenRequest request);

    @DELETE("/api/v1/fcm/unregister")
    Call<Void> unregisterFcmToken(@Query("fcm_token") String fcmToken);
}
