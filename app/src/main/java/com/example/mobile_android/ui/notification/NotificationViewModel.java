package com.example.mobile_android.ui.notification;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile_android.data.NotificationRepository;
import com.example.mobile_android.data.local.NotificationEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class NotificationViewModel extends AndroidViewModel {

    private final NotificationRepository repository;
    private final String userId;
    private final LiveData<List<NotificationEntity>> notifications;
    private final LiveData<Integer> unreadCount;

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        repository = NotificationRepository.getInstance(application);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : null;
        if (userId != null) {
            notifications = repository.getNotifications(userId);
            unreadCount = repository.getUnreadCount(userId);

            // 더미 데이터 정리 (한 번만 실행)
            clearDummyDataOnce(application, userId);
        } else {
            MutableLiveData<List<NotificationEntity>> emptyNotifications = new MutableLiveData<>();
            emptyNotifications.setValue(java.util.Collections.emptyList());
            notifications = emptyNotifications;
            unreadCount = new MutableLiveData<>(0);
        }
    }

    /**
     * 앱 시작 시 한 번만 더미 데이터를 정리합니다.
     */
    private void clearDummyDataOnce(Application application, String userId) {
        SharedPreferences prefs = application.getSharedPreferences("notification_cleanup", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("dummy_data_cleaned_v2", false)) {
            repository.clearDummyData(userId);
            prefs.edit().putBoolean("dummy_data_cleaned_v2", true).apply();
        }
    }

    public LiveData<List<NotificationEntity>> getNotifications() {
        return notifications;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    public void markAsRead(String notificationId) {
        repository.markAsRead(notificationId);
    }

    public void delete(String notificationId) {
        repository.delete(notificationId);
    }
}
