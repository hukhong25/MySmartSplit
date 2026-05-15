package vn.haui.smartsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;

public class SettleUpActivity extends AppCompatActivity {

    private Spinner spFromUser, spToUser;
    private EditText etSettleAmount;
    private Button btnConfirmSettle, btnPickImage;
    private ImageView ivProofPreview;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private String groupId;
    private Uri imageUri;

    private final List<User> memberList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProofPreview.setVisibility(View.VISIBLE);
                    ivProofPreview.setImageURI(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_up);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        groupId = getIntent().getStringExtra("GROUP_ID");

        spFromUser = findViewById(R.id.spFromUser);
        spToUser = findViewById(R.id.spToUser);
        etSettleAmount = findViewById(R.id.etSettleAmount);
        btnConfirmSettle = findViewById(R.id.btnConfirmSettle);
        btnPickImage = findViewById(R.id.btnPickImage);
        ivProofPreview = findViewById(R.id.ivProofPreview);

        loadGroupMembers();

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

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

        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getUid().equals(currentUid)) {
                spFromUser.setSelection(i);
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

        if (fromIndex < 0 || toIndex < 0) return;
        if (fromIndex == toIndex) {
            Toast.makeText(this, "Người trả và người nhận không được giống nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        User fromUser = memberList.get(fromIndex);
        User toUser = memberList.get(toIndex);

        btnConfirmSettle.setEnabled(false);

        if (imageUri != null) {
            uploadImageAndSave(fromUser, toUser, amount);
        } else {
            saveSettlement(fromUser, toUser, amount, null);
        }
    }

    private void uploadImageAndSave(User fromUser, User toUser, double amount) {
        String fileName = "proofs/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveSettlement(fromUser, toUser, amount, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    btnConfirmSettle.setEnabled(true);
                    Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveSettlement(User fromUser, User toUser, double amount, String imageUrl) {
        Map<String, Double> splitDetails = new HashMap<>();
        splitDetails.put(toUser.getUid(), amount);

        String expenseId = db.collection("expenses").document().getId();
        String description = "Thanh toán: " + fromUser.getName() + " → " + toUser.getName();
        
        Expense settlement = new Expense();
        settlement.setId(expenseId);
        settlement.setDescription(description);
        settlement.setAmount(amount);
        settlement.setPayerId(fromUser.getName().equals(mAuth.getCurrentUser().getDisplayName()) ? mAuth.getUid() : fromUser.getUid()); // Simplified
        // Correction: payerId should be the UID of fromUser
        settlement.setPayerId(fromUser.getUid());
        settlement.setPayerName(fromUser.getName());
        settlement.setGroupId(groupId);
        settlement.setTimestamp(System.currentTimeMillis());
        settlement.setSplitDetails(splitDetails);
        settlement.setProofImageUrl(imageUrl);
        settlement.setSettlement(true);
        
        // If there's an image, it needs confirmation
        settlement.setStatus(imageUrl != null ? Expense.STATUS_PENDING : Expense.STATUS_COMPLETED);

        db.collection("expenses").document(expenseId)
                .set(settlement)
                .addOnSuccessListener(aVoid -> {
                    String msg = imageUrl != null ? "Yêu cầu thanh toán đã được gửi, chờ xác nhận!" : "Thanh toán thành công!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmSettle.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
