package vn.haui.smartsplit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.viewmodels.SettleUpViewModel;

public class SettleUpActivity extends BaseActivity {

    private Spinner spFromUser, spToUser;
    private EditText etSettleAmount;
    private Button btnConfirmSettle, btnPickImage;
    private ImageView ivProofPreview;
    private View progressBar;

    private SettleUpViewModel viewModel;
    private String groupId;
    private Uri imageUri;

    private final List<User> memberList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivProofPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(imageUri).into(ivProofPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_up);

        groupId = getIntent().getStringExtra("GROUP_ID");
        viewModel = new ViewModelProvider(this).get(SettleUpViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarSettleUp);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_settle_up));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spFromUser = findViewById(R.id.spFromUser);
        spToUser = findViewById(R.id.spToUser);
        etSettleAmount = findViewById(R.id.etSettleAmount);
        btnConfirmSettle = findViewById(R.id.btnConfirmSettle);
        btnPickImage = findViewById(R.id.btnPickImage);
        ivProofPreview = findViewById(R.id.ivProofPreview);
        progressBar = findViewById(R.id.progressBar);

        observeViewModel();

        viewModel.loadMembers(groupId);

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnConfirmSettle.setOnClickListener(v -> confirmSettle());
    }

    private void observeViewModel() {
        viewModel.getMemberList().observe(this, users -> {
            memberList.clear();
            memberList.addAll(users);
            updateSpinners();
            handleSuggestions();
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, getString(R.string.toast_settle_request_sent), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, getString(R.string.toast_error_prefix, err), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnConfirmSettle.setEnabled(!loading);
        });
    }

    private void handleSuggestions() {
        String targetUserId = getIntent().getStringExtra("TARGET_USER_ID");
        double amount = getIntent().getDoubleExtra("AMOUNT", 0);
        if (targetUserId != null) {
            for (int i = 0; i < memberList.size(); i++) {
                if (memberList.get(i).getUid().equals(targetUserId)) {
                    spToUser.setSelection(i);
                    break;
                }
            }
        }
        if (amount > 0) etSettleAmount.setText(String.valueOf((long)Math.abs(amount)));
    }

    private void updateSpinners() {
        List<String> names = new ArrayList<>();
        for (User u : memberList) names.add(u.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFromUser.setAdapter(adapter);
        spToUser.setAdapter(adapter);

        String currentUid = FirebaseAuth.getInstance().getUid();
        for (int i = 0; i < memberList.size(); i++) {
            if (memberList.get(i).getUid().equals(currentUid)) {
                spFromUser.setSelection(i);
                spToUser.setSelection(i == 0 ? 1 : 0);
                break;
            }
        }
    }

    private void confirmSettle() {
        String amountStr = etSettleAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (Exception e) { return; }

        User from = memberList.get(spFromUser.getSelectedItemPosition());
        User to = memberList.get(spToUser.getSelectedItemPosition());

        if (from.getUid().equals(to.getUid())) {
            Toast.makeText(this, getString(R.string.toast_settle_users_duplicate), Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Image = null;
        if (imageUri != null) {
            base64Image = vn.haui.smartsplit.utils.ImageUtils.convertUriToBase64(getContentResolver(), imageUri, 600);
            if (base64Image == null) {
                Toast.makeText(this, getString(R.string.toast_image_processing_error), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        viewModel.confirmSettle(
                from, to, amount, base64Image, groupId,
                getString(R.string.settle_description_format),
                getString(R.string.notif_title_new_payment_request),
                getString(R.string.notif_content_payment_request_format)
        );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
