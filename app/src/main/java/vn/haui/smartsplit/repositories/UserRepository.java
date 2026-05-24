package vn.haui.smartsplit.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;
import vn.haui.smartsplit.models.User;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public interface OnUserLoadedListener {
        void onLoaded(User user);
        void onError(Exception e);
    }

    public void getUserById(String uid, OnUserLoadedListener listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setUid(documentSnapshot.getId());
                        listener.onLoaded(user);
                    } else {
                        listener.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public Task<QuerySnapshot> getUserByEmail(String email) {
        return db.collection("users").whereEqualTo("email", email).get();
    }

    public Task<AuthResult> login(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<Void> saveUserToFirestore(User user) {
        return db.collection("users").document(user.getUid()).set(user);
    }

    public Task<Void> updateFirestoreUser(String uid, Map<String, Object> updates) {
        return db.collection("users").document(uid).update(updates);
    }

    public Task<Void> updateFirebaseProfile(String displayName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();
            return user.updateProfile(profileUpdates);
        }
        return null;
    }

    public Task<Void> reauthenticate(String email, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);
            return user.reauthenticate(credential);
        }
        return null;
    }

    public Task<Void> updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return user.updatePassword(newPassword);
        }
        return null;
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return mAuth.getCurrentUser();
    }
}
