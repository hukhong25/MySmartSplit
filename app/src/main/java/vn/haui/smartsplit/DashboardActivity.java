package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import vn.haui.smartsplit.adapters.GroupAdapter;
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
        
        // TODO: Logic tính toán tổng nợ và tổng được trả sẽ được cập nhật sau dựa trên database
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
}
