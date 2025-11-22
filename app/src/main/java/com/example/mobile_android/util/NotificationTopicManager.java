package com.example.mobile_android.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Utility for managing FCM topic subscriptions that back user notification toggles.
 */
public final class NotificationTopicManager {

    public static final String TOPIC_CRAWL_NEW_POSTS = "crawl_new_posts";
    public static final String TOPIC_CALENDAR_REMINDER = "calendar_reminder";

    private static final String TAG = "NotificationTopicMgr";

    private NotificationTopicManager() {
    }

    public interface TopicUpdateCallback {
        void onComplete(boolean success);
    }

    public static void updateTopic(@NonNull String topic, boolean enable, @Nullable TopicUpdateCallback callback) {
        Task<Void> task = enable
                ? FirebaseMessaging.getInstance().subscribeToTopic(topic)
                : FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);

        task.addOnCompleteListener(TaskExecutors.MAIN_THREAD, result -> {
            boolean success = result.isSuccessful();
            if (!success && result.getException() != null) {
                Log.w(TAG, "Failed to update topic " + topic, result.getException());
            }
            if (callback != null) {
                callback.onComplete(success);
            }
        });
    }

    /**
     * Best-effort sync without callback (e.g., when permission changes).
     */
    public static void ensureTopicState(@NonNull String topic, boolean enable) {
        Task<Void> task = enable
                ? FirebaseMessaging.getInstance().subscribeToTopic(topic)
                : FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
        task.addOnFailureListener(e -> Log.w(TAG, "ensureTopicState failed for " + topic, e));
    }
}
