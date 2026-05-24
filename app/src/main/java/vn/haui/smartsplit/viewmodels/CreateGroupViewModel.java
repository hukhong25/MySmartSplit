package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.GroupRepository;

public class CreateGroupViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<User>> addedMembers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> createSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<User>> getAddedMembers() { return addedMembers; }
    public LiveData<Boolean> getCreateSuccess() { return createSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void addMemberByEmail(String email) {
        if (email.isEmpty()) return;

        if (mAuth.getCurrentUser() != null && email.equalsIgnoreCase(mAuth.getCurrentUser().getEmail())) {
            error.setValue("Bạn đã là thành viên của nhóm");
            return;
        }

        List<User> currentList = addedMembers.getValue();
        if (currentList != null) {
            for (User u : currentList) {
                if (email.equalsIgnoreCase(u.getEmail())) {
                    error.setValue("Người dùng này đã có trong danh sách");
                    return;
                }
            }
        }

        isLoading.setValue(true);
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        List<User> list = addedMembers.getValue();
                        if (list == null) list = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            list.add(user);
                        }
                        addedMembers.setValue(list);
                    } else {
                        error.setValue("Không tìm thấy người dùng với email này");
                    }
                });
    }

    public void removeMember(User user) {
        List<User> list = addedMembers.getValue();
        if (list != null) {
            list.remove(user);
            addedMembers.setValue(list);
        }
    }

    public void createGroup(String groupName) {
        if (mAuth.getCurrentUser() == null) return;
        isLoading.setValue(true);

        String currentUserId = mAuth.getCurrentUser().getUid();
        String groupId = groupRepository.generateGroupId();
        String joinCode = generateJoinCode();

        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUserId);
        List<User> members = addedMembers.getValue();
        if (members != null) {
            for (User u : members) {
                memberIds.add(u.getUid());
            }
        }

        Group newGroup = new Group(groupId, groupName, memberIds, currentUserId, joinCode);

        groupRepository.createGroup(newGroup)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    createSuccess.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue(e.getMessage());
                });
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 6) {
            int index = (int) (rnd.nextFloat() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}
