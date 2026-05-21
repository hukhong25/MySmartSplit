package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import vn.haui.smartsplit.models.AppNotification;
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
    private String expenseId; // ID của khoản chi nếu ở chế độ sửa
    private boolean isEditMode = false;

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
        expenseId = getIntent().getStringExtra("EXPENSE_ID");
        isEditMode = (expenseId != null);

        Toolbar toolbar = findViewById(R.id.toolbarAddExpense);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Sửa khoản chi" : "Thêm khoản chi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spPayer = findViewById(R.id.spPayer);
        rvSplitMembers = findViewById(R.id.rvSplitMembers);
        btnSaveGroupExpense = findViewById(R.id.btnSaveGroupExpense);

        if (isEditMode) {
            btnSaveGroupExpense.setText("Cập nhật");
        }

        rvSplitMembers.setLayoutManager(new LinearLayoutManager(this));
        splitMemberAdapter = new SplitMemberAdapter(memberList, selectedUserIds);
        rvSplitMembers.setAdapter(splitMemberAdapter);

        loadGroupMembers();
    }

    private void loadGroupMembers() {
        if (groupId == null) return;

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> memberIds = (List<String>) documentSnapshot.get("memberIds");
                    if (memberIds == null || memberIds.isEmpty()) return;

                    AtomicInteger fetchCount = new AtomicInteger(0);
                    int total = memberIds.size();

                    for (String uid : memberIds) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    User u = userDoc.toObject(User.class);
                                    if (u != null) {
                                        u.setUid(userDoc.getId());
                                        memberList.add(u);
                                    }

                                    if (fetchCount.incrementAndGet() == total) {
                                        updatePayerSpinner();
                                        if (isEditMode) {
                                            loadExpenseData();
                                        } else {
                                            // Mặc định chọn tất cả nếu thêm mới
                                            selectedUserIds.addAll(memberIds);
                                            splitMemberAdapter.notifyDataSetChanged();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (fetchCount.incrementAndGet() == total) {
                                        updatePayerSpinner();
                                    }
                                });
                    }
                });
    }

    private void loadExpenseData() {
        db.collection("expenses").document(expenseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Expense expense = documentSnapshot.toObject(Expense.class);
                    if (expense != null) {
                        etExpenseDescription.setText(expense.getDescription());
                        etExpenseAmount.setText(String.valueOf((long)expense.getAmount()));
                        
                        // Chọn người trả
                        for (int i = 0; i < memberList.size(); i++) {
                            if (memberList.get(i).getUid().equals(expense.getPayerId())) {
                                spPayer.setSelection(i);
                                break;
                            }
                        }

                        // Chọn người chia tiền
                        selectedUserIds.clear();
                        if (expense.getSplitDetails() != null) {
                            selectedUserIds.addAll(expense.getSplitDetails().keySet());
                        }
                        splitMemberAdapter.notifyDataSetChanged();
                    }
                });
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

        if (!isEditMode) {
            String currentUid = mAuth.getUid();
            for (int i = 0; i < memberList.size(); i++) {
                if (memberList.get(i).getUid().equals(currentUid)) {
                    spPayer.setSelection(i);
                    break;
                }
            }
        }

        btnSaveGroupExpense.setOnClickListener(v -> saveExpense());
    }

    private void saveExpense() {
        String desc = etExpenseDescription.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim();

        if (desc.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một người chia", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        User payer = memberList.get(spPayer.getSelectedItemPosition());
        double share = amount / selectedUserIds.size();

        Map<String, Double> splitDetails = new HashMap<>();
        for (String uid : selectedUserIds) splitDetails.put(uid, share);

        String id = isEditMode ? expenseId : db.collection("expenses").document().getId();
        Expense expense = new Expense(id, desc, amount, payer.getUid(), groupId, System.currentTimeMillis(), splitDetails);
        expense.setPayerName(payer.getName());

        btnSaveGroupExpense.setEnabled(false);
        db.collection("expenses").document(id).set(expense)
                .addOnSuccessListener(aVoid -> {
                    notifyInvolvedMembers(expense, isEditMode);
                    Toast.makeText(this, isEditMode ? "Cập nhật khoản chi thành công" : "Thêm khoản chi thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveGroupExpense.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void notifyInvolvedMembers(Expense expense, boolean isUpdate) {
        String currentUid = mAuth.getUid();
        String title = isUpdate ? "Cập nhật chi tiêu" : "Khoản chi mới";
        String content = expense.getPayerName() + (isUpdate ? " đã cập nhật: " : " đã thêm: ") + expense.getDescription() + " (" + (long)expense.getAmount() + " VND)";

        for (String uid : expense.getSplitDetails().keySet()) {
            if (uid.equals(currentUid)) continue; // Don't notify self

            String notifId = db.collection("notifications").document().getId();
            AppNotification notif = new AppNotification(
                    notifId,
                    uid,
                    title,
                    content,
                    "EXPENSE_ADDED",
                    expense.getId()
            );
            db.collection("notifications").document(notifId).set(notif);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
