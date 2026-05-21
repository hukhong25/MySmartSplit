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
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(ivAvatar);
                tvAvatarInitial.setVisibility(View.GONE);
            } else {
                tvAvatarInitial.setVisibility(View.VISIBLE);
                String name = user.getDisplayName();
                if (name != null && !name.isEmpty()) {
                    tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }
            }
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
        final StorageReference ref = storage.getReference().child("avatars/" + user.getUid() + ".jpg");
        UploadTask uploadTask = ref.putFile(selectedImageUri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null) throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                updateProfile(user, newName, downloadUri);
            } else {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Lỗi tải ảnh: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateProfile(FirebaseUser user, String newName, Uri photoUri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .setPhotoUri(photoUri)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateFirestoreProfile(user.getUid(), newName, photoUri != null ? photoUri.toString() : null);
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
