package com.example.mobile_android.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.mobile_android.model.CalendarEvent;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.Site;

/**
 * Room database entry point.
 */
@Database(
        entities = {
                NotificationEntity.class,
                Post.class,
                Site.class,
                CalendarEvent.class
        },
        version = 8, // CalendarEvent 백엔드 API 스키마로 변경 + category 추가
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "mobile_android.db";
    private static volatile AppDatabase INSTANCE;

    public abstract NotificationDao notificationDao();

    public abstract PostDao postDao(); // Post 데이터 관리를 위한 DAO 추가

    public abstract SiteDao siteDao();

    public abstract CalendarEventDao calendarEventDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME
                    )
                    // 개발 중 스키마 변경 시, 기존 데이터를 삭제하고 새로 시작 (마이그레이션 불필요)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
