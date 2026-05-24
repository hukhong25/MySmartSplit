package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.ExpenseRepository;
import vn.haui.smartsplit.repositories.GroupRepository;
import vn.haui.smartsplit.repositories.NotificationRepository;
import vn.haui.smartsplit.repositories.UserRepository;

public class SettleUpViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();

    private final MutableLiveData<List<User>> memberList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LiveData<List<User>> getMemberList() { return memberList; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void loadMembers(String groupId) {
        if (groupId == null) return;
        isLoading.setValue(true);
        groupRepository.getGroupById(groupId, new GroupRepository.OnGroupLoadedListener() {
            @Override
            public void onLoaded(vn.haui.smartsplit.models.Group group) {
                fetchUsers(group.getMemberIds());
            }

            @Override
            public void onError(Exception e) {
                isLoading.setValue(false);
                error.setValue(e.getMessage());
            }
        });
    }

    private void fetchUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            isLoading.setValue(false);
            return;
        }
        List<User> users = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        for (String uid : userIds) {
            userRepository.getUserById(uid, new UserRepository.OnUserLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    users.add(user);
                    if (count.incrementAndGet() == userIds.size()) {
                        memberList.setValue(users);
                        isLoading.setValue(false);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (count.incrementAndGet() == userIds.size()) {
                        memberList.setValue(users);
                        isLoading.setValue(false);
                    }
                }
            });
        }
    }

    public void confirmSettle(User from, User to, double amount, String base64Image, String groupId, String descFormat, String notifTitle, String notifContentFormat) {
        isLoading.setValue(true);
        // Changed to Map<String, Object> to match the refactored Expense model
        Map<String, Object> split = new HashMap<>();
        split.put(to.getUid(), amount);

        Expense settlement = new Expense();
        String id = notificationRepository.generateId();
        settlement.setId(id);
        settlement.setDescription(String.format(descFormat, from.getName(), to.getName()));
        settlement.setAmount(amount);
        settlement.setPayerId(from.getUid());
        settlement.setPayerName(from.getName());
        settlement.setGroupId(groupId);
        settlement.setTimestamp(System.currentTimeMillis());
        settlement.setSplitDetails(split);
        settlement.setProofImageUrl(base64Image);
        settlement.setSettlement(true);
        settlement.setStatus(Expense.STATUS_PENDING);

        expenseRepository.saveExpense(settlement)
                .addOnSuccessListener(aVoid -> {
                    sendNotification(to.getUid(), from.getName(), amount, id, notifTitle, notifContentFormat);
                    saveSuccess.setValue(true);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    error.setValue(e.getMessage());
                });
    }

    private void sendNotification(String receiverId, String senderName, double amount, String expenseId, String title, String contentFormat) {
        String notifId = notificationRepository.generateId();
        AppNotification notif = new AppNotification(
                notifId, receiverId, title,
                String.format(contentFormat, senderName, (long) amount),
                "PAYMENT_REQUEST", expenseId
        );
        notificationRepository.sendNotification(notif);
    }
}
