package com.example.mobile_android.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database entry point. Keeps notification history on-device so it survives process death.
 */
@Database(
        entities = {
                NotificationEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "mobile_android.db";
    private static volatile AppDatabase INSTANCE;

    public abstract NotificationDao notificationDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
