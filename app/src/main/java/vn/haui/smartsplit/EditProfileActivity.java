package vn.haui.smartsplit;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etDisplayName;
    private ImageView ivAvatar;
    private TextView tvAvatarInitial;
    private MaterialButton btnSave, btnCancel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivAvatar.setImageURI(uri);
                    tvAvatarInitial.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        ivAvatar = findViewById(R.id.ivAvatar);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        View frameAvatar = findViewById(R.id.frameAvatar);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etDisplayName.setText(user.getDisplayName());
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String photoUrlStr = documentSnapshot.getString("photoUrl");
                        if (photoUrlStr != null && !photoUrlStr.isEmpty()) {
                            vn.haui.smartsplit.utils.ImageUtils.loadImage(this, photoUrlStr, ivAvatar, 0);
                            tvAvatarInitial.setVisibility(View.GONE);
                        } else if (user.getPhotoUrl() != null) {
                            vn.haui.smartsplit.utils.ImageUtils.loadImage(this, user.getPhotoUrl().toString(), ivAvatar, 0);
                            tvAvatarInitial.setVisibility(View.GONE);
                        } else {
                            tvAvatarInitial.setVisibility(View.VISIBLE);
                            String name = user.getDisplayName();
                            if (name != null && !name.isEmpty()) {
                                tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                            }
                        }
                    });
        }

        frameAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
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
        if (user == null) return;

        btnSave.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(user, newName);
        } else {
            updateProfile(user, newName, user.getPhotoUrl());
        }
    }

    private void uploadImageAndSaveProfile(FirebaseUser user, String newName) {
        // Convert selected image to compressed Base64 string (max 300px for avatar)
        String base64Image = vn.haui.smartsplit.utils.ImageUtils.convertUriToBase64(getContentResolver(), selectedImageUri, 300);
        if (base64Image == null) {
            btnSave.setEnabled(true);
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateFirestoreProfile(user.getUid(), newName, base64Image);
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Lỗi cập nhật profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProfile(FirebaseUser user, String newName, Uri photoUri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateFirestoreProfile(user.getUid(), newName, null);
                    } else {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Lỗi cập nhật profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFirestoreProfile(String uid, String newName, String photoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        if (photoUrl != null) {
            updates.put("photoUrl", photoUrl);
        }

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu dữ liệu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
