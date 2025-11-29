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
import retrofit2.http.Path;
import retrofit2.http.Query;
import com.example.mobile_android.model.Site;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.model.UserStatistics;

import java.util.List;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("/api/v1/sites/")
    Call<SiteRegisterResponse> registerSite(@Header("Authorization") String token, @Body SiteRegisterRequest request);

    // 등록한 사이트 전체 불러오기
    @GET("/api/v1/sites")
    Call<List<Site>> getSites(@Header("Authorization") String token);

    // 사이트 상세 조회
    @GET("/api/v1/sites/{siteId}")
    Call<Site> getSiteById(@Header("Authorization") String token, @Path("siteId") String siteId);

    // 사이트 수정
    @PATCH("/api/v1/sites/{siteId}")
    Call<Site> updateSite(@Header("Authorization") String token, @Path("siteId") String siteId, @Body Site site);

    // 사이트 삭제
    @DELETE("/api/v1/sites/{siteId}")
    Call<Void> deleteSite(@Header("Authorization") String token, @Path("siteId") String siteId);

    @GET("/api/v1/posts/list")
    Call<PostListResponse> getPosts(
            @Header("Authorization") String token,
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
    Call<Post> getPostDetail(@Header("Authorization") String token, @Path("post_id") String postId);

    @GET("/api/v1/notifications")
    Call<List<Notification>> getNotifications(
            @Header("Authorization") String token,
            @Query("user_id") String userId,
            @Query("type") String type,
            @Query("is_read") Boolean isRead
    );

    @POST("/api/v1/notifications/{notification_id}/read")
    Call<Void> markNotificationAsRead(@Header("Authorization") String token, @Path("notification_id") String notificationId);

    @POST("/api/v1/fcm/register")
    Call<Void> registerFcmToken(@Header("Authorization") String token, @Body FcmTokenRequest request);

    @DELETE("/api/v1/fcm/unregister")
    Call<Void> unregisterFcmToken(@Header("Authorization") String token, @Query("fcm_token") String fcmToken);

    // ========== 캘린더 일정 API ==========

    // 일정 생성
    @POST("/api/v1/calendar-events")
    Call<CalendarEvent> createEvent(@Header("Authorization") String token, @Body CalendarEvent event);

    // 일정 목록 조회
    @GET("/api/v1/calendar-events")
    Call<List<CalendarEvent>> getEvents(
            @Header("Authorization") String token,
            @Query("user_id") String userId,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
    );

    // 일정 수정
    @PUT("/api/v1/calendar-events/{id}")
    Call<CalendarEvent> updateEvent(@Header("Authorization") String token, @Path("id") String id, @Body CalendarEvent event);

    // 일정 삭제
    @DELETE("/api/v1/calendar-events/{id}")
    Call<Void> deleteEvent(@Header("Authorization") String token, @Path("id") String id);

    // ========== 통계 API ==========

    // 사용자 활동 통계 조회
    @GET("/api/v1/users/{userId}/statistics")
    Call<UserStatistics> getUserStatistics(@Header("Authorization") String token, @Path("userId") String userId);
}
