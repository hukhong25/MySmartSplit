package vn.haui.smartsplit.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.smartsplit.LoginActivity;
import vn.haui.smartsplit.R;

public class ProfileFragment extends Fragment {

    private static final String PREF_DARK_MODE = "dark_mode_enabled";

    private FirebaseAuth mAuth;
    private SharedPreferences prefs;

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
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        TextView tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        TextView tvProfileName   = view.findViewById(R.id.tvProfileName);
        TextView tvProfileEmail  = view.findViewById(R.id.tvProfileEmail);
        SwitchMaterial switchDark = view.findViewById(R.id.switchDarkMode);
        MaterialButton btnLogout  = view.findViewById(R.id.btnLogout);
        View rowChangePassword    = view.findViewById(R.id.rowChangePassword);
        View rowEditProfile       = view.findViewById(R.id.rowEditProfile);

        // Populate user info
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();

            if (name != null && !name.isEmpty()) {
                tvProfileName.setText(name);
                tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                tvProfileName.setText("Người dùng");
                tvAvatarInitial.setText("U");
            }

            if (email != null) {
                tvProfileEmail.setText(email);
            }
        }

        // Dark mode switch – reflect current setting
        boolean isDark = prefs.getBoolean(PREF_DARK_MODE, false);
        switchDark.setChecked(isDark);
        switchDark.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Change Password
        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(v -> sendPasswordReset());
        }

        // Edit Profile (placeholder)
        if (rowEditProfile != null) {
            rowEditProfile.setOnClickListener(v -> Toast.makeText(requireContext(),
                    "Chức năng đang phát triển", Toast.LENGTH_SHORT).show());
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void sendPasswordReset() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        mAuth.sendPasswordResetEmail(user.getEmail())
                .addOnSuccessListener(unused -> Toast.makeText(requireContext(),
                        "Email đặt lại mật khẩu đã được gửi!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
