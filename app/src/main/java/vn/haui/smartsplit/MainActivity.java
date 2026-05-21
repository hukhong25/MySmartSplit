package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity {

    private static final String PREF_DARK_MODE = "dark_mode_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved dark mode preference before setContentView
        boolean isDark = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Splash: chuyển màn hình sau 1.8 giây
        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;
            if (currentUser != null) {
                intent = new Intent(MainActivity.this, HomeContainerActivity.class);
            } else {
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1800);
    }
}
