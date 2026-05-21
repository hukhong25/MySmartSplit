package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vn.haui.smartsplit.adapters.MemberAdapter;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;

public class CreateGroupActivity extends BaseActivity {

    private EditText etGroupName, etMemberEmail;
    private Button btnCreateGroup, btnAddMember;
    private RecyclerView rvAddedMembers;
    private MemberAdapter memberAdapter;
    private List<User> addedMembers;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etGroupName = findViewById(R.id.etGroupName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        btnAddMember = findViewById(R.id.btnAddMember);
        rvAddedMembers = findViewById(R.id.rvAddedMembers);

        addedMembers = new ArrayList<>();
        
        String currentUserId = mAuth.getUid();


        // SỬA ĐOẠN DÒNG 53 THÀNH CẤU TRÚC NÀY:
        memberAdapter = new MemberAdapter(addedMembers, currentUserId, currentUserId, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onEditMember(User user) {
                // Màn hình Tạo nhóm mới thì thường không cần chỉnh sửa vai trò, bạn có thể để trống hoặc thông báo
                Toast.makeText(CreateGroupActivity.this, "Không thể sửa vai trò khi đang tạo nhóm", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveMember(User user) {
                // Logic xóa thành viên khỏi danh sách tạm chuẩn bị tạo nhóm (giữ nguyên logic cũ của bạn)
                addedMembers.remove(user);
                memberAdapter.notifyDataSetChanged();
            }
        });
        
        rvAddedMembers.setLayoutManager(new LinearLayoutManager(this));
        rvAddedMembers.setAdapter(memberAdapter);

        btnAddMember.setOnClickListener(v -> addMemberByEmail());

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            } else {
                createGroupInFirebase(groupName);
            }
        });
    }

    private void addMemberByEmail() {
        String email = etMemberEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() != null && email.equalsIgnoreCase(mAuth.getCurrentUser().getEmail())) {
            Toast.makeText(this, "Bạn đã là thành viên của nhóm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra trùng lặp trong danh sách tạm
        for (User u : addedMembers) {
            if (email.equalsIgnoreCase(u.getEmail())) {
                Toast.makeText(this, "Người dùng này đã có trong danh sách", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            addedMembers.add(user);
                            memberAdapter.notifyDataSetChanged();
                            etMemberEmail.setText("");
                            Toast.makeText(this, "Đã thêm " + user.getName(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy người dùng với email này", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createGroupInFirebase(String groupName) {
        if (mAuth.getCurrentUser() == null) return;
        
        String currentUserId = mAuth.getCurrentUser().getUid();
        String groupId = db.collection("groups").document().getId();
        String joinCode = generateJoinCode();

        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUserId); // Thêm trưởng nhóm
        for (User u : addedMembers) {
            memberIds.add(u.getUid()); // Thêm các thành viên đã chọn
        }

        Group newGroup = new Group(groupId, groupName, memberIds, currentUserId, joinCode);

        btnCreateGroup.setEnabled(false);
        db.collection("groups").document(groupId)
                .set(newGroup)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo nhóm thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateGroup.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 6) {
            int index = (int) (rnd.nextFloat() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}
