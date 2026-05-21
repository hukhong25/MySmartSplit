package vn.haui.smartsplit.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

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
        View rowLanguage          = view.findViewById(R.id.rowLanguage);

        updateUI();

        // Ngôn ngữ
        if (rowLanguage != null) {
            rowLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        // Dark mode switch
        boolean isDark = prefs.getBoolean(PREF_DARK_MODE, false);
        switchDark.setChecked(isDark);
        switchDark.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Change Password
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

            tvProfileName.setText(name != null && !name.isEmpty() ? name : getString(R.string.default_username));
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
                                    tvAvatarInitial.setText(getString(R.string.default_profile_initial));
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
                            tvAvatarInitial.setText(getString(R.string.default_profile_initial));
                        }
                    });
        }
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_logout_title))
                .setMessage(getString(R.string.dialog_logout_message))
                .setPositiveButton(getString(R.string.dialog_action_logout), (dialog, which) -> logoutUser())
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLanguageDialog() {
        String[] languages = {
                getString(R.string.lang_vietnamese),
                getString(R.string.lang_english)
        };
        int currentLangIndex = prefs.getInt("selected_language_index", 0);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.dialog_language_title))
                .setSingleChoiceItems(languages, currentLangIndex, (dialog, which) -> {
                    prefs.edit().putInt("selected_language_index", which).apply();

                    if (which == 0) {
                        changeSystemLanguage("vi");
                    } else {
                        changeSystemLanguage("en");
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.dialog_language_cancel), null)
                .show();
    }

    private void changeSystemLanguage(String langCode) {
        prefs.edit().putString("app_lang", langCode).apply();

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources res = requireContext().getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        if (getActivity() != null) {
            getActivity().recreate();
        }
    }
}