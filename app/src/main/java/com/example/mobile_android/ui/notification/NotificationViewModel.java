package com.example.mobile_android.ui.notification;

import android.app.Application;

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
        } else {
            MutableLiveData<List<NotificationEntity>> emptyNotifications = new MutableLiveData<>();
            emptyNotifications.setValue(java.util.Collections.emptyList());
            notifications = emptyNotifications;
            unreadCount = new MutableLiveData<>(0);
        }
    }

    public LiveData<List<NotificationEntity>> getNotifications() {
        return notifications;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    public void refresh() {
        if (userId != null) {
            repository.refreshFromServer(userId);
        }
    }

    public void markAsRead(String notificationId) {
        repository.markAsRead(notificationId);
    }

    public void delete(String notificationId) {
        repository.delete(notificationId);
    }
}
