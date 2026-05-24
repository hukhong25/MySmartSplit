package vn.haui.smartsplit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.adapters.MemberAdapter;
import vn.haui.smartsplit.models.User;
import vn.haui.smartsplit.viewmodels.CreateGroupViewModel;

public class CreateGroupActivity extends BaseActivity {

    private EditText etGroupName, etMemberEmail;
    private Button btnCreateGroup, btnAddMember;
    private RecyclerView rvAddedMembers;
    private ProgressBar progressBar;
    private MemberAdapter memberAdapter;
    private List<User> addedMembersList = new ArrayList<>();
    private CreateGroupViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        viewModel = new ViewModelProvider(this).get(CreateGroupViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.create_group_title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        etGroupName = findViewById(R.id.etGroupName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnAddMember = findViewById(R.id.btnAddMember);
        rvAddedMembers = findViewById(R.id.rvAddedMembers);
        progressBar = findViewById(R.id.progressBar);

        setupRecyclerView();
        observeViewModel();

        btnAddMember.setOnClickListener(v -> {
            String email = etMemberEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                viewModel.addMemberByEmail(email);
            } else {
                Toast.makeText(this, R.string.toast_missing_email, Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, R.string.toast_missing_group_name, Toast.LENGTH_SHORT).show();
            } else {
                viewModel.createGroup(groupName);
            }
        });
    }

    private void setupRecyclerView() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        memberAdapter = new MemberAdapter(addedMembersList, currentUserId, currentUserId, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onEditMember(User user) {
                Toast.makeText(CreateGroupActivity.this, R.string.toast_cannot_edit_role_on_create, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveMember(User user) {
                viewModel.removeMember(user);
            }
        });
        
        rvAddedMembers.setLayoutManager(new LinearLayoutManager(this));
        rvAddedMembers.setAdapter(memberAdapter);
    }

    private void observeViewModel() {
        viewModel.getAddedMembers().observe(this, users -> {
            addedMembersList.clear();
            addedMembersList.addAll(users);
            memberAdapter.notifyDataSetChanged();
            etMemberEmail.setText("");
        });

        viewModel.getCreateSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, R.string.toast_create_group_success, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnCreateGroup.setEnabled(!loading);
            btnAddMember.setEnabled(!loading);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
