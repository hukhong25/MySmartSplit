package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;

public class SettleUpActivity extends AppCompatActivity {

    private Spinner spFromUser, spToUser;
    private EditText etSettleAmount;
    private Button btnConfirmSettle;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String groupId;

    private final List<User> memberList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_up);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        groupId = getIntent().getStringExtra("GROUP_ID");

        spFromUser = findViewById(R.id.spFromUser);
        spToUser = findViewById(R.id.spToUser);
        etSettleAmount = findViewById(R.id.etSettleAmount);
        btnConfirmSettle = findViewById(R.id.btnConfirmSettle);

        loadGroupMembers();

        btnConfirmSettle.setOnClickListener(v -> confirmSettle());
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
                                    String name = userDoc.getString("name");
                                    String email = userDoc.getString("email");
                                    if (name == null || name.isEmpty()) {
                                        name = (email != null) ? email : uid;
                                    }
                                    memberList.add(new User(userDoc.getId(), name, email));

                                    if (fetchCount.incrementAndGet() == total) {
                                        updateSpinners();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (fetchCount.incrementAndGet() == total) {
                                        updateSpinners();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải thành viên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateSpinners() {
        List<String> names = new ArrayList<>();
        for (User u : memberList) {
            names.add(u.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spFromUser.setAdapter(adapter);
        spToUser.setAdapter(adapter);

        // Mặc định "Người trả" = user hiện tại
        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getUid().equals(currentUid)) {
                spFromUser.setSelection(i);
                // Người nhận mặc định = người đầu tiên khác
                spToUser.setSelection(i == 0 ? 1 : 0);
                break;
            }
        }
    }

    private void confirmSettle() {
        String amountStr = etSettleAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        int fromIndex = spFromUser.getSelectedItemPosition();
        int toIndex = spToUser.getSelectedItemPosition();

        if (fromIndex < 0 || toIndex < 0 || fromIndex >= memberList.size() || toIndex >= memberList.size()) {
            Toast.makeText(this, "Vui lòng chọn đúng thành viên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromIndex == toIndex) {
            Toast.makeText(this, "Người trả và người nhận không được giống nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        User fromUser = memberList.get(fromIndex);
        User toUser = memberList.get(toIndex);

        // Ghi settlement vào Firestore như một expense đặc biệt
        // fromUser trả tiền → toUser nhận → balance[fromUser] += amount, balance[toUser] -= amount
        Map<String, Double> splitDetails = new HashMap<>();
        splitDetails.put(toUser.getUid(), amount);

        String expenseId = db.collection("expenses").document().getId();
        String description = "Thanh toán: " + fromUser.getName() + " → " + toUser.getName();
        Expense settlement = new Expense(expenseId, description, amount,
                fromUser.getUid(), groupId, System.currentTimeMillis(), splitDetails);
        settlement.setPayerName(fromUser.getName());

        btnConfirmSettle.setEnabled(false);
        db.collection("expenses").document(expenseId)
                .set(settlement)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Xác nhận thanh toán thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmSettle.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
