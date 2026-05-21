package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import vn.haui.smartsplit.adapters.MemberAdapter;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;

public class GroupMembersActivity extends BaseActivity {

    private RecyclerView rvMembers;
    private FloatingActionButton fabAddMember;
    private FirebaseFirestore db;
    private String groupId, currentUserId;
    private Group currentGroup;
    private final List<User> memberList = new ArrayList<>();
    private MemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        groupId = getIntent().getStringExtra("GROUP_ID");

        Toolbar toolbar = findViewById(R.id.toolbarMembers);
        toolbar.getTop();
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_members));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvMembers = findViewById(R.id.rvMembers);
        fabAddMember = findViewById(R.id.fabAddMember);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        loadGroupAndMembers();

        fabAddMember.setOnClickListener(v -> {
            if (currentGroup != null) {
                if (currentUserId.equals(currentGroup.getAdminId())) {
                    showAddMemberByEmailDialog();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_join_code_title))
                            .setMessage(getString(R.string.dialog_share_join_code_msg_format, currentGroup.getJoinCode()))
                            .setPositiveButton(getString(R.string.dialog_action_ok), null)
                            .show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentGroup != null && currentUserId.equals(currentGroup.getAdminId())) {
            MenuItem item = menu.add(Menu.NONE, 101, Menu.NONE, getString(R.string.menu_dissolve_group));
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 101) {
            confirmDissolveGroup();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDissolveGroup() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_dissolve_group))
                .setMessage(getString(R.string.dialog_dissolve_group_msg))
                .setPositiveButton(getString(R.string.btn_dissolve), (dialog, which) -> dissolveGroup())
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void dissolveGroup() {
        db.collection("groups").document(groupId)
                .update("memberIds", new ArrayList<String>())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.toast_dissolve_group_success), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HomeContainerActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private void showAddMemberByEmailDialog() {
        final EditText etEmail = new EditText(this);
        etEmail.setHint(getString(R.string.hint_enter_gmail));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etEmail.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_add_member_title))
                .setView(etEmail)
                .setPositiveButton(getString(R.string.dialog_action_add), (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (!email.isEmpty()) searchAndAddUser(email);
                })
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void searchAndAddUser(String email) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            addUserToGroup(doc.getId(), email);
                            return;
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.toast_user_not_found), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addUserToGroup(String uid, String email) {
        if (currentGroup.getMemberIds().contains(uid)) {
            Toast.makeText(this, getString(R.string.toast_user_already_in_group), Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("groups").document(groupId)
                .update("memberIds", FieldValue.arrayUnion(uid))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.toast_added_member_success_format, email), Toast.LENGTH_SHORT).show());
    }

    private void loadGroupAndMembers() {
        db.collection("groups").document(groupId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                currentGroup = snapshot.toObject(Group.class);
                if (currentGroup != null) {
                    invalidateOptionsMenu();
                    fetchMemberDetails(currentGroup.getMemberIds());
                }
            }
        });
    }

    private void fetchMemberDetails(List<String> ids) {
        memberList.clear();
        if (ids == null || ids.isEmpty()) {
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }
        for (String uid : ids) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                User u = doc.toObject(User.class);
                if (u != null) {
                    u.setUid(doc.getId());
                    memberList.add(u);
                    if (memberList.size() == ids.size()) updateUI();
                }
            });
        }
    }

    private void updateUI() {
        adapter = new MemberAdapter(memberList, currentGroup.getAdminId(), currentUserId, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onEditMember(User user) {
                if (currentUserId.equals(currentGroup.getAdminId())) {
                    showEditMemberRoleDialog(user);
                } else {
                    Toast.makeText(GroupMembersActivity.this, getString(R.string.toast_permission_admin_edit_only), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onRemoveMember(User user) {
                if (currentUserId.equals(currentGroup.getAdminId())) {
                    confirmRemove(user);
                } else {
                    Toast.makeText(GroupMembersActivity.this, getString(R.string.toast_permission_admin_remove_only), Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvMembers.setAdapter(adapter);
    }

    private void showEditMemberRoleDialog(User user) {
        if (user.getUid().equals(currentGroup.getAdminId())) {
            Toast.makeText(this, getString(R.string.toast_user_already_admin), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_transfer_admin_title))
                .setMessage(getString(R.string.dialog_transfer_admin_msg_format, user.getName()))
                .setPositiveButton(getString(R.string.dialog_action_confirm_transfer), (dialog, which) -> transferAdminRole(user.getUid()))
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void transferAdminRole(String newAdminUid) {
        db.collection("groups").document(groupId)
                .update("adminId", newAdminUid)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, getString(R.string.toast_transfer_admin_success), Toast.LENGTH_SHORT).show();
                    invalidateOptionsMenu();
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private void confirmRemove(User user) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_remove_member_title))
                .setMessage(getString(R.string.dialog_remove_member_msg_format, user.getName()))
                .setPositiveButton(getString(R.string.btn_remove), (dialog, which) -> removeMember(user.getUid()))
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void removeMember(String uid) {
        db.collection("groups").document(groupId)
                .update("memberIds", FieldValue.arrayRemove(uid))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.toast_remove_member_success), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}