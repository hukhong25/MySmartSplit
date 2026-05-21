package vn.haui.smartsplit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.smartsplit.CreateGroupActivity;
import vn.haui.smartsplit.GroupDetailsActivity;
import vn.haui.smartsplit.NotificationsActivity;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Expense;
import vn.haui.smartsplit.models.Group;

public class DashboardFragment extends Fragment {

    private RecyclerView rvDashboardGroups;
    private GroupAdapter groupAdapter;
    private List<Group> groupList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvTotalOwe, tvTotalOwed, tvWelcome, tvSeeAllGroups;
    private ImageView ivNotification;
    private View viewNotifBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvWelcome        = view.findViewById(R.id.tvWelcome);
        tvTotalOwe       = view.findViewById(R.id.tvTotalOwe);
        tvTotalOwed      = view.findViewById(R.id.tvTotalOwed);
        tvSeeAllGroups   = view.findViewById(R.id.tvSeeAllGroups);
        rvDashboardGroups = view.findViewById(R.id.rvDashboardGroups);
        ivNotification   = view.findViewById(R.id.ivNotification);
        viewNotifBadge   = view.findViewById(R.id.viewNotifBadge);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvWelcome.setText(user.getDisplayName());
        } else if (user != null) {
            tvWelcome.setText("Người dùng");
        }

        rvDashboardGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupList = new ArrayList<>();
        groupAdapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(requireContext(), GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvDashboardGroups.setAdapter(groupAdapter);

        tvSeeAllGroups.setOnClickListener(v -> {
            if (getActivity() instanceof vn.haui.smartsplit.HomeContainerActivity) {
                ((vn.haui.smartsplit.HomeContainerActivity) getActivity())
                        .navigateTo(R.id.nav_groups);
            }
        });

        ivNotification.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), NotificationsActivity.class));
        });

        View btnCreate = view.findViewById(R.id.btnQuickCreateGroup);
        View btnJoin   = view.findViewById(R.id.btnQuickJoinGroup);
        if (btnCreate != null) btnCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateGroupActivity.class)));
        if (btnJoin != null) btnJoin.setOnClickListener(v -> showJoinGroupDialog());

        loadDashboardData();
        listenToNotifications();
    }

    private void listenToNotifications() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getUid();

        db.collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;
                    viewNotifBadge.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .limit(3)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    groupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Group group = doc.toObject(Group.class);
                            groupList.add(group);
                        }
                    }
                    groupAdapter.notifyDataSetChanged();
                });

        db.collectionGroup("expenses")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !isAdded()) return;

                    Map<String, Double> groupBalances = new HashMap<>();

                    for (QueryDocumentSnapshot doc : value) {
                        String status = doc.getString("status");
                        Boolean isSettlement = doc.getBoolean("settlement");
                        if (isSettlement == null) isSettlement = false;

                        // Logic: Chấp nhận COMPLETED cho tất cả, hoặc PENDING nếu là settlement (để cập nhật số dư tạm thời)
                        if (!Expense.STATUS_COMPLETED.equals(status)) {
                            if (!(isSettlement && Expense.STATUS_PENDING.equals(status))) {
                                continue;
                            }
                        }

                        String groupId = doc.getString("groupId");
                        if (groupId == null) continue;

                        String payerId = doc.getString("payerId");
                        double amount = 0;
                        Object amtObj = doc.get("amount");
                        if (amtObj != null) {
                            try { amount = Double.parseDouble(amtObj.toString()); }
                            catch (Exception ignored) {}
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, Object> splitDetails = (Map<String, Object>) doc.get("splitDetails");
                        if (splitDetails == null) continue;

                        double userBalanceChange = 0;
                        if (currentUserId.equals(payerId)) {
                            userBalanceChange += amount;
                        }
                        if (splitDetails.containsKey(currentUserId)) {
                            try {
                                userBalanceChange -= Double.parseDouble(splitDetails.get(currentUserId).toString());
                            } catch (Exception ignored) {}
                        }

                        if (userBalanceChange != 0) {
                            double current = groupBalances.getOrDefault(groupId, 0.0);
                            groupBalances.put(groupId, current + userBalanceChange);
                        }
                    }

                    double totalOwe = 0;
                    double totalOwed = 0;
                    for (double bal : groupBalances.values()) {
                        if (bal > 1.0) totalOwed += bal;
                        else if (bal < -1.0) totalOwe += Math.abs(bal);
                    }

                    DecimalFormat fmt = new DecimalFormat("#,###");
                    tvTotalOwe.setText(fmt.format(totalOwe) + " VNĐ");
                    tvTotalOwed.setText(fmt.format(totalOwed) + " VNĐ");
                });
    }

    private void showJoinGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nhập mã tham gia");
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setHint("Ví dụ: ABC123");
        builder.setView(input);
        builder.setPositiveButton("Tham gia", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (!code.isEmpty()) joinGroupWithCode(code);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void joinGroupWithCode(String code) {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("groups").whereEqualTo("joinCode", code).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String groupId = snapshots.getDocuments().get(0).getId();
                        db.collection("groups").document(groupId)
                                .update("memberIds", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener(v -> Toast.makeText(requireContext(),
                                        "Đã tham gia nhóm thành công!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(requireContext(),
                                "Mã tham gia không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
