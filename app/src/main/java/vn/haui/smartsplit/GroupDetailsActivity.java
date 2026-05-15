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

public class GroupDetailsActivity extends AppCompatActivity {

    private RecyclerView rvGroupContent;
    private TabLayout tabLayoutGroup;
    private FloatingActionButton fabAddGroupExpense;
    private String groupId, groupName;
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

        // FAB mặc định: thêm chi tiêu
        fabAddGroupExpense.setOnClickListener(v -> openAddExpense());

        tabLayoutGroup.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    showExpenses();
                    fabAddGroupExpense.setOnClickListener(v -> openAddExpense());
                } else {
                    showBalances();
                    fabAddGroupExpense.setOnClickListener(v -> openSettleUp());
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void openAddExpense() {
        Intent intent = new Intent(GroupDetailsActivity.this, AddGroupExpenseActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
    }

    private void openSettleUp() {
        Intent intent = new Intent(GroupDetailsActivity.this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        startActivity(intent);
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
        removeExpenseListener();

        List<Expense> expenses = new ArrayList<>();
        GroupExpenseAdapter adapter = new GroupExpenseAdapter(expenses);
        rvGroupContent.setAdapter(adapter);

        expenseListener = db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải chi tiêu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    expenses.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Expense expense = doc.toObject(Expense.class);
                            expenses.add(expense);
                        }
                        expenses.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showBalances() {
        removeExpenseListener();

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> {
                    List<String> memberIds = (List<String>) groupDoc.get("memberIds");
                    if (memberIds == null || memberIds.isEmpty()) {
                        rvGroupContent.setAdapter(new BalanceAdapter(new ArrayList<>(), new HashMap<>()));
                        return;
                    }

                    List<User> members = new ArrayList<>();
                    AtomicInteger fetchCount = new AtomicInteger(0);
                    int total = memberIds.size();

                    for (String uid : memberIds) {
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    String email = userDoc.getString("email");
                                    if (name == null || name.isEmpty()) {
                                        name = (email != null) ? email : uid;
                                    }
                                    members.add(new User(userDoc.getId(), name, email));

                                    if (fetchCount.incrementAndGet() == total) {
                                        computeAndShowBalances(members);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (fetchCount.incrementAndGet() == total) {
                                        computeAndShowBalances(members);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải nhóm: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void computeAndShowBalances(List<User> members) {
        db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Double> balances = new HashMap<>();
                    for (User u : members) {
                        balances.put(u.getUid(), 0.0);
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Expense expense = doc.toObject(Expense.class);

                        // Người trả được cộng toàn bộ số tiền
                        String payerId = expense.getPayerId();
                        if (balances.containsKey(payerId)) {
                            balances.put(payerId, balances.get(payerId) + expense.getAmount());
                        }

                        // Mỗi người trong splitDetails bị trừ phần họ nợ
                        Map<String, Double> splitDetails = expense.getSplitDetails();
                        if (splitDetails != null) {
                            for (Map.Entry<String, Double> entry : splitDetails.entrySet()) {
                                String uid = entry.getKey();
                                Double owed = entry.getValue();
                                if (balances.containsKey(uid)) {
                                    balances.put(uid, balances.get(uid) - owed);
                                }
                            }
                        }
                    }

                    BalanceAdapter adapter = new BalanceAdapter(members, balances);
                    rvGroupContent.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tính số dư: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeExpenseListener() {
        if (expenseListener != null) {
            expenseListener.remove();
            expenseListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeExpenseListener();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
