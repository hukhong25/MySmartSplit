package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.haui.smartsplit.adapters.NotificationAdapter;
import vn.haui.smartsplit.models.AppNotification;

public class NotificationsActivity extends BaseActivity {

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private NotificationAdapter adapter;
    private List<AppNotification> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông báo");
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this::handleNotificationClick);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getUid();

        // Lấy tất cả thông báo của user, không dùng orderBy để tránh lỗi Index
        db.collection("notifications")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            AppNotification notif = doc.toObject(AppNotification.class);
                            notificationList.add(notif);
                        }
                    }

                    // Sắp xếp theo thời gian mới nhất lên đầu trong code Java
                    Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                    adapter.notifyDataSetChanged();
                    tvNoNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void handleNotificationClick(AppNotification notification) {
        // Đánh dấu đã đọc
        db.collection("notifications").document(notification.getId()).update("read", true);

        // Chuyển màn hình dựa trên loại thông báo
        Intent intent = null;
        if ("PAYMENT_REQUEST".equals(notification.getType()) || "EXPENSE_ADDED".equals(notification.getType()) || "PAYMENT_RESPONSE".equals(notification.getType())) {
            intent = new Intent(this, SettleDetailActivity.class);
            intent.putExtra("EXPENSE_ID", notification.getRelatedId());
        } else if ("REMIND".equals(notification.getType())) {
            intent = new Intent(this, GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", notification.getRelatedId());
        }

        if (intent != null) startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
