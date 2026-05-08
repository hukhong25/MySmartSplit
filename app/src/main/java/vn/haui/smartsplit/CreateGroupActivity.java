package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName, etMemberEmail;
    private Button btnAddMember, btnCreateGroup;
    private RecyclerView rvAddedMembers;
    private List<String> memberList;
    // Giả sử có một adapter đơn giản để hiển thị email đã thêm
    // private MemberAdapter adapter; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        etGroupName = findViewById(R.id.etGroupName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        rvAddedMembers = findViewById(R.id.rvAddedMembers);

        memberList = new ArrayList<>();
        rvAddedMembers.setLayoutManager(new LinearLayoutManager(this));
        
        btnAddMember.setOnClickListener(v -> {
            String email = etMemberEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                memberList.add(email);
                etMemberEmail.setText("");
                Toast.makeText(this, "Đã thêm " + email, Toast.LENGTH_SHORT).show();
                // adapter.notifyDataSetChanged();
            }
        });

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            } else if (memberList.isEmpty()) {
                Toast.makeText(this, "Vui lòng thêm ít nhất một thành viên", Toast.LENGTH_SHORT).show();
            } else {
                // Logic lưu nhóm vào Firebase Firestore
                Toast.makeText(this, "Đang tạo nhóm: " + groupName, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
