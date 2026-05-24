package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.repositories.GroupRepository;

public class GroupsViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<Group>> groups = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration groupsListener;

    public LiveData<List<Group>> getGroups() { return groups; }
    public LiveData<String> getError() { return error; }

    public void loadGroups() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (groupsListener != null) groupsListener.remove();

        groupsListener = groupRepository.getGroupsByUser(uid, 100, new GroupRepository.OnGroupsLoadedListener() {
            @Override
            public void onLoaded(List<Group> loadedGroups) {
                groups.setValue(loadedGroups);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    public void joinGroup(String code) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        groupRepository.joinGroupWithCode(code, uid, new GroupRepository.OnActionListener() {
            @Override
            public void onSuccess() {
                // Success handled by Firestore listener
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (groupsListener != null) groupsListener.remove();
    }
}
