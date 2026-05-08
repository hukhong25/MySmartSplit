package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import vn.haui.smartsplit.adapters.GroupAdapter;
import vn.haui.smartsplit.models.Group;

public class GroupsActivity extends AppCompatActivity {

    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<Group> groupList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        Toolbar toolbar = findViewById(R.id.toolbarGroups);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        // Dữ liệu giả lập để hiển thị giao diện
        groupList = new ArrayList<>();
        groupList.add(new Group("1", "Du lịch Đà Lạt", Arrays.asList("u1", "u2", "u3"), "u1"));
        groupList.add(new Group("2", "Tiền nhà tháng 5", Arrays.asList("u1", "u4"), "u1"));

        adapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(GroupsActivity.this, GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", group.getId());
            intent.putExtra("GROUP_NAME", group.getName());
            startActivity(intent);
        });
        rvGroups.setAdapter(adapter);

        FloatingActionButton fabAddGroup = findViewById(R.id.fabAddGroup);
        fabAddGroup.setOnClickListener(v -> {
            startActivity(new Intent(GroupsActivity.this, CreateGroupActivity.class));
        });
    }
}
