package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.adapters.NotificationAdapter;
import vn.haui.smartsplit.models.AppNotification;
import vn.haui.smartsplit.viewmodels.NotificationsViewModel;

public class NotificationsActivity extends BaseActivity {

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;
    private List<AppNotification> notificationList = new ArrayList<>();
    private NotificationsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.notifications_title);
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        progressBar = findViewById(R.id.progressBar);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, this::handleNotificationClick);
        rvNotifications.setAdapter(adapter);

        observeViewModel();
        viewModel.loadNotifications();
    }

    private void observeViewModel() {
        viewModel.getNotifications().observe(this, notifications -> {
            notificationList.clear();
            notificationList.addAll(notifications);
            adapter.notifyDataSetChanged();
            tvNoNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(this, errMsg -> {
            if (errMsg != null) {
                Toast.makeText(this, getString(R.string.toast_error_prefix, errMsg), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Add loading state if needed in VM, currently VM is simple
    }

    private void handleNotificationClick(AppNotification notification) {
        // Mark as read in VM
        viewModel.markAsRead(notification.getId());

        // Navigation logic
        Intent intent = null;
        String type = notification.getType();
        String relatedId = notification.getRelatedId();

        if ("PAYMENT_REQUEST".equals(type) || "EXPENSE_ADDED".equals(type) || "PAYMENT_RESPONSE".equals(type)) {
            intent = new Intent(this, SettleDetailActivity.class);
            intent.putExtra("EXPENSE_ID", relatedId);
        } else if ("REMIND".equals(type)) {
            intent = new Intent(this, GroupDetailsActivity.class);
            intent.putExtra("GROUP_ID", relatedId);
            // Optionally pass group name if available, but init(groupId) in VM will fetch it
        }

        if (intent != null) startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
