package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vn.haui.smartsplit.adapters.BalanceAdapter;
import vn.haui.smartsplit.adapters.GroupExpenseAdapter;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.User;

public class GroupDetailsActivity extends AppCompatActivity {

    private RecyclerView rvGroupContent;
    private TabLayout tabLayoutGroup;
    private FloatingActionButton fabAddGroupExpense;
    private String groupId, groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

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

        // Mặc định hiển thị tab Giao dịch
        showExpenses();

        tabLayoutGroup.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showExpenses();
                } else {
                    showBalances();
                }
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

    private void showExpenses() {
        // Dữ liệu mẫu hóa đơn nhóm
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("e1", "Ăn tối lẩu", 600000, "u1", groupId, System.currentTimeMillis(), new HashMap<>()));
        expenses.add(new Expense("e2", "Vé xem phim", 300000, "u2", groupId, System.currentTimeMillis(), new HashMap<>()));
        
        GroupExpenseAdapter adapter = new GroupExpenseAdapter(expenses);
        rvGroupContent.setAdapter(adapter);
    }

    private void showBalances() {
        // Dữ liệu mẫu bảng nợ
        List<User> members = new ArrayList<>();
        members.add(new User("u1", "Tôi", "me@example.com"));
        members.add(new User("u2", "Nguyễn Văn A", "a@example.com"));
        
        Map<String, Double> balances = new HashMap<>();
        balances.put("u1", 150000.0); // Tôi được trả 150k
        balances.put("u2", -150000.0); // A nợ 150k

        BalanceAdapter adapter = new BalanceAdapter(members, balances);
        rvGroupContent.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
