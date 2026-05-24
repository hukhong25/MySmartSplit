package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.UserRepository;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository = new UserRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<User> getUser() { return user; }
    public LiveData<Boolean> getUpdateSuccess() { return updateSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadUserProfile() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        isLoading.setValue(true);
        userRepository.getUserById(uid, new UserRepository.OnUserLoadedListener() {
            @Override
            public void onLoaded(User loadedUser) {
                user.setValue(loadedUser);
                isLoading.setValue(false);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void updateProfile(String newName, String base64Image) {
        FirebaseUser fUser = mAuth.getCurrentUser();
        if (fUser == null) return;

        isLoading.setValue(true);
        userRepository.updateFirebaseProfile(newName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", newName);
                        if (base64Image != null) {
                            updates.put("photoUrl", base64Image);
                        }

                        userRepository.updateFirestoreUser(fUser.getUid(), updates)
                                .addOnSuccessListener(aVoid -> {
                                    updateSuccess.setValue(true);
                                    isLoading.setValue(false);
                                })
                                .addOnFailureListener(e -> {
                                    error.setValue(e.getMessage());
                                    isLoading.setValue(false);
                                });
                    } else {
                        error.setValue(task.getException() != null ? task.getException().getMessage() : "Profile update failed");
                        isLoading.setValue(false);
                    }
                });
    }
}
