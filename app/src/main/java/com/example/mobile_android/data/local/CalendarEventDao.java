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
    @Query("SELECT * FROM calendar_events ORDER BY event_date ASC, event_time ASC")
    LiveData<List<CalendarEvent>> getAllEvents();

    /**
     * 특정 날짜의 이벤트를 조회합니다.
     * @param date YYYY-MM-DD 형식
     */
    @Query("SELECT * FROM calendar_events WHERE event_date = :date ORDER BY event_time ASC")
    LiveData<List<CalendarEvent>> getEventsByDate(String date);

    /**
     * 특정 ID의 이벤트를 조회합니다.
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    LiveData<CalendarEvent> getEventById(int id);

    /**
     * 이벤트를 삽입합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CalendarEvent event);

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
    void deleteById(int id);

    /**
     * 모든 이벤트를 삭제합니다.
     */
    @Query("DELETE FROM calendar_events")
    void deleteAll();

    /**
     * 이벤트가 있는 날짜 목록을 조회합니다 (중복 제거).
     */
    @Query("SELECT DISTINCT event_date FROM calendar_events ORDER BY event_date ASC")
    LiveData<List<String>> getEventDates();
}
