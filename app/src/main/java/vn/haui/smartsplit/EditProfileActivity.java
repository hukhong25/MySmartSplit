package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etDisplayName;
    private MaterialButton btnSave, btnCancel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etDisplayName = findViewById(R.id.etDisplayName);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etDisplayName.setText(user.getDisplayName());
        }

        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveProfile() {
        String newName = etDisplayName.getText().toString().trim();

        if (newName.isEmpty()) {
            etDisplayName.setError("Tên hiển thị không được để trống");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        // 1. Update Firebase Auth Profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 2. Update Firestore
                        updateFirestoreProfile(user.getUid(), newName);
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Lỗi cập nhật profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFirestoreProfile(String uid, String newName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi cập nhật dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
