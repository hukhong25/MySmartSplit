package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;

import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.models.Expense;

public class SettleDetailActivity extends BaseActivity {

    private TextView tvDetailDescription, tvDetailAmount, tvDetailStatus, tvDetailPayer;
    private ImageView ivDetailProof;
    private LinearLayout layoutActionButtons;
    private Button btnAccept, btnReject;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String expenseId;
    private Expense currentExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        expenseId = getIntent().getStringExtra("EXPENSE_ID");

        Toolbar toolbar = findViewById(R.id.toolbarSettleDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_transaction_detail));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailPayer = findViewById(R.id.tvDetailPayer);
        tvDetailAmount = findViewById(R.id.tvDetailAmount);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        ivDetailProof = findViewById(R.id.ivDetailProof);
        layoutActionButtons = findViewById(R.id.layoutActionButtons);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);

        loadExpenseDetail();
    }

    private void loadExpenseDetail() {
        if (expenseId == null) return;

        db.collection("expenses").document(expenseId).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null) return;
            currentExpense = doc.toObject(Expense.class);
            if (currentExpense != null) {
                displayDetail();
            }
        });
    }

    private void displayDetail() {
        tvDetailDescription.setText(currentExpense.getDescription());
        tvDetailPayer.setText(getString(R.string.detail_sender_format, currentExpense.getPayerName()));
        tvDetailAmount.setText(getString(R.string.currency_vnd_format, currentExpense.getAmount()));

        String status = currentExpense.getStatus();
        if (Expense.STATUS_PENDING.equals(status)) {
            tvDetailStatus.setText(getString(R.string.detail_status_pending));
            tvDetailStatus.setTextColor(android.graphics.Color.parseColor("#FFA500"));
        } else if (Expense.STATUS_COMPLETED.equals(status)) {
            tvDetailStatus.setText(getString(R.string.detail_status_settled));
            tvDetailStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            tvDetailStatus.setText(getString(R.string.detail_status_rejected));
            tvDetailStatus.setTextColor(android.graphics.Color.RED);
        }

        if (currentExpense.getProofImageUrl() != null && !currentExpense.getProofImageUrl().isEmpty()) {
            ivDetailProof.setVisibility(View.VISIBLE);
            vn.haui.smartsplit.utils.ImageUtils.loadImage(this, currentExpense.getProofImageUrl(), ivDetailProof, 0);
        } else {
            ivDetailProof.setVisibility(View.GONE);
        }

        String currentUid = mAuth.getUid();
        Map<String, Double> split = currentExpense.getSplitDetails();
        boolean isReceiver = split != null && split.containsKey(currentUid);

        if (Expense.STATUS_PENDING.equals(status) && isReceiver) {
            layoutActionButtons.setVisibility(View.VISIBLE);
        } else {
            layoutActionButtons.setVisibility(View.GONE);
        }

        btnAccept.setOnClickListener(v -> updateStatus(Expense.STATUS_COMPLETED));
        btnReject.setOnClickListener(v -> updateStatus(Expense.STATUS_REJECTED));
    }

    private void updateStatus(String newStatus) {
        db.collection("expenses").document(expenseId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    createResponseNotification(newStatus);
                    String msg = newStatus.equals(Expense.STATUS_COMPLETED)
                            ? getString(R.string.toast_payment_accepted)
                            : getString(R.string.toast_payment_rejected);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void createResponseNotification(String status) {
        if (currentExpense == null) return;
        String senderId = currentExpense.getPayerId();
        String receiverName = mAuth.getCurrentUser().getDisplayName();
        if (receiverName == null || receiverName.isEmpty()) {
            receiverName = getString(R.string.default_receiver_name);
        }

        String title = status.equals(Expense.STATUS_COMPLETED)
                ? getString(R.string.notif_title_payment_accepted)
                : getString(R.string.notif_title_payment_rejected);

        String contentFormat = status.equals(Expense.STATUS_COMPLETED)
                ? getString(R.string.notif_content_payment_accepted_format)
                : getString(R.string.notif_content_payment_rejected_format);

        String content = String.format(contentFormat, receiverName, (long) currentExpense.getAmount());

        String notifId = db.collection("notifications").document().getId();
        AppNotification notif = new AppNotification(
                notifId,
                senderId,
                title,
                content,
                "PAYMENT_RESPONSE",
                expenseId
        );
        db.collection("notifications").document(notifId).set(notif);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}