package vn.haui.smartsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
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

import vn.haui.smartsplit.models.AppNotification;
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
                    Glide.with(this).load(imageUri).into(ivProofPreview);
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

        Toolbar toolbar = findViewById(R.id.toolbarSettleUp);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thanh toán");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
        db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
            List<String> memberIds = (List<String>) doc.get("memberIds");
            if (memberIds == null) return;
            AtomicInteger count = new AtomicInteger(0);
            memberList.clear();
            for (String uid : memberIds) {
                db.collection("users").document(uid).get().addOnSuccessListener(uDoc -> {
                    User u = uDoc.toObject(User.class);
                    if (u != null) { u.setUid(uDoc.getId()); memberList.add(u); }
                    if (count.incrementAndGet() == memberIds.size()) {
                        updateSpinners();
                        handleSuggestions();
                    }
                });
            }
        });
    }

    private void handleSuggestions() {
        String targetUserId = getIntent().getStringExtra("TARGET_USER_ID");
        double amount = getIntent().getDoubleExtra("AMOUNT", 0);
        if (targetUserId != null) {
            for (int i = 0; i < memberList.size(); i++) {
                if (memberList.get(i).getUid().equals(targetUserId)) {
                    spToUser.setSelection(i);
                    break;
                }
            }
        }
        if (amount > 0) etSettleAmount.setText(String.valueOf((long)Math.abs(amount)));
    }

    private void updateSpinners() {
        List<String> names = new ArrayList<>();
        for (User u : memberList) names.add(u.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFromUser.setAdapter(adapter);
        spToUser.setAdapter(adapter);

        String currentUid = mAuth.getUid();
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
        try { amount = Double.parseDouble(amountStr); } catch (Exception e) { return; }

        User from = memberList.get(spFromUser.getSelectedItemPosition());
        User to = memberList.get(spToUser.getSelectedItemPosition());

        if (from.getUid().equals(to.getUid())) {
            Toast.makeText(this, "Người gửi và nhận không được trùng nhau", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmSettle.setEnabled(false);
        if (imageUri != null) {
            String fileName = "proofs/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);
            ref.putFile(imageUri).addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(uri -> 
                saveSettlement(from, to, amount, uri.toString())
            )).addOnFailureListener(e -> {
                btnConfirmSettle.setEnabled(true);
                Toast.makeText(this, "Lỗi tải ảnh", Toast.LENGTH_SHORT).show();
            });
        } else {
            saveSettlement(from, to, amount, null);
        }
    }

    private void saveSettlement(User from, User to, double amount, String imageUrl) {
        Map<String, Double> split = new HashMap<>();
        split.put(to.getUid(), amount);

        String id = db.collection("expenses").document().getId();
        Expense settlement = new Expense();
        settlement.setId(id);
        settlement.setDescription("Thanh toán: " + from.getName() + " → " + to.getName());
        settlement.setAmount(amount);
        settlement.setPayerId(from.getUid());
        settlement.setPayerName(from.getName());
        settlement.setGroupId(groupId);
        settlement.setTimestamp(System.currentTimeMillis());
        settlement.setSplitDetails(split);
        settlement.setProofImageUrl(imageUrl);
        settlement.setSettlement(true);
        settlement.setStatus(Expense.STATUS_PENDING);

        db.collection("expenses").document(id).set(settlement).addOnSuccessListener(aVoid -> {
            sendNotification(to.getUid(), from.getName(), amount, id);
            Toast.makeText(this, "Đã gửi yêu cầu thanh toán, chờ xác nhận!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            btnConfirmSettle.setEnabled(true);
            Toast.makeText(this, "Lỗi lưu dữ liệu", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendNotification(String receiverId, String senderName, double amount, String expenseId) {
        String notifId = db.collection("notifications").document().getId();
        AppNotification notif = new AppNotification(
                notifId,
                receiverId,
                "Yêu cầu thanh toán mới",
                senderName + " đã gửi xác nhận thanh toán " + (long)amount + " VND. Vui lòng xác nhận.",
                "PAYMENT_REQUEST",
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
