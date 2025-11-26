package com.example.mobile_android.util;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.data.local.SiteDao;
import com.example.mobile_android.data.local.CalendarEventDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.CalendarEvent;

import java.util.List;

/**
 * 통계 정보를 계산하고 관찰하는 유틸리티 클래스
 * HomeFragment와 MyPageFragment에서 공통으로 사용
 */
public class StatisticsHelper {

    /**
     * 통계 콜백 인터페이스
     */
    public interface StatisticsCallback {
        void onTotalSitesUpdated(int count);
        void onNewPostsUpdated(int count);
        void onSavedEventsUpdated(int count);
    }

    private final SiteDao siteDao;
    private final PostDao postDao;
    private final CalendarEventDao calendarEventDao;

    public StatisticsHelper(SiteDao siteDao, PostDao postDao, CalendarEventDao calendarEventDao) {
        this.siteDao = siteDao;
        this.postDao = postDao;
        this.calendarEventDao = calendarEventDao;
    }

    /**
     * 모든 통계를 관찰하고 변경 시 콜백 호출
     *
     * @param lifecycleOwner Fragment 또는 Activity
     * @param callback 통계 업데이트 콜백
     */
    public void observeStatistics(LifecycleOwner lifecycleOwner, StatisticsCallback callback) {
        // 전체 사이트 수 관찰
        observeTotalSites(lifecycleOwner, callback);

        // 새 게시물 수 관찰
        observeNewPosts(lifecycleOwner, callback);

        // 저장된 일정 수 관찰
        observeSavedEvents(lifecycleOwner, callback);
    }

    /**
     * 전체 사이트 수 관찰
     */
    public void observeTotalSites(LifecycleOwner lifecycleOwner, StatisticsCallback callback) {
        siteDao.observeAll().observe(lifecycleOwner, sites -> {
            int count = (sites != null) ? sites.size() : 0;
            callback.onTotalSitesUpdated(count);
        });
    }

    /**
     * 새 게시물 수 관찰
     * Post.isActuallyNew() 메서드로 실제 "새 게시물" 여부 판단
     */
    public void observeNewPosts(LifecycleOwner lifecycleOwner, StatisticsCallback callback) {
        postDao.getAllPostsForNewFilter().observe(lifecycleOwner, posts -> {
            if (posts != null) {
                long count = posts.stream()
                        .filter(post -> post.isActuallyNew())
                        .count();
                callback.onNewPostsUpdated((int) count);
            } else {
                callback.onNewPostsUpdated(0);
            }
        });
    }

    /**
     * 저장된 일정 수 관찰
     */
    public void observeSavedEvents(LifecycleOwner lifecycleOwner, StatisticsCallback callback) {
        calendarEventDao.getAllEvents().observe(lifecycleOwner, events -> {
            int count = (events != null) ? events.size() : 0;
            callback.onSavedEventsUpdated(count);
        });
    }

    /**
     * 현재 통계 정보를 한 번만 조회 (LiveData 관찰 없이)
     * 비동기 작업이므로 콜백으로 결과 전달
     */
    public void getCurrentStatistics(LifecycleOwner lifecycleOwner, StatisticsCallback callback) {
        // LiveData를 한 번만 관찰
        siteDao.observeAll().observe(lifecycleOwner, sites -> {
            int siteCount = (sites != null) ? sites.size() : 0;
            callback.onTotalSitesUpdated(siteCount);
        });

        postDao.getAllPostsForNewFilter().observe(lifecycleOwner, posts -> {
            if (posts != null) {
                long count = posts.stream()
                        .filter(post -> post.isActuallyNew())
                        .count();
                callback.onNewPostsUpdated((int) count);
            } else {
                callback.onNewPostsUpdated(0);
            }
        });

        calendarEventDao.getAllEvents().observe(lifecycleOwner, events -> {
            int count = (events != null) ? events.size() : 0;
            callback.onSavedEventsUpdated(count);
        });
    }
}
