package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import vn.haui.smartsplit.viewmodels.ChangePasswordViewModel;

public class ChangePasswordActivity extends BaseActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnChangePassword, btnCancel;
    private ProgressBar progressBar;
    private ChangePasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        viewModel = new ViewModelProvider(this).get(ChangePasswordViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        observeViewModel();

        btnChangePassword.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmNewPassword.getText().toString().trim();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmNewPassword.setError(getString(R.string.error_password_mismatch));
                return;
            }

            if (newPass.length() < 6) {
                etNewPassword.setError(getString(R.string.error_password_too_short));
                return;
            }

            viewModel.changePassword(currentPass, newPass);
        });

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void observeViewModel() {
        viewModel.getSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, getString(R.string.toast_change_password_success), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                if (err.equals("REAUTH_FAILED")) {
                    etCurrentPassword.setError(getString(R.string.error_current_password_incorrect));
                    Toast.makeText(this, getString(R.string.toast_reauth_failed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.toast_error_prefix, err), Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnChangePassword.setEnabled(!loading);
        });
    }
}
