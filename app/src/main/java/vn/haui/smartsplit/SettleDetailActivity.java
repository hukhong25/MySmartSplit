package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.viewmodels.SettleDetailViewModel;

public class SettleDetailActivity extends BaseActivity {

    private TextView tvDetailDescription, tvDetailAmount, tvDetailStatus, tvDetailPayer;
    private ImageView ivDetailProof;
    private LinearLayout layoutActionButtons;
    private Button btnAccept, btnReject;

    private SettleDetailViewModel viewModel;
    private String expenseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_detail);

        expenseId = getIntent().getStringExtra("EXPENSE_ID");
        viewModel = new ViewModelProvider(this).get(SettleDetailViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarSettleDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_transaction_detail));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailPayer = findViewById(R.id.tvDetailPayer);
        tvDetailAmount = findViewById(R.id.tvDetailAmount);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        ivDetailProof = findViewById(R.id.ivDetailProof);
        layoutActionButtons = findViewById(R.id.layoutActionButtons);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);

        observeViewModel();
        viewModel.loadExpense(expenseId);
    }

    private void observeViewModel() {
        viewModel.getExpense().observe(this, expense -> {
            if (expense != null) {
                displayDetail(expense);
            }
        });

        viewModel.getStatusUpdateSuccess().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, getString(R.string.toast_error_prefix, err), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayDetail(Expense expense) {
        tvDetailDescription.setText(expense.getDescription());
        tvDetailPayer.setText(getString(R.string.detail_sender_format, expense.getPayerName()));
        tvDetailAmount.setText(getString(R.string.currency_vnd_format, expense.getAmount()));

        String status = expense.getStatus();
        if (Expense.STATUS_PENDING.equals(status)) {
            tvDetailStatus.setText(getString(R.string.detail_status_pending));
            tvDetailStatus.setTextColor(android.graphics.Color.parseColor("#FFA500"));
        } else if (Expense.STATUS_COMPLETED.equals(status)) {
            tvDetailStatus.setText(getString(R.string.detail_status_settled));
            tvDetailStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            tvDetailStatus.setText(getString(R.string.detail_status_rejected));
            tvDetailStatus.setTextColor(android.graphics.Color.RED);
        }

        if (expense.getProofImageUrl() != null && !expense.getProofImageUrl().isEmpty()) {
            ivDetailProof.setVisibility(View.VISIBLE);
            vn.haui.smartsplit.utils.ImageUtils.loadImage(this, expense.getProofImageUrl(), ivDetailProof, 0);
        } else {
            ivDetailProof.setVisibility(View.GONE);
        }

        String currentUid = FirebaseAuth.getInstance().getUid();
        
        // FIXED: Change from Map<String, Double> to Map<String, Object> 
        // to match the refactored Expense model.
        Map<String, Object> split = expense.getSplitDetails();
        boolean isReceiver = split != null && split.containsKey(currentUid);

        if (Expense.STATUS_PENDING.equals(status) && isReceiver) {
            layoutActionButtons.setVisibility(View.VISIBLE);
        } else {
            layoutActionButtons.setVisibility(View.GONE);
        }

        btnAccept.setOnClickListener(v -> viewModel.updateStatus(
                Expense.STATUS_COMPLETED,
                getString(R.string.toast_payment_accepted),
                getString(R.string.toast_payment_rejected),
                getString(R.string.notif_title_payment_accepted),
                getString(R.string.notif_title_payment_rejected),
                getString(R.string.notif_content_payment_accepted_format),
                getString(R.string.notif_content_payment_rejected_format),
                getString(R.string.default_receiver_name)
        ));
        
        btnReject.setOnClickListener(v -> viewModel.updateStatus(
                Expense.STATUS_REJECTED,
                getString(R.string.toast_payment_accepted),
                getString(R.string.toast_payment_rejected),
                getString(R.string.notif_title_payment_accepted),
                getString(R.string.notif_title_payment_rejected),
                getString(R.string.notif_content_payment_accepted_format),
                getString(R.string.notif_content_payment_rejected_format),
                getString(R.string.default_receiver_name)
        ));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
