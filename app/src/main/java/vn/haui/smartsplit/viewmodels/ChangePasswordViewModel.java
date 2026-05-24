package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

import vn.haui.smartsplit.repositories.UserRepository;

public class ChangePasswordViewModel extends ViewModel {
    private final UserRepository userRepository = new UserRepository();

    private final MutableLiveData<Boolean> success = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<Boolean> getSuccess() { return success; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = userRepository.getCurrentFirebaseUser();
        if (user == null || user.getEmail() == null) {
            error.setValue("User not logged in");
            return;
        }

        isLoading.setValue(true);
        userRepository.reauthenticate(user.getEmail(), currentPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userRepository.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    isLoading.setValue(false);
                                    if (updateTask.isSuccessful()) {
                                        success.setValue(true);
                                    } else {
                                        error.setValue(updateTask.getException() != null ? updateTask.getException().getMessage() : "Update failed");
                                    }
                                });
                    } else {
                        isLoading.setValue(false);
                        error.setValue("REAUTH_FAILED");
                    }
                });
    }
}
