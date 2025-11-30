package com.example.mobile_android.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.mobile_android.model.Post;

import java.util.List;

@Dao
public abstract class PostDao {

    /**
     * 특정 ID의 Post를 조회합니다.
     */
    @Query("SELECT * FROM post WHERE id = :postId")
    public abstract LiveData<Post> getPostById(String postId);

    /**
     * 특정 사이트의 모든 Post를 최신순으로 조회합니다.
     */
    @Query("SELECT * FROM post WHERE siteId = :siteId ORDER BY createdAt DESC")
    public abstract LiveData<List<Post>> getPostsBySite(String siteId);

    /**
     * 특정 사이트의 게시물 수를 동기적으로 조회합니다 (RecyclerView Adapter용).
     */
    @Query("SELECT COUNT(*) FROM post WHERE siteId = :siteId")
    public abstract int getPostCountBySiteSync(String siteId);

    /**
     * 특정 사이트의 가장 최근 게시물의 createdAt을 동기적으로 조회합니다.
     * 마지막 크롤링 날짜를 표시하는 데 사용됩니다.
     */
    @Query("SELECT createdAt FROM post WHERE siteId = :siteId ORDER BY createdAt DESC LIMIT 1")
    public abstract String getLatestPostDateBySiteSync(String siteId);

    /**
     * 특정 사이트의 가장 최근 게시물의 createdAt을 LiveData로 조회합니다.
     * 마지막 크롤링 날짜를 표시하는 데 사용됩니다.
     */
    @Query("SELECT createdAt FROM post WHERE siteId = :siteId ORDER BY createdAt DESC LIMIT 1")
    public abstract LiveData<String> getLatestPostDateBySite(String siteId);

    /**
     * 데이터베이스의 모든 Post를 최신순으로 조회합니다.
     */
    @Query("SELECT * FROM post ORDER BY createdAt DESC")
    public abstract LiveData<List<Post>> getAllPosts();

    /**
     * is_new가 true인 게시물 개수를 조회합니다.
     * 주의: 실제 "새 게시물" 판단은 Post.isActuallyNew() 메서드를 사용하는 것을 권장합니다.
     */
    @Query("SELECT COUNT(*) FROM post WHERE isNew = 1")
    public abstract LiveData<Integer> getNewPostCount();

    /**
     * 모든 게시물을 조회합니다 (is_new 필터링 없이).
     * 애플리케이션 레벨에서 Post.isActuallyNew()로 필터링할 수 있습니다.
     */
    @Query("SELECT * FROM post ORDER BY createdAt DESC")
    public abstract LiveData<List<Post>> getAllPostsForNewFilter();

    /**
     * 캘린더에 저장된 모든 Post를 조회합니다.
     */
    @Query("SELECT * FROM post WHERE isSaved = 1 ORDER BY eventStartDate DESC")
    public abstract LiveData<List<Post>> getSavedPosts();

    /**
     * 데이터베이스의 모든 Post의 고유 카테고리 목록을 조회합니다.
     */
    @Query("SELECT DISTINCT category FROM post WHERE category IS NOT NULL AND category != '' ORDER BY category")
    public abstract LiveData<List<String>> getDistinctCategories();

    /**
     * 특정 사이트의 Post의 고유 카테고리 목록을 조회합니다.
     */
    @Query("SELECT DISTINCT category FROM post WHERE siteId = :siteId AND category IS NOT NULL AND category != '' ORDER BY category")
    public abstract LiveData<List<String>> getCategoriesBySite(String siteId);

    /**
     * 특정 Post의 캘린더 저장 상태를 업데이트합니다.
     */
    @Query("UPDATE post SET isSaved = :isSaved WHERE id = :postId")
    public abstract void updateSaveState(String postId, boolean isSaved);

    /**
     * 단일 Post를 데이터베이스에 삽입 또는 교체합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(Post post);

    /**
     * 특정 Post가 저장되어 있는지 확인합니다.
     * @return 저장되어 있다면 true, 아니거나 없으면 false 또는 null
     */
    @Query("SELECT isSaved FROM post WHERE id = :postId LIMIT 1")
    public abstract Boolean isPostSaved(String postId);

    /**
     * 모든 Post를 삭제합니다.
     */
    @Query("DELETE FROM post")
    public abstract void deleteAll();

    /**
     * 특정 사이트의 모든 Post를 삭제합니다.
     */
    @Query("DELETE FROM post WHERE siteId = :siteId")
    public abstract void deletePostsBySite(String siteId);

    /**
     * 여러 Post를 한번에 삽입합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertAll(List<Post> posts);

    /**
     * 네트워크에서 가져온 Post 목록을 데이터베이스에 덮어씁니다.
     * 오프라인 상태에서 데이터 보존을 위한 캐시로 사용됩니다.
     * 기존 게시물의 isSaved 상태를 유지하면서 전체 삭제 후 덮어씁니다.
     */
    @Transaction
    public void upsert(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            deleteAll();
            return;
        }

        // 기존 게시물의 저장 상태를 먼저 백업
        for (Post newPost : posts) {
            Boolean savedState = isPostSaved(newPost.getId());
            if (savedState != null && savedState) {
                newPost.setSaved(true);
            }
        }

        // 전체 삭제 후 새 데이터 삽입
        deleteAll();
        insertAll(posts);
    }

    /**
     * 특정 사이트의 Post 목록을 덮어씁니다.
     * 기존 게시물의 isSaved 상태를 유지하면서 해당 사이트 게시물만 삭제 후 덮어씁니다.
     */
    @Transaction
    public void upsertBySite(String siteId, List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            deletePostsBySite(siteId);
            return;
        }

        // 기존 게시물의 저장 상태를 먼저 백업
        for (Post newPost : posts) {
            Boolean savedState = isPostSaved(newPost.getId());
            if (savedState != null && savedState) {
                newPost.setSaved(true);
            }
        }

        // 해당 사이트의 기존 데이터 삭제 후 새 데이터 삽입
        deletePostsBySite(siteId);
        insertAll(posts);
    }
}
