package com.example.mobile_android.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY " +
            "CASE WHEN created_at IS NULL THEN 1 ELSE 0 END, created_at DESC, received_at DESC")
    LiveData<List<NotificationEntity>> observeNotifications(String userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    LiveData<Integer> observeUnreadCount(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(NotificationEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(List<NotificationEntity> entities);

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    void markAsRead(String id);

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    void clearForUser(String userId);

    @Query("DELETE FROM notifications WHERE user_id = :userId AND title = :title")
    void deleteByTitle(String userId, String title);
}
