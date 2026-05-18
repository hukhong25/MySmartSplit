package vn.haui.smartsplit;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import vn.haui.smartsplit.fragments.DashboardFragment;
import vn.haui.smartsplit.fragments.GroupsFragment;
import vn.haui.smartsplit.fragments.ProfileFragment;
import vn.haui.smartsplit.fragments.StatsFragment;

/**
 * HomeContainerActivity – host duy nhất cho BottomNavigationView.
 * Quản lý 4 Fragment: Dashboard, Groups, Stats, Profile.
 */
public class HomeContainerActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private static final String KEY_SELECTED_TAB = "selected_tab";
    private int currentTabId = R.id.nav_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_container);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Khôi phục tab đã chọn (nếu có)
        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_dashboard);
        }

        // Hiển thị fragment tương ứng với tab ban đầu
        if (savedInstanceState == null) {
            loadFragment(getFragmentForId(currentTabId));
        }

        bottomNavigationView.setSelectedItemId(currentTabId);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentTabId) return true;
            currentTabId = id;
            loadFragment(getFragmentForId(id));
            return true;
        });
    }

    private Fragment getFragmentForId(int id) {
        if (id == R.id.nav_groups)  return new GroupsFragment();
        if (id == R.id.nav_stats)   return new StatsFragment();
        if (id == R.id.nav_profile) return new ProfileFragment();
        return new DashboardFragment();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, currentTabId);
    }

    /** Cho phép fragment con chuyển sang tab khác */
    public void navigateTo(int tabId) {
        bottomNavigationView.setSelectedItemId(tabId);
    }

    /** Cho phép fragment con mở màn hình ngoài bottom nav */
    public void openActivity(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }
}
