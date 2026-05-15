package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.adapters.SplitMemberAdapter;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;

public class AddGroupExpenseActivity extends AppCompatActivity {

    private EditText etExpenseDescription, etExpenseAmount;
    private Spinner spPayer;
    private RecyclerView rvSplitMembers;
    private Button btnSaveGroupExpense;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String groupId;

    private final List<User> memberList = new ArrayList<>();
    private final List<String> selectedUserIds = new ArrayList<>();
    private SplitMemberAdapter splitMemberAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_expense);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        groupId = getIntent().getStringExtra("GROUP_ID");

        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spPayer = findViewById(R.id.spPayer);
        rvSplitMembers = findViewById(R.id.rvSplitMembers);
        btnSaveGroupExpense = findViewById(R.id.btnSaveGroupExpense);

        rvSplitMembers.setLayoutManager(new LinearLayoutManager(this));
        splitMemberAdapter = new SplitMemberAdapter(memberList, selectedUserIds);
        rvSplitMembers.setAdapter(splitMemberAdapter);

        loadGroupMembers();

        btnSaveGroupExpense.setOnClickListener(v -> saveExpense());
    }

    private void loadGroupMembers() {
        if (groupId == null) return;

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                    if (memberIds == null || memberIds.isEmpty()) return;

                    // Pre-select tất cả thành viên
                    selectedUserIds.clear();
                    selectedUserIds.addAll(memberIds);

                    AtomicInteger fetchCount = new AtomicInteger(0);
                    int total = memberIds.size();

                    for (String uid : memberIds) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    String email = userDoc.getString("email");
                                    if (name == null || name.isEmpty()) {
                                        name = (email != null) ? email : uid;
                                    }
                                    memberList.add(new User(userDoc.getId(), name, email));

                                    if (fetchCount.incrementAndGet() == total) {
                                        updatePayerSpinner();
                                        splitMemberAdapter.notifyDataSetChanged();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (fetchCount.incrementAndGet() == total) {
                                        updatePayerSpinner();
                                        splitMemberAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thành viên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updatePayerSpinner() {
        List<String> names = new ArrayList<>();
        for (User u : memberList) {
            names.add(u.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPayer.setAdapter(adapter);

        // Mặc định chọn user hiện tại
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getUid().equals(currentUid)) {
                spPayer.setSelection(i);
                break;
            }
        }
    }

    private void saveExpense() {
        String desc = etExpenseDescription.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim();

        if (desc.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một người để chia tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        int payerIndex = spPayer.getSelectedItemPosition();
        if (payerIndex < 0 || payerIndex >= memberList.size()) {
            Toast.makeText(this, "Vui lòng chọn người trả tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        User payer = memberList.get(payerIndex);
        double sharePerPerson = amount / selectedUserIds.size();

        Map<String, Double> splitDetails = new HashMap<>();
        for (String uid : selectedUserIds) {
            splitDetails.put(uid, sharePerPerson);
        }

        String expenseId = db.collection("expenses").document().getId();
        Expense expense = new Expense(expenseId, desc, amount, payer.getUid(),
                groupId, System.currentTimeMillis(), splitDetails);
        expense.setPayerName(payer.getName());

        btnSaveGroupExpense.setEnabled(false);
        db.collection("expenses").document(expenseId)
                .set(expense)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã thêm chi tiêu nhóm thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveGroupExpense.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
