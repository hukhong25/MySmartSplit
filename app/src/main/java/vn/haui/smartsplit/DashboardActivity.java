package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvDashboardGroups;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvTotalOwe, tvTotalOwed, tvSeeAllGroups, tvWelcome;
    private Button btnMainLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotalOwe = findViewById(R.id.tvTotalOwe);
        tvTotalOwed = findViewById(R.id.tvTotalOwed);
        tvSeeAllGroups = findViewById(R.id.tvSeeAllGroups);
        btnMainLogout = findViewById(R.id.btnMainLogout);
        rvDashboardGroups = findViewById(R.id.rvDashboardGroups);

        // Hiển thị tên người dùng
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null) {
            tvWelcome.setText("Chào, " + user.getDisplayName() + "!");
        }

        rvDashboardGroups.setLayoutManager(new LinearLayoutManager(this));
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(DashboardActivity.this, GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvDashboardGroups.setAdapter(groupAdapter);

        tvSeeAllGroups.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, GroupsActivity.class));
        });

        // Xử lý nút Đăng xuất
        btnMainLogout.setOnClickListener(v -> logoutUser());

        ExtendedFloatingActionButton fabDashboardAddGroup = findViewById(R.id.fabDashboardAddGroup);
        fabDashboardAddGroup.setOnClickListener(v -> {
            showAddOptionsDialog();
        });

        loadDashboardData();
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() == null) return;
        
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Load 3 nhóm gần nhất
        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .limit(3)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    groupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Group group = doc.toObject(Group.class);
                            groupList.add(group);
                        }
                    }
                    groupAdapter.notifyDataSetChanged();
                });
        
        // Tính toán tổng nợ và tổng được trả thực tế
        db.collectionGroup("expenses")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    double totalOwe = 0;   // Bạn nợ người khác
                    double totalOwed = 0;  // Người khác nợ bạn

                    for (QueryDocumentSnapshot doc : value) {
                        String status = doc.getString("status");
                        // Chỉ tính các giao dịch đã hoàn thành hoặc mặc định là hoàn thành (cho các chi tiêu thường)
                        if (status != null && !status.equals(Expense.STATUS_COMPLETED)) continue;

                        String payerId = doc.getString("payerId");
                        Map<String, Object> splitDetails = (Map<String, Object>) doc.get("splitDetails");

                        if (splitDetails == null) continue;

                        if (currentUserId.equals(payerId)) {
                            // Bạn là người trả -> tính những người khác nợ bạn
                            for (Map.Entry<String, Object> entry : splitDetails.entrySet()) {
                                if (!entry.getKey().equals(currentUserId)) {
                                    totalOwed += Double.parseDouble(entry.getValue().toString());
                                }
                            }
                        } else if (splitDetails.containsKey(currentUserId)) {
                            // Người khác trả -> bạn nợ họ phần của bạn
                            totalOwe += Double.parseDouble(splitDetails.get(currentUserId).toString());
                        }
                    }

                    java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
                    tvTotalOwe.setText(formatter.format(totalOwe) + " VNĐ");
                    tvTotalOwed.setText(formatter.format(totalOwed) + " VNĐ");
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAddOptionsDialog() {
        String[] options = {"Tạo nhóm mới", "Tham gia bằng mã"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tùy chọn nhóm");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                startActivity(new Intent(DashboardActivity.this, CreateGroupActivity.class));
            } else {
                showJoinGroupDialog();
            }
        });
        builder.show();
    }

    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập mã tham gia");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint("Ví dụ: ABC123");
        builder.setView(input);

        builder.setPositiveButton("Tham gia", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!code.isEmpty()) {
                joinGroupWithCode(code);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void joinGroupWithCode(String code) {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("groups")
                .whereEqualTo("joinCode", code)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String groupId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("groups").document(groupId)
                                .update("memberIds", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã tham gia nhóm thành công!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi khi tham gia: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Mã tham gia không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
