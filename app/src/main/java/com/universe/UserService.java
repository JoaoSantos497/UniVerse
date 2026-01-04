package com.universe;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserService {
    private FirebaseFirestore db;
    private String myUid;
    public UserService() {
            this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            this.db = FirebaseFirestore.getInstance();
    }

    public WriteBatch followUser(WriteBatch batch, String tUid) {

        DocumentReference refFollowing = followingReferences(tUid);
        DocumentReference refFollower = followersReferences(tUid);
        DocumentReference refMe = db.collection("users").document(myUid);
        DocumentReference refTarget = db.collection("users").document(tUid);

        Map<String, Object> data = new HashMap<>();
        data.put("uid", tUid);
        data.put("timestamp", FieldValue.serverTimestamp());

        Map<String, Object> dataFollower = new HashMap<>();
        dataFollower.put("uid", myUid);
        dataFollower.put("timestamp", FieldValue.serverTimestamp());

        batch.set(refFollowing, data);
        batch.set(refFollower, dataFollower);

        batch.update(refMe, "followingCount", FieldValue.increment(1));
        batch.update(refTarget, "followersCount", FieldValue.increment(1));

        return batch;
    }
    public DocumentReference followingReferences (String targetUserId) {
        return db.collection("users").document(myUid).collection("following").document(targetUserId);
    }
    public DocumentReference followersReferences (String targetUserId) {
        return db.collection("users").document(targetUserId).collection("followers").document(myUid);
    }

    public WriteBatch unfollowUser(WriteBatch batch, String targetUserId) {
        DocumentReference refFollowing = followingReferences(targetUserId);
        DocumentReference refFollower = followersReferences(targetUserId);
        DocumentReference refMe = db.collection("users").document(myUid);
        DocumentReference refTarget = db.collection("users").document(targetUserId);

        batch.delete(refFollowing);
        batch.delete(refFollower);

        batch.update(refMe, "followingCount", FieldValue.increment(-1));
        batch.update(refTarget, "followersCount", FieldValue.increment(-1));

        return batch;
    }

    public Task<Optional<User>> getUser(String userId) {
        return db.collection("users")
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw requireNonNull(task.getException());
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        return ofNullable(doc.toObject(User.class));
                    }
                    return empty();
                });
    }

    public  Task<Boolean> getFollowing(String targetUid) {
        return db.collection("users")
                .document(myUid)
                .collection("following")
                .document(targetUid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw requireNonNull(task.getException());
                    }
                    DocumentSnapshot doc = task.getResult();
                    return doc != null && doc.exists();
                });
    }
}
