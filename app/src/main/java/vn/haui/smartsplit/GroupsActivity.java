package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Group;

public class GroupsActivity extends BaseActivity {

    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<Group> groupList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SearchView searchViewGroups;
    private TextView tvEmptyGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbarGroups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvGroups = findViewById(R.id.rvGroups);
        tvEmptyGroups = findViewById(R.id.tvEmptyGroups);
        searchViewGroups = findViewById(R.id.searchViewGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(GroupsActivity.this, GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvGroups.setAdapter(adapter);

        // Kết nối SearchView với bộ lọc adapter
        searchViewGroups.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                updateEmptyState(newText);
                return true;
            }
        });

        // Load data from Firestore
        loadGroups();

    }

    private void loadGroups() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        db.collection("groups")
                .whereArrayContains("memberIds", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    groupList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Group group = doc.toObject(Group.class);
                            groupList.add(group);
                        }
                    }
                    // Cập nhật danh sách gốc trong adapter sau mỗi lần Firestore reload
                    adapter.updateFullList(groupList);

                    // Áp dụng lại bộ lọc hiện tại nếu người dùng đang tìm kiếm
                    String currentQuery = searchViewGroups.getQuery().toString();
                    adapter.getFilter().filter(currentQuery);
                    updateEmptyState(currentQuery);
                });
    }

    /** Hiện/ẩn thông báo "Không tìm thấy nhóm nào" */
    private void updateEmptyState(String query) {
        // Dùng post để chờ adapter cập nhật xong
        rvGroups.post(() -> {
            if (adapter.getItemCount() == 0) {
                tvEmptyGroups.setVisibility(View.VISIBLE);
                rvGroups.setVisibility(View.GONE);
            } else {
                tvEmptyGroups.setVisibility(View.GONE);
                rvGroups.setVisibility(View.VISIBLE);
            }
        });
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
