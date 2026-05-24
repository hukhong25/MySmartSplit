package vn.haui.smartsplit.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.repositories.ExpenseRepository;
import vn.haui.smartsplit.repositories.NotificationRepository;

public class SettleDetailViewModel extends ViewModel {
    private final ExpenseRepository expenseRepository = new ExpenseRepository();
    private final NotificationRepository notificationRepository = new NotificationRepository();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final MutableLiveData<Expense> expense = new MutableLiveData<>();
    private final MutableLiveData<String> statusUpdateSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private ListenerRegistration expenseListener;

    public LiveData<Expense> getExpense() { return expense; }
    public LiveData<String> getStatusUpdateSuccess() { return statusUpdateSuccess; }
    public LiveData<String> getError() { return error; }

    public void loadExpense(String expenseId) {
        if (expenseId == null) return;
        if (expenseListener != null) expenseListener.remove();
        
        expenseListener = expenseRepository.observeExpenseById(expenseId, new ExpenseRepository.OnExpenseLoadedListener() {
            @Override
            public void onLoaded(Expense loadedExpense) {
                expense.setValue(loadedExpense);
            }

            @Override
            public void onError(Exception e) {
                error.setValue(e.getMessage());
            }
        });
    }

    public void updateStatus(String status, String acceptedMsg, String rejectedMsg, String notifTitleAccepted, String notifTitleRejected, String contentFormatAccepted, String contentFormatRejected, String defaultReceiverName) {
        Expense current = expense.getValue();
        if (current == null) return;

        expenseRepository.updateExpenseStatus(current.getId(), status)
                .addOnSuccessListener(aVoid -> {
                    sendResponseNotification(current, status, notifTitleAccepted, notifTitleRejected, contentFormatAccepted, contentFormatRejected, defaultReceiverName);
                    statusUpdateSuccess.setValue(status.equals(Expense.STATUS_COMPLETED) ? acceptedMsg : rejectedMsg);
                })
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }

    private void sendResponseNotification(Expense currentExpense, String status, String titleAccepted, String titleRejected, String contentFormatAccepted, String contentFormatRejected, String defaultReceiverName) {
        String senderId = currentExpense.getPayerId();
        String receiverName = mAuth.getCurrentUser().getDisplayName();
        if (receiverName == null || receiverName.isEmpty()) {
            receiverName = defaultReceiverName;
        }

        String title = status.equals(Expense.STATUS_COMPLETED) ? titleAccepted : titleRejected;
        String contentFormat = status.equals(Expense.STATUS_COMPLETED) ? contentFormatAccepted : contentFormatRejected;
        String content = String.format(contentFormat, receiverName, (long) currentExpense.getAmount());

        String notifId = notificationRepository.generateId();
        AppNotification notif = new AppNotification(
                notifId, senderId, title, content, "PAYMENT_RESPONSE", currentExpense.getId()
        );
        notificationRepository.sendNotification(notif);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (expenseListener != null) expenseListener.remove();
    }
}
