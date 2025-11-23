package com.example.mobile_android.fcm.handler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.mobile_android.MainActivity;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.data.local.SiteDao;
import com.example.mobile_android.fcm.CrawlStatus;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;
import com.example.mobile_android.util.TokenManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrawlNewPostsHandler extends BaseNotificationHandler {

    private static final String TAG = "CrawlNewPostsHandler";

    @Override
    public void handle(Context context, Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));
        String siteId = data.get("site_id");

        Log.d(TAG, "FCM 알림 처리 - status: " + status + ", siteId: " + siteId);

        // 크롤링 성공 또는 새 게시물이 있을 때 데이터 갱신
        if (status == CrawlStatus.SUCCESS || status == CrawlStatus.NEW_POST) {
            refreshData(context, siteId);
        }
    }

    private void refreshData(Context context, String siteId) {
        String token = TokenManager.getBearerToken(context);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 사이트 목록 갱신
        apiService.getSites(token).enqueue(new Callback<List<Site>>() {
            @Override
            public void onResponse(Call<List<Site>> call, Response<List<Site>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Site> sites = response.body();
                    Log.d(TAG, "사이트 목록 갱신: " + sites.size() + "개");
                    executor.execute(() -> {
                        SiteDao siteDao = AppDatabase.getInstance(context).siteDao();
                        siteDao.replaceAll(sites);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Site>> call, Throwable t) {
                Log.e(TAG, "사이트 목록 갱신 실패", t);
            }
        });

        // 특정 사이트의 게시물 갱신
        if (siteId != null && !siteId.isEmpty()) {
            apiService.getPosts(token, 1, 100, null, siteId, null, null, "created_at", "desc")
                    .enqueue(new Callback<PostListResponse>() {
                        @Override
                        public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "게시물 갱신: " + response.body().getItems().size() + "개");
                                executor.execute(() -> {
                                    PostDao postDao = AppDatabase.getInstance(context).postDao();
                                    postDao.upsertBySite(siteId, response.body().getItems());
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<PostListResponse> call, Throwable t) {
                            Log.e(TAG, "게시물 갱신 실패", t);
                        }
                    });
        } else {
            // siteId가 없으면 전체 게시물 갱신
            apiService.getPosts(token, 1, 100, null, null, null, null, "created_at", "desc")
                    .enqueue(new Callback<PostListResponse>() {
                        @Override
                        public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "전체 게시물 갱신: " + response.body().getItems().size() + "개");
                                executor.execute(() -> {
                                    PostDao postDao = AppDatabase.getInstance(context).postDao();
                                    postDao.upsert(response.body().getItems());
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<PostListResponse> call, Throwable t) {
                            Log.e(TAG, "전체 게시물 갱신 실패", t);
                        }
                    });
        }
    }

    @Override
    public Intent getIntent(Context context, Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));
        String siteId = data.get("site_id");

        Intent intent = new Intent(context, MainActivity.class);

        // new_post 또는 success일 때 사이트 상세로 이동 가능
        if ((status == CrawlStatus.NEW_POST || status == CrawlStatus.SUCCESS)
                && siteId != null && !siteId.isEmpty()) {
            intent.putExtra("NAVIGATE_TO", "site");
            intent.putExtra("SITE_ID", siteId);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public String getTitle(Map<String, String> data) {
        CrawlStatus status = CrawlStatus.fromString(data.get("status"));
        String siteName = data.get("site_name");

        if (status == null) {
            return "알림";
        }

        switch (status) {
            case SUCCESS:
                return "사이트 등록 완료";
            case FAILED:
                return "크롤링 실패";
            case NEW_POST:
                return siteName != null ? siteName : "새 게시물";
            case UNKNOWN:
            default:
                return "알림";
        }
    }

    @Override
    public String getMessage(Map<String, String> data) {
        // 백엔드에서 자동 생성된 message 사용
        return data.getOrDefault("message", "");
    }
}
