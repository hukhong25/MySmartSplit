package vn.haui.smartsplit.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import vn.haui.smartsplit.ChangePasswordActivity;
import vn.haui.smartsplit.EditProfileActivity;
import vn.haui.smartsplit.LoginActivity;
import vn.haui.smartsplit.R;

public class ProfileFragment extends Fragment {

    private static final String PREF_DARK_MODE = "dark_mode_enabled";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences prefs;
    private TextView tvAvatarInitial, tvProfileName, tvProfileEmail;
    private ImageView ivAvatar;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateUI();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        tvProfileName   = view.findViewById(R.id.tvProfileName);
        tvProfileEmail  = view.findViewById(R.id.tvProfileEmail);
        ivAvatar        = view.findViewById(R.id.ivAvatarProfile); 

        SwitchMaterial switchDark = view.findViewById(R.id.switchDarkMode);
        MaterialButton btnLogout  = view.findViewById(R.id.btnLogout);
        View rowChangePassword    = view.findViewById(R.id.rowChangePassword);
        View rowEditProfile       = view.findViewById(R.id.rowEditProfile);

        updateUI();

        // Dark mode switch
        boolean isDark = prefs.getBoolean(PREF_DARK_MODE, false);
        switchDark.setChecked(isDark);
        switchDark.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Change Password - Navigate to ChangePasswordActivity
        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
                startActivity(intent);
            });
        }

        // Edit Profile
        if (rowEditProfile != null) {
            rowEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                editProfileLauncher.launch(intent);
            });
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void updateUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();

            tvProfileName.setText(name != null && !name.isEmpty() ? name : "Người dùng");
            tvProfileEmail.setText(email != null ? email : "");

            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded()) return;
                        String photoUrlStr = documentSnapshot.getString("photoUrl");
                        if (photoUrlStr != null && !photoUrlStr.isEmpty()) {
                            if (ivAvatar != null) {
                                ivAvatar.setVisibility(View.VISIBLE);
                                vn.haui.smartsplit.utils.ImageUtils.loadImage(requireContext(), photoUrlStr, ivAvatar, R.drawable.bg_avatar_circle);
                            }
                            tvAvatarInitial.setVisibility(View.GONE);
                        } else {
                            if (user.getPhotoUrl() != null) {
                                if (ivAvatar != null) {
                                    ivAvatar.setVisibility(View.VISIBLE);
                                    vn.haui.smartsplit.utils.ImageUtils.loadImage(requireContext(), user.getPhotoUrl().toString(), ivAvatar, R.drawable.bg_avatar_circle);
                                }
                                tvAvatarInitial.setVisibility(View.GONE);
                            } else {
                                if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
                                tvAvatarInitial.setVisibility(View.VISIBLE);
                                if (name != null && !name.isEmpty()) {
                                    tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                                } else {
                                    tvAvatarInitial.setText("U");
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
                        tvAvatarInitial.setVisibility(View.VISIBLE);
                        if (name != null && !name.isEmpty()) {
                            tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        } else {
                            tvAvatarInitial.setText("U");
                        }
                    });
        }
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logoutUser())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
