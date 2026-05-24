package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import vn.haui.smartsplit.viewmodels.AuthViewModel;

public class RegisterActivity extends BaseActivity {

    private EditText etDisplayName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        observeViewModel();

        btnRegister.setOnClickListener(v -> {
            String name = etDisplayName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, getString(R.string.error_password_too_short), Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.register(email, password, name);
        });

        tvLogin.setOnClickListener(v -> finish());
    }

    private void observeViewModel() {
        viewModel.getFirebaseUser().observe(this, user -> {
            if (user != null) {
                Toast.makeText(RegisterActivity.this, getString(R.string.toast_register_success), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, HomeContainerActivity.class));
                finishAffinity();
            }
        });

        viewModel.getError().observe(this, errMsg -> {
            if (errMsg != null) {
                Toast.makeText(RegisterActivity.this, getString(R.string.toast_register_failed_prefix, errMsg), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnRegister.setEnabled(!loading);
        });
    }
}
