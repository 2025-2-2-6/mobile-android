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
     * 데이터베이스의 모든 Post를 최신순으로 조회합니다.
     */
    @Query("SELECT * FROM post ORDER BY createdAt DESC")
    public abstract LiveData<List<Post>> getAllPosts();

    /**
     * 캘린더에 저장된 모든 Post를 조회합니다.
     */
    @Query("SELECT * FROM post WHERE isSaved = 1 ORDER BY eventStartDate DESC")
    public abstract LiveData<List<Post>> getSavedPosts();

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
     */
    @Query("SELECT isSaved FROM post WHERE id = :postId")
    public abstract boolean isPostSaved(String postId);

    /**
     * 네트워크에서 가져온 Post 목록을 데이터베이스에 삽입/업데이트(upsert)합니다.
     */
    @Transaction
    public void upsert(List<Post> posts) {
        for (Post post : posts) {
            boolean isSaved = isPostSaved(post.getId());
            post.setSaved(isSaved);
            insert(post);
        }
    }
}
