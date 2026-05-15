package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Map;

import vn.haui.smartsplit.models.Expense;

public class SettleDetailActivity extends AppCompatActivity {

    private TextView tvDetailDescription, tvDetailAmount, tvDetailStatus;
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

        tvDetailDescription = findViewById(R.id.tvDetailDescription);
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

        db.collection("expenses").document(expenseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentExpense = documentSnapshot.toObject(Expense.class);
                    if (currentExpense != null) {
                        displayDetail();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void displayDetail() {
        tvDetailDescription.setText(currentExpense.getDescription());
        tvDetailAmount.setText(String.format(Locale.getDefault(), "%,.0f VND", currentExpense.getAmount()));
        
        String statusText = "Trạng thái: ";
        if (Expense.STATUS_PENDING.equals(currentExpense.getStatus())) {
            statusText += "Chờ xác nhận";
        } else if (Expense.STATUS_COMPLETED.equals(currentExpense.getStatus())) {
            statusText += "Đã hoàn thành";
        } else if (Expense.STATUS_REJECTED.equals(currentExpense.getStatus())) {
            statusText += "Đã bị từ chối";
        }
        tvDetailStatus.setText(statusText);

        if (currentExpense.getProofImageUrl() != null) {
            Glide.with(this).load(currentExpense.getProofImageUrl()).into(ivDetailProof);
        }

        // Check if current user is the receiver to show buttons
        String currentUid = mAuth.getUid();
        Map<String, Double> splitDetails = currentExpense.getSplitDetails();
        
        if (Expense.STATUS_PENDING.equals(currentExpense.getStatus()) && 
            splitDetails != null && splitDetails.containsKey(currentUid)) {
            layoutActionButtons.setVisibility(View.VISIBLE);
        } else {
            layoutActionButtons.setVisibility(View.GONE);
        }

        btnAccept.setOnClickListener(v -> updateStatus(Expense.STATUS_COMPLETED));
        btnReject.setOnClickListener(v -> updateStatus(Expense.STATUS_REJECTED));
    }

    private void updateStatus(String newStatus) {
        layoutActionButtons.setVisibility(View.GONE);
        db.collection("expenses").document(expenseId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    layoutActionButtons.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
