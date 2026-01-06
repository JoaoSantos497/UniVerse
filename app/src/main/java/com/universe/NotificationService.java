package com.universe;

import static com.google.firebase.firestore.Query.Direction.DESCENDING;
import static com.universe.NotificationType.FOLLOW;

import android.content.res.Resources.NotFoundException;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NotificationService {

    private static final String TARGET_USER_ID = "targetUserId";
    private static final String TIMESTAMP = "timestamp";
    private static final String READ = "read";

    private final UserService userService;
    private final FirebaseFirestore db;
    private final String myUid;

    public NotificationService(UserService userService) {
        this.userService = userService;
        this.db = FirebaseFirestore.getInstance();
        this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public ListenerRegistration listenToNotifications(
            String targetUserId,
            DataListener<List<Notification>> listener
    ) {
        return notificationCollectionRef()
                .whereEqualTo(TARGET_USER_ID, targetUserId)
                .orderBy(TIMESTAMP, DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots == null) {
                        listener.onData(new ArrayList<>());
                        return;
                    }

                    List<Notification> result = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setNotificationId(doc.getId());
                            result.add(n);
                        }
                    }

                    listener.onData(result);
                });
    }

    public ListenerRegistration listenForUnreadNotifications(
            String targetUserId,
            DataListener<Boolean> listener
    ) {
        return notificationCollectionRef()
                .whereEqualTo(TARGET_USER_ID, targetUserId)
                .whereEqualTo(READ, false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    boolean hasUnread = snapshots != null && !snapshots.isEmpty();

                    listener.onData(hasUnread);
                });
    }

    public Task<Void> markAsRead(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return Tasks.forResult(null);
        }

        WriteBatch batch = db.batch();

        notifications.stream()
                .filter(n -> !n.isRead())
                .map(Notification::getNotificationId)
                .filter(Objects::nonNull)
                .map(id -> notificationCollectionRef().document(id))
                .forEach(ref -> batch.update(ref, READ, true));

        return batch.commit();
    }

    public Task<Void> addFollowNotificationToBatch(WriteBatch batch, String targetUserId) {
        return createNotificationInternal(targetUserId, FOLLOW, null)
                .onSuccessTask(notification -> {
                    DocumentReference ref = notificationRef();
                    batch.set(ref, notification);
                    return Tasks.forResult(null);
                });
    }

    public Task<Notification> sendNotification(
            String targetUserId,
            NotificationType type
    ) {
        return sendNotificationInternal(targetUserId, type, null);
    }

    public Task<Notification> sendNotification(
            String targetUserId,
            NotificationType type,
            String postId
    ) {
        return sendNotificationInternal(targetUserId, type, postId);
    }

    private Task<Notification> sendNotificationInternal(
            String targetUserId,
            NotificationType type,
            String postId
    ) {
        return createNotificationInternal(targetUserId, type, postId)
                .onSuccessTask(notification ->
                        notificationCollectionRef()
                                .add(notification)
                                .onSuccessTask(ref -> {
                                    notification.setNotificationId(ref.getId());
                                    return Tasks.forResult(notification);
                                })
                );
    }

    private DocumentReference notificationRef() {
        return notificationCollectionRef().document();
    }

    private CollectionReference notificationCollectionRef() {
        return db.collection("notifications");
    }

    private Task<Notification> createNotificationInternal(
            String targetUserId,
            NotificationType type,
            String postId
    ) {
        if (targetUserId.equals(myUid)) {
            return Tasks.forException(
                    new IllegalStateException("User cannot notify themselves")
            );
        }

        return userService.getUser(myUid)
                .onSuccessTask(userOpt ->
                        userOpt.map(user ->
                                Tasks.forResult(
                                        new Notification(
                                                user.getUid(),
                                                user.getNome(),
                                                user.getPhotoUrl(),
                                                targetUserId,
                                                type,
                                                postId
                                        )
                                )
                        ).orElseGet(() ->
                                Tasks.forException(new NotFoundException())
                        )
                );
    }
}
