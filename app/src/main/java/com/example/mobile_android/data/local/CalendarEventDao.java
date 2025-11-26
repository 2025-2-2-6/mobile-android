package com.example.mobile_android.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobile_android.model.CalendarEvent;

import java.util.List;

@Dao
public interface CalendarEventDao {

    /**
     * 모든 캘린더 이벤트를 조회합니다.
     */
    @Query("SELECT * FROM calendar_events ORDER BY start_time ASC")
    LiveData<List<CalendarEvent>> getAllEvents();

    /**
     * 특정 날짜의 이벤트를 조회합니다.
     * @param date YYYY-MM-DD 형식
     */
    @Query("SELECT * FROM calendar_events WHERE start_time LIKE :date || '%' ORDER BY start_time ASC")
    LiveData<List<CalendarEvent>> getEventsByDate(String date);

    /**
     * 특정 사용자의 모든 이벤트를 조회합니다.
     */
    @Query("SELECT * FROM calendar_events WHERE user_id = :userId ORDER BY start_time ASC")
    LiveData<List<CalendarEvent>> getEventsByUserId(String userId);

    /**
     * 특정 ID의 이벤트를 조회합니다 (LiveData).
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    LiveData<CalendarEvent> getEventByIdLive(String id);

    /**
     * 특정 ID의 이벤트를 조회합니다 (동기).
     * 삭제 전에 post_id를 확인할 때 사용.
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    CalendarEvent getEventById(String id);

    /**
     * Post ID로 캘린더 이벤트를 조회합니다 (동기).
     * Post에서 생성된 캘린더 이벤트를 찾아 삭제할 때 사용.
     */
    @Query("SELECT * FROM calendar_events WHERE post_id = :postId LIMIT 1")
    CalendarEvent getEventByPostId(String postId);

    /**
     * 이벤트를 삽입합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CalendarEvent event);

    /**
     * 여러 이벤트를 한번에 삽입합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CalendarEvent> events);

    /**
     * 이벤트를 업데이트합니다.
     */
    @Update
    void update(CalendarEvent event);

    /**
     * 이벤트를 삭제합니다.
     */
    @Delete
    void delete(CalendarEvent event);

    /**
     * 특정 ID의 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events WHERE id = :id")
    void deleteById(String id);

    /**
     * 특정 사용자의 모든 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events WHERE user_id = :userId")
    void deleteByUserId(String userId);

    /**
     * 모든 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events")
    void deleteAll();

    /**
     * 이벤트가 있는 날짜 목록을 조회합니다 (중복 제거).
     * start_time에서 YYYY-MM-DD 부분만 추출
     */
    @Query("SELECT DISTINCT substr(start_time, 1, 10) as event_date FROM calendar_events ORDER BY event_date ASC")
    LiveData<List<String>> getEventDates();
}
