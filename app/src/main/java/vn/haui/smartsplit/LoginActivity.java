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

public class LoginActivity extends BaseActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        observeViewModel();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
            } else {
                viewModel.login(email, password);
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void observeViewModel() {
        viewModel.getFirebaseUser().observe(this, user -> {
            if (user != null) {
                Toast.makeText(LoginActivity.this, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, HomeContainerActivity.class));
                finish();
            }
        });

        viewModel.getError().observe(this, errMsg -> {
            if (errMsg != null) {
                Toast.makeText(LoginActivity.this, getString(R.string.toast_login_failed_prefix, errMsg), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!loading);
        });
    }
}
