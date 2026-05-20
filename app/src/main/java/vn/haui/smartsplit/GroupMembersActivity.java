package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

public class GroupMembersActivity extends AppCompatActivity {

    private RecyclerView rvMembers;
    private FloatingActionButton fabAddMember;
    private FirebaseFirestore db;
    private String groupId, currentUserId;
    private Group currentGroup;
    private List<User> memberList = new ArrayList<>();
    private MemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_members);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        groupId = getIntent().getStringExtra("GROUP_ID");

        Toolbar toolbar = findViewById(R.id.toolbarMembers);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thành viên");
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
                        .setTitle("Mã tham gia nhóm")
                        .setMessage("Chia sẻ mã này cho bạn bè: " + currentGroup.getJoinCode())
                        .setPositiveButton("OK", null)
                        .show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Chỉ hiện nút giải tán nếu là admin
        if (currentGroup != null && currentUserId.equals(currentGroup.getAdminId())) {
            MenuItem item = menu.add(Menu.NONE, 101, Menu.NONE, "Giải tán nhóm");
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
            .setTitle("Giải tán nhóm")
            .setMessage("Bạn có chắc chắn muốn giải tán nhóm này? Nhóm sẽ không còn hiển thị với bất kỳ ai nhưng dữ liệu vẫn được lưu trữ an toàn.")
            .setPositiveButton("Giải tán", (dialog, which) -> dissolveGroup())
            .setNegativeButton("Hủy", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void dissolveGroup() {
        // Thay vì xóa Document, ta xóa danh sách thành viên để ẩn khỏi giao diện mọi người
        db.collection("groups").document(groupId)
            .update("memberIds", new ArrayList<String>())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Nhóm đã được giải tán thành công", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, HomeContainerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showAddMemberByEmailDialog() {
        final EditText etEmail = new EditText(this);
        etEmail.setHint("Nhập gmail người dùng");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etEmail.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
            .setTitle("Thêm thành viên")
            .setView(etEmail)
            .setPositiveButton("Thêm", (dialog, which) -> {
                String email = etEmail.getText().toString().trim();
                if (!email.isEmpty()) searchAndAddUser(email);
            })
            .setNegativeButton("Hủy", null)
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
                    Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void addUserToGroup(String uid, String email) {
        if (currentGroup.getMemberIds().contains(uid)) {
            Toast.makeText(this, "Người dùng đã ở trong nhóm", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("groups").document(groupId)
            .update("memberIds", FieldValue.arrayUnion(uid))
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã thêm " + email, Toast.LENGTH_SHORT).show());
    }

    private void loadGroupAndMembers() {
        db.collection("groups").document(groupId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                currentGroup = snapshot.toObject(Group.class);
                if (currentGroup != null) {
                    invalidateOptionsMenu(); // Cập nhật lại menu để hiện nút giải tán
                    fetchMemberDetails(currentGroup.getMemberIds());
                }
            }
        });
    }

    private void fetchMemberDetails(List<String> ids) {
        memberList.clear();
        if (ids == null || ids.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }
        for (String uid : ids) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                User u = doc.toObject(User.class);
                if (u != null) {
                    memberList.add(u);
                    if (memberList.size() == ids.size()) updateUI();
                }
            });
        }
    }

    private void updateUI() {
        adapter = new MemberAdapter(memberList, currentGroup.getAdminId(), currentUserId, user -> {
            if (currentUserId.equals(currentGroup.getAdminId())) {
                confirmRemove(user);
            } else {
                Toast.makeText(this, "Chỉ trưởng nhóm mới có quyền xóa", Toast.LENGTH_SHORT).show();
            }
        });
        rvMembers.setAdapter(adapter);
    }

    private void confirmRemove(User user) {
        new AlertDialog.Builder(this)
            .setTitle("Xóa thành viên")
            .setMessage("Xóa " + user.getName() + " khỏi nhóm?")
            .setPositiveButton("Xóa", (dialog, which) -> removeMember(user.getUid()))
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void removeMember(String uid) {
        db.collection("groups").document(groupId)
            .update("memberIds", FieldValue.arrayRemove(uid))
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
