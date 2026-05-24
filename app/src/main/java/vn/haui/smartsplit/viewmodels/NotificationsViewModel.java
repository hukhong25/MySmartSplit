package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.repositories.NotificationRepository;

public class NotificationsViewModel extends ViewModel {
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<AppNotification>> notifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration notificationsListener;

    public LiveData<List<AppNotification>> getNotifications() { return notifications; }
    public LiveData<String> getError() { return error; }

    public void loadNotifications() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (notificationsListener != null) notificationsListener.remove();

        notificationsListener = notificationRepository.getNotificationsByUser(uid, new NotificationRepository.OnNotificationsLoadedListener() {
            @Override
            public void onLoaded(List<AppNotification> loadedNotifications) {
                // Sort by timestamp descending
                Collections.sort(loadedNotifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                notifications.setValue(loadedNotifications);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    public void markAsRead(String notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (notificationsListener != null) notificationsListener.remove();
    }
}
