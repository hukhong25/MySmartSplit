package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import vn.haui.smartsplit.adapters.BalanceAdapter;
import vn.haui.smartsplit.adapters.GroupExpenseAdapter;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;

public class GroupDetailsActivity extends AppCompatActivity implements BalanceAdapter.OnActionClickListener {

    private RecyclerView rvGroupContent;
    private TabLayout tabLayoutGroup;
    private FloatingActionButton fabAddGroupExpense;
    private String groupId, groupName;
    private String joinCode;
    private FirebaseFirestore db;
    private ListenerRegistration expenseListener;

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

        fetchGroupDetails();
        showExpenses();

        fabAddGroupExpense.setOnClickListener(v -> openAddExpense());

        tabLayoutGroup.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showExpenses();
                    fabAddGroupExpense.setImageResource(android.R.drawable.ic_input_add);
                    fabAddGroupExpense.setOnClickListener(v -> openAddExpense());
                } else {
                    showBalances();
                    fabAddGroupExpense.setImageResource(android.R.drawable.ic_menu_send);
                    fabAddGroupExpense.setOnClickListener(v -> openSettleUp());
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_invite) {
            shareInviteCode();
            return true;
        } else if (id == R.id.action_manage_members) {
            Intent intent = new Intent(this, GroupMembersActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareInviteCode() {
        if (joinCode == null) {
            Toast.makeText(this, "Đang tải mã mời...", Toast.LENGTH_SHORT).show();
            return;
        }
        String message = "Tham gia nhóm '" + groupName + "' trên SmartSplit bằng mã: " + joinCode;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, "Mời thành viên qua"));
    }

    private void openAddExpense() {
        Intent intent = new Intent(this, AddGroupExpenseActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
    }

    private void openSettleUp() {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
    }

    private void fetchGroupDetails() {
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null) {
                        joinCode = group.getJoinCode();
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle("Mã: " + joinCode);
                        }
                    }
                });
    }

    private void showExpenses() {
        removeExpenseListener();
        List<Expense> expenses = new ArrayList<>();
        GroupExpenseAdapter adapter = new GroupExpenseAdapter(expenses, this::handleExpenseClick);
        rvGroupContent.setAdapter(adapter);

        expenseListener = db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    expenses.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            expenses.add(doc.toObject(Expense.class));
                        }
                        expenses.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void handleExpenseClick(Expense expense) {
        Intent intent = new Intent(this, SettleDetailActivity.class);
        intent.putExtra("EXPENSE_ID", expense.getId());
        startActivity(intent);
    }

    private void showBalances() {
        removeExpenseListener();
        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> {
                    Object memberIdsObj = groupDoc.get("memberIds");
                    if (!(memberIdsObj instanceof List)) return;
                    List<String> memberIds = (List<String>) memberIdsObj;

                    List<User> members = new ArrayList<>();
                    AtomicInteger fetchCount = new AtomicInteger(0);
                    for (String uid : memberIds) {
                        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                            User u = userDoc.toObject(User.class);
                            if (u != null) members.add(u);
                            if (fetchCount.incrementAndGet() == memberIds.size()) {
                                computeAndShowBalances(members);
                            }
                        });
                    }
                });
    }

    private void computeAndShowBalances(List<User> members) {
        db.collection("expenses").whereEqualTo("groupId", groupId).get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Double> balances = new HashMap<>();
                    for (User u : members) balances.put(u.getUid(), 0.0);

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Expense exp = doc.toObject(Expense.class);
                        if (!Expense.STATUS_COMPLETED.equals(exp.getStatus())) continue;

                        String payer = exp.getPayerId();
                        if (balances.containsKey(payer)) {
                            Double current = balances.get(payer);
                            balances.put(payer, (current != null ? current : 0.0) + exp.getAmount());
                        }

                        Map<String, Double> split = exp.getSplitDetails();
                        if (split != null) {
                            for (Map.Entry<String, Double> entry : split.entrySet()) {
                                if (balances.containsKey(entry.getKey())) {
                                    Double current = balances.get(entry.getKey());
                                    balances.put(entry.getKey(), (current != null ? current : 0.0) - entry.getValue());
                                }
                            }
                        }
                    }
                    BalanceAdapter adapter = new BalanceAdapter(members, balances);
                    adapter.setActionListener(this);
                    rvGroupContent.setAdapter(adapter);
                });
    }

    @Override
    public void onSettleUp(User user, double balance) {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("TARGET_USER_ID", user.getUid());
        intent.putExtra("AMOUNT", balance);
        startActivity(intent);
    }

    @Override
    public void onRemind(User user, double balance) {
        Toast.makeText(this, "Đã gửi lời nhắc đến " + user.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSettleTransaction(BalanceAdapter.Transaction transaction) {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("TARGET_USER_ID", transaction.toId);
        intent.putExtra("AMOUNT", transaction.amount);
        startActivity(intent);
    }

    private void removeExpenseListener() {
        if (expenseListener != null) { expenseListener.remove(); expenseListener = null; }
    }

    @Override
    protected void onDestroy() { super.onDestroy(); removeExpenseListener(); }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
