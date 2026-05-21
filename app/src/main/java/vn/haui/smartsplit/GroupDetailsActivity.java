package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;
import vn.haui.smartsplit.models.User;

public class GroupDetailsActivity extends BaseActivity implements
        BalanceAdapter.OnActionClickListener,
        GroupExpenseAdapter.OnExpenseActionListener {

    private RecyclerView rvGroupContent;
    private TabLayout tabLayoutGroup;
    private FloatingActionButton fabAddGroupExpense;
    private String groupId, groupName;
    private String joinCode;
    private FirebaseFirestore db;
    private ListenerRegistration expenseListener;
    private ListenerRegistration groupListener;
    private int currentTab = 0;

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

        tabLayoutGroup.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshCurrentTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
    }

    private void refreshCurrentTab() {
        removeListeners();
        if (currentTab == 0) {
            showExpenses();
            fabAddGroupExpense.setImageResource(android.R.drawable.ic_input_add);
            fabAddGroupExpense.setOnClickListener(v -> openAddExpense());
        } else {
            showBalances();
            fabAddGroupExpense.setImageResource(android.R.drawable.ic_menu_send);
            fabAddGroupExpense.setOnClickListener(v -> openSettleUp());
        }
    }

    private void fetchGroupDetails() {
        if (groupListener != null) groupListener.remove();
        groupListener = db.collection("groups").document(groupId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null) return;
                    Group group = documentSnapshot.toObject(Group.class);
                    if (group != null) {
                        joinCode = group.getJoinCode();
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setSubtitle(getString(R.string.group_subtitle_code_prefix, joinCode));
                        }
                    }
                });
    }

    private void showExpenses() {
        List<Expense> expenses = new ArrayList<>();
        GroupExpenseAdapter adapter = new GroupExpenseAdapter(expenses, this);
        rvGroupContent.setAdapter(adapter);

        expenseListener = db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || currentTab != 0) return;
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

    @Override
    public void onExpenseClick(Expense expense) {
        Intent intent = new Intent(this, SettleDetailActivity.class);
        intent.putExtra("EXPENSE_ID", expense.getId());
        startActivity(intent);
    }

    @Override
    public void onExpenseEdit(Expense expense) {
        if (expense.isSettlement()) {
            Toast.makeText(this, getString(R.string.toast_cannot_edit_settlement), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, AddGroupExpenseActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("EXPENSE_ID", expense.getId());
        startActivity(intent);
    }

    @Override
    public void onExpenseDelete(Expense expense) {
        if (expense.isSettlement()) {
            Toast.makeText(this, getString(R.string.toast_cannot_delete_settlement), Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_confirm_delete_title))
                .setMessage(getString(R.string.dialog_confirm_delete_expense_msg))
                .setPositiveButton(getString(R.string.dialog_action_agree), (dialog, which) -> {
                    db.collection("expenses").document(expense.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.toast_delete_success), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.toast_error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(getString(R.string.dialog_action_cancel), null)
                .show();
    }

    private void showBalances() {
        db.collection("groups").document(groupId).get().addOnSuccessListener(groupDoc -> {
            if (currentTab != 1) return;
            @SuppressWarnings("unchecked")
            List<String> memberIds = (List<String>) groupDoc.get("memberIds");
            if (memberIds == null) return;

            List<User> members = new ArrayList<>();
            AtomicInteger fetchCount = new AtomicInteger(0);
            for (String uid : memberIds) {
                db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                    if (currentTab != 1) return;
                    User u = userDoc.toObject(User.class);
                    if (u != null) { u.setUid(userDoc.getId()); members.add(u); }
                    if (fetchCount.incrementAndGet() == memberIds.size()) {
                        listenToExpensesForBalances(members);
                    }
                });
            }
        });
    }

    private void listenToExpensesForBalances(List<User> members) {
        expenseListener = db.collection("expenses")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || currentTab != 1) return;

                    Map<String, Double> balances = new HashMap<>();
                    for (User u : members) balances.put(u.getUid(), 0.0);

                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Expense exp = doc.toObject(Expense.class);
                            if (!Expense.STATUS_COMPLETED.equals(exp.getStatus())) continue;

                            String payer = exp.getPayerId();
                            if (balances.containsKey(payer)) {
                                balances.put(payer, balances.get(payer) + exp.getAmount());
                            }

                            Map<String, Double> split = exp.getSplitDetails();
                            if (split != null) {
                                for (Map.Entry<String, Double> entry : split.entrySet()) {
                                    if (balances.containsKey(entry.getKey())) {
                                        balances.put(entry.getKey(), balances.get(entry.getKey()) - entry.getValue());
                                    }
                                }
                            }
                        }
                    }
                    BalanceAdapter adapter = new BalanceAdapter(members, balances);
                    adapter.setActionListener(this);
                    rvGroupContent.setAdapter(adapter);
                });
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

    @Override
    public void onSettleUp(User user, double balance) {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("TARGET_USER_ID", user.getUid());
        intent.putExtra("AMOUNT", Math.abs(balance));
        startActivity(intent);
    }

    @Override
    public void onRemind(User user, double balance) {
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentUserName == null || currentUserName.isEmpty()) {
            currentUserName = getString(R.string.default_member_name);
        }

        String title = getString(R.string.notif_title_debt_remind);
        String content = getString(R.string.notif_content_debt_remind_format, currentUserName, (long) Math.abs(balance), groupName);

        String notifId = db.collection("notifications").document().getId();
        AppNotification notif = new AppNotification(
                notifId,
                user.getUid(),
                title,
                content,
                "REMIND",
                groupId
        );
        db.collection("notifications").document(notifId).set(notif)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, getString(R.string.toast_remind_sent_success, user.getName()), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSettleTransaction(BalanceAdapter.Transaction transaction) {
        Intent intent = new Intent(this, SettleUpActivity.class);
        intent.putExtra("GROUP_ID", groupId);
        intent.putExtra("TARGET_USER_ID", transaction.toId);
        intent.putExtra("AMOUNT", transaction.amount);
        startActivity(intent);
    }

    private void removeListeners() {
        if (expenseListener != null) { expenseListener.remove(); expenseListener = null; }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
        if (groupListener != null) groupListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) { finish(); return true; }
        if (id == R.id.action_invite) { shareInviteCode(); return true; }
        if (id == R.id.action_manage_members) {
            Intent intent = new Intent(this, GroupMembersActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareInviteCode() {
        if (joinCode == null) return;
        String message = getString(R.string.share_invite_msg_format, groupName, joinCode);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));
    }
}