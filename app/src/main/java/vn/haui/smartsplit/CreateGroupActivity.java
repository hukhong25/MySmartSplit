package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vn.haui.smartsplit.models.Group;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName;
    private Button btnCreateGroup;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etGroupName = findViewById(R.id.etGroupName);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            } else {
                createGroupInFirebase(groupName);
            }
        });
    }

    private void createGroupInFirebase(String groupName) {
        String currentUserId = mAuth.getCurrentUser().getUid();
        String groupId = db.collection("groups").document().getId();
        String joinCode = generateJoinCode();

        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUserId);

        Group newGroup = new Group(groupId, groupName, memberIds, currentUserId, joinCode);

        btnCreateGroup.setEnabled(false);
        db.collection("groups").document(groupId)
                .set(newGroup)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo nhóm thành công! Mã tham gia: " + joinCode, Toast.LENGTH_LONG).show();
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
