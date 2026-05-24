package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.UserRepository;

public class AuthViewModel extends ViewModel {
    private final UserRepository userRepository = new UserRepository();
    
    private final MutableLiveData<FirebaseUser> firebaseUser = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<FirebaseUser> getFirebaseUser() { return firebaseUser; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void login(String email, String password) {
        isLoading.setValue(true);
        userRepository.login(email, password)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        firebaseUser.setValue(task.getResult().getUser());
                    } else {
                        error.setValue(task.getException() != null ? task.getException().getMessage() : "Login failed");
                    }
                });
    }

    public void register(String email, String password, String fullName) {
        isLoading.setValue(true);
        userRepository.register(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fUser = task.getResult().getUser();
                        if (fUser != null) {
                            // Update display name in Firebase Auth
                            userRepository.updateFirebaseProfile(fullName)
                                    .addOnCompleteListener(profileTask -> {
                                        // Save user info to Firestore
                                        User user = new User(fUser.getUid(), fullName, email);
                                        userRepository.saveUserToFirestore(user)
                                                .addOnCompleteListener(dbTask -> {
                                                    isLoading.setValue(false);
                                                    if (dbTask.isSuccessful()) {
                                                        firebaseUser.setValue(fUser);
                                                    } else {
                                                        error.setValue(dbTask.getException() != null ? dbTask.getException().getMessage() : "Firestore save failed");
                                                    }
                                                });
                                    });
                        }
                    } else {
                        isLoading.setValue(false);
                        error.setValue(task.getException() != null ? task.getException().getMessage() : "Registration failed");
                    }
                });
    }
}
