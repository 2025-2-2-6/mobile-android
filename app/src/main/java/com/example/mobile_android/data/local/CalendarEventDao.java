package com.example.mobile_android.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.mobile_android.model.CalendarEvent;

import java.util.List;

@Dao
public abstract class CalendarEventDao {

    /**
     * 모든 캘린더 이벤트를 조회합니다.
     */
    @Query("SELECT * FROM calendar_events ORDER BY start_time ASC")
    public abstract LiveData<List<CalendarEvent>> getAllEvents();

    /**
     * 특정 날짜의 이벤트를 조회합니다.
     * @param date YYYY-MM-DD 형식
     */
    @Query("SELECT * FROM calendar_events WHERE start_time LIKE :date || '%' ORDER BY start_time ASC")
    public abstract LiveData<List<CalendarEvent>> getEventsByDate(String date);

    /**
     * 특정 사용자의 모든 이벤트를 조회합니다.
     */
    @Query("SELECT * FROM calendar_events WHERE user_id = :userId ORDER BY start_time ASC")
    public abstract LiveData<List<CalendarEvent>> getEventsByUserId(String userId);

    /**
     * 특정 ID의 이벤트를 조회합니다 (LiveData).
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    public abstract LiveData<CalendarEvent> getEventByIdLive(String id);

    /**
     * 특정 ID의 이벤트를 조회합니다 (동기).
     * 삭제 전에 post_id를 확인할 때 사용.
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    public abstract CalendarEvent getEventById(String id);

    /**
     * Post ID로 캘린더 이벤트를 조회합니다 (동기).
     * Post에서 생성된 캘린더 이벤트를 찾아 삭제할 때 사용.
     */
    @Query("SELECT * FROM calendar_events WHERE post_id = :postId LIMIT 1")
    public abstract CalendarEvent getEventByPostId(String postId);

    /**
     * 이벤트를 삽입합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(CalendarEvent event);

    /**
     * 여러 이벤트를 한번에 삽입합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract void insertAll(List<CalendarEvent> events);

    /**
     * 이벤트를 업데이트합니다.
     */
    @Update
    public abstract void update(CalendarEvent event);

    /**
     * 이벤트를 삭제합니다.
     */
    @Delete
    public abstract void delete(CalendarEvent event);

    /**
     * 특정 ID의 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events WHERE id = :id")
    public abstract void deleteById(String id);

    /**
     * 특정 사용자의 모든 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events WHERE user_id = :userId")
    public abstract void deleteByUserId(String userId);

    /**
     * 모든 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events")
    protected abstract void deleteAll();

    /**
     * 이벤트가 있는 날짜 목록을 조회합니다 (중복 제거).
     * start_time에서 YYYY-MM-DD 부분만 추출
     */
    @Query("SELECT DISTINCT substr(start_time, 1, 10) as event_date FROM calendar_events ORDER BY event_date ASC")
    public abstract LiveData<List<String>> getEventDates();

    /**
     * 서버에서 받은 최신 데이터로 전체 캘린더 이벤트를 덮어씁니다.
     * SiteDao.replaceAll()과 동일한 패턴: 트랜잭션으로 전체 삭제 후 삽입
     */
    @Transaction
    public void replaceAll(List<CalendarEvent> events) {
        // 전체 캘린더 이벤트를 삭제하고 서버에서 받은 데이터로 덮어씀
        deleteAll();
        if (events != null && !events.isEmpty()) {
            insertAll(events);
        }
    }
}
