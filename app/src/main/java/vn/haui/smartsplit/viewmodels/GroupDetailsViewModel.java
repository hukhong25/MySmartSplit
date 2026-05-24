package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.repositories.ExpenseRepository;
import vn.haui.smartsplit.repositories.GroupRepository;
import vn.haui.smartsplit.repositories.UserRepository;

public class GroupDetailsViewModel extends ViewModel {
    private final GroupRepository groupRepository = new GroupRepository();
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final UserRepository userRepository = new UserRepository();

    private final MutableLiveData<Group> group = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> expenses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, Double>> balances = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<List<User>> members = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private ListenerRegistration groupListener;
    private ListenerRegistration expenseListener;

    public LiveData<Group> getGroup() { return group; }
    public LiveData<List<Expense>> getExpenses() { return expenses; }
    public LiveData<Map<String, Double>> getBalances() { return balances; }
    public LiveData<List<User>> getMembers() { return members; }
    public LiveData<String> getError() { return error; }

    public void init(String groupId) {
        if (groupId == null) return;

        // Listen to Group Details
        if (groupListener != null) groupListener.remove();
        groupListener = groupRepository.getGroupById(groupId, new GroupRepository.OnGroupLoadedListener() {
            @Override
            public void onLoaded(Group loadedGroup) {
                group.setValue(loadedGroup);
                loadMembers(loadedGroup.getMemberIds());
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });

        // Listen to Expenses
        if (expenseListener != null) expenseListener.remove();
        expenseListener = expenseRepository.getExpensesByGroup(groupId, new ExpenseRepository.OnExpensesLoadedListener() {
            @Override
            public void onLoaded(List<Expense> loadedExpenses) {
                expenses.setValue(loadedExpenses);
                calculateBalances(loadedExpenses, members.getValue());
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    private void loadMembers(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;

        List<User> loadedMembers = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        for (String uid : memberIds) {
            userRepository.getUserById(uid, new UserRepository.OnUserLoadedListener() {
                @Override
                public void onLoaded(User user) {
                    loadedMembers.add(user);
                    if (count.incrementAndGet() == memberIds.size()) {
                        members.setValue(loadedMembers);
                        calculateBalances(expenses.getValue(), loadedMembers);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (count.incrementAndGet() == memberIds.size()) {
                        members.setValue(loadedMembers);
                        calculateBalances(expenses.getValue(), loadedMembers);
                    }
                }
            });
        }
    }

    private void calculateBalances(List<Expense> expenseList, List<User> memberList) {
        if (memberList == null || memberList.isEmpty()) return;

        Map<String, Double> newBalances = new HashMap<>();
        for (User u : memberList) newBalances.put(u.getUid(), 0.0);

        if (expenseList != null) {
            for (Expense exp : expenseList) {
                if (!Expense.STATUS_COMPLETED.equals(exp.getStatus())) continue;

                String payer = exp.getPayerId();
                if (newBalances.containsKey(payer)) {
                    Double currentPayerBalance = newBalances.get(payer);
                    if (currentPayerBalance != null) {
                        newBalances.put(payer, currentPayerBalance + exp.getAmount());
                    }
                }

                Map<String, Object> split = exp.getSplitDetails();
                if (split != null) {
                    for (Map.Entry<String, Object> entry : split.entrySet()) {
                        if (newBalances.containsKey(entry.getKey())) {
                            try {
                                double shareValue = Double.parseDouble(entry.getValue().toString());
                                Double currentUserBalance = newBalances.get(entry.getKey());
                                if (currentUserBalance != null) {
                                    newBalances.put(entry.getKey(), currentUserBalance - shareValue);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
        balances.setValue(newBalances);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (groupListener != null) groupListener.remove();
        if (expenseListener != null) expenseListener.remove();
    }
}
