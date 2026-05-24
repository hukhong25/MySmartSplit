package vn.haui.smartsplit.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.models.AppNotification;

public class NotificationRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnNotificationsLoadedListener {
        void onLoaded(List<AppNotification> notifications);
        void onError(Exception e);
    }

    public ListenerRegistration getNotificationsByUser(String userId, OnNotificationsLoadedListener listener) {
        return db.collection("notifications")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<AppNotification> notifications = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            notifications.add(doc.toObject(AppNotification.class));
                        }
                    }
                    listener.onLoaded(notifications);
                });
    }

    public Task<Void> markAsRead(String notificationId) {
        return db.collection("notifications").document(notificationId).update("read", true);
    }

    public Task<Void> sendNotification(AppNotification notification) {
        return db.collection("notifications").document(notification.getId()).set(notification);
    }

    public String generateId() {
        return db.collection("notifications").document().getId();
    }
}
