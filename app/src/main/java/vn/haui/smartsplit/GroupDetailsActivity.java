package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.haui.smartsplit.adapters.BalanceAdapter;
import vn.haui.smartsplit.adapters.GroupExpenseAdapter;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;

public class GroupDetailsActivity extends AppCompatActivity {

    private RecyclerView rvGroupContent;
    private TabLayout tabLayoutGroup;
    private FloatingActionButton fabAddGroupExpense;
    private String groupId, groupName;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        db = FirebaseFirestore.getInstance();
        groupId = getIntent().getStringExtra("GROUP_ID");
        groupName = getIntent().getStringExtra("GROUP_NAME");

        Toolbar toolbar = findViewById(R.id.toolbarGroupDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(groupName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvGroupContent = findViewById(R.id.rvGroupContent);
        tabLayoutGroup = findViewById(R.id.tabLayoutGroup);
        fabAddGroupExpense = findViewById(R.id.fabAddGroupExpense);

        rvGroupContent.setLayoutManager(new LinearLayoutManager(this));

        // Lấy thông tin nhóm để hiển thị mã tham gia
        fetchGroupDetails();

        showExpenses();

        tabLayoutGroup.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) showExpenses();
                else showBalances();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAddGroupExpense.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetailsActivity.this, AddGroupExpenseActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
        });
    }

    private void fetchGroupDetails() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null && getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle("Mã: " + group.getJoinCode());
                    }
                });
    }

    private void showExpenses() {
        // Tạm thời để dữ liệu mẫu, bạn có thể viết thêm logic lấy từ Firestore collection "expenses"
        List<Expense> expenses = new ArrayList<>();
        GroupExpenseAdapter adapter = new GroupExpenseAdapter(expenses);
        rvGroupContent.setAdapter(adapter);
    }

    private void showBalances() {
        List<User> members = new ArrayList<>();
        Map<String, Double> balances = new HashMap<>();
        BalanceAdapter adapter = new BalanceAdapter(members, balances);
        rvGroupContent.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
