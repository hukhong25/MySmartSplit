package vn.haui.smartsplit.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.CreateGroupActivity;
import vn.haui.smartsplit.GroupDetailsActivity;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Group;

public class GroupsFragment extends Fragment {

    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<Group> groupList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText etSearch;
    private View layoutEmpty;
    private TextView tvGroupCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvGroups     = view.findViewById(R.id.rvGroups);
        etSearch     = view.findViewById(R.id.etSearchGroups);
        layoutEmpty  = view.findViewById(R.id.layoutEmpty);
        tvGroupCount = view.findViewById(R.id.tvGroupCount);

        MaterialButton btnCreate = view.findViewById(R.id.btnCreateGroup);
        MaterialButton btnJoin   = view.findViewById(R.id.btnJoinGroup);

        rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(requireContext(), GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvGroups.setAdapter(adapter);

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
                updateEmptyState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (btnCreate != null) btnCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateGroupActivity.class)));
        if (btnJoin != null) btnJoin.setOnClickListener(v -> showJoinGroupDialog());

        loadGroups();
    }

    private void loadGroups() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    groupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            groupList.add(doc.toObject(Group.class));
                        }
                    }
                    adapter.updateFullList(groupList);
                    String query = etSearch != null ? etSearch.getText().toString() : "";
                    adapter.getFilter().filter(query);
                    updateGroupCount();
                    updateEmptyState();
                });
    }

    private void updateGroupCount() {
        if (tvGroupCount != null) {
            int count = groupList.size();
            tvGroupCount.setText(count + " nhóm");
        }
    }

    private void updateEmptyState() {
        if (rvGroups == null || layoutEmpty == null) return;
        rvGroups.post(() -> {
            boolean empty = adapter.getItemCount() == 0;
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvGroups.setVisibility(empty ? View.GONE : View.VISIBLE);
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
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("groups").whereEqualTo("joinCode", code).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String groupId = snapshots.getDocuments().get(0).getId();
                        db.collection("groups").document(groupId)
                                .update("memberIds", FieldValue.arrayUnion(uid))
                                .addOnSuccessListener(v -> Toast.makeText(requireContext(),
                                        "Đã tham gia nhóm thành công!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                        "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(requireContext(),
                                "Mã không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
