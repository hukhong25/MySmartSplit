package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.adapters.SplitMemberAdapter;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.viewmodels.AddGroupExpenseViewModel;

public class AddGroupExpenseActivity extends BaseActivity {

    private EditText etExpenseDescription, etExpenseAmount;
    private Spinner spPayer;
    private RecyclerView rvSplitMembers;
    private Button btnSaveGroupExpense;
    private ProgressBar pbLoading;

    private AddGroupExpenseViewModel viewModel;
    private String groupId;
    private String expenseId;
    private boolean isEditMode = false;

    private final List<User> memberList = new ArrayList<>();
    private final List<String> selectedUserIds = new ArrayList<>();
    private SplitMemberAdapter splitMemberAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_expense);

        groupId = getIntent().getStringExtra("GROUP_ID");
        expenseId = getIntent().getStringExtra("EXPENSE_ID");
        isEditMode = (expenseId != null);

        viewModel = new ViewModelProvider(this).get(AddGroupExpenseViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarAddExpense);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = isEditMode ? getString(R.string.title_edit_expense) : getString(R.string.title_add_expense);
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spPayer = findViewById(R.id.spPayer);
        rvSplitMembers = findViewById(R.id.rvSplitMembers);
        btnSaveGroupExpense = findViewById(R.id.btnSaveGroupExpense);
        pbLoading = findViewById(R.id.progressBar);

        if (isEditMode) {
            btnSaveGroupExpense.setText(getString(R.string.btn_update));
        }

        rvSplitMembers.setLayoutManager(new LinearLayoutManager(this));
        splitMemberAdapter = new SplitMemberAdapter(memberList, selectedUserIds);
        rvSplitMembers.setAdapter(splitMemberAdapter);

        observeViewModel();

        viewModel.loadGroupMembers(groupId);
        if (isEditMode) {
            viewModel.loadExpense(expenseId);
        }

        btnSaveGroupExpense.setOnClickListener(v -> saveExpense());
    }

    private void observeViewModel() {
        viewModel.getMemberList().observe(this, users -> {
            memberList.clear();
            memberList.addAll(users);
            updatePayerSpinner();
            if (!isEditMode && selectedUserIds.isEmpty()) {
                for (User u : users) selectedUserIds.add(u.getUid());
            }
            splitMemberAdapter.notifyDataSetChanged();
        });

        viewModel.getExpenseData().observe(this, expense -> {
            if (expense != null) {
                etExpenseDescription.setText(expense.getDescription());
                etExpenseAmount.setText(String.valueOf((long) expense.getAmount()));
                
                // Select Payer
                for (int i = 0; i < memberList.size(); i++) {
                    if (memberList.get(i).getUid().equals(expense.getPayerId())) {
                        spPayer.setSelection(i);
                        break;
                    }
                }

                // Select split members
                selectedUserIds.clear();
                if (expense.getSplitDetails() != null) {
                    selectedUserIds.addAll(expense.getSplitDetails().keySet());
                }
                splitMemberAdapter.notifyDataSetChanged();
            }
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                String msg = isEditMode ? getString(R.string.toast_update_expense_success) : getString(R.string.toast_add_expense_success);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, getString(R.string.toast_error_prefix, err), Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (pbLoading != null) pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSaveGroupExpense.setEnabled(!loading);
        });
    }

    private void updatePayerSpinner() {
        List<String> names = new ArrayList<>();
        for (User u : memberList) names.add(u.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPayer.setAdapter(adapter);

        if (!isEditMode) {
            String currentUid = FirebaseAuth.getInstance().getUid();
            for (int i = 0; i < memberList.size(); i++) {
                if (memberList.get(i).getUid().equals(currentUid)) {
                    spPayer.setSelection(i);
                    break;
                }
            }
        }
    }

    private void saveExpense() {
        String desc = etExpenseDescription.getText().toString().trim();
        String amountStr = etExpenseAmount.getText().toString().trim();

        if (desc.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedUserIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_split_members), Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        User payer = memberList.get(spPayer.getSelectedItemPosition());
        
        // Tạo ID mới nếu thêm mới, hoặc dùng lại ID cũ nếu sửa
        String id = isEditMode ? expenseId : FirebaseFirestore.getInstance().collection("expenses").document().getId();

        viewModel.saveExpense(id, desc, amount, payer, groupId, selectedUserIds, FirebaseAuth.getInstance().getUid());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
