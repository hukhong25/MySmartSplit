package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class AddGroupExpenseActivity extends AppCompatActivity {

    private EditText etExpenseDescription, etExpenseAmount;
    private Spinner spPayer;
    private RecyclerView rvSplitMembers;
    private Button btnSaveGroupExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_expense);

        etExpenseDescription = findViewById(R.id.etExpenseDescription);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        spPayer = findViewById(R.id.spPayer);
        rvSplitMembers = findViewById(R.id.rvSplitMembers);
        btnSaveGroupExpense = findViewById(R.id.btnSaveGroupExpense);

        rvSplitMembers.setLayoutManager(new LinearLayoutManager(this));

        // Dữ liệu mẫu thành viên nhóm
        String[] members = {"Tôi", "Nguyễn Văn A", "Trần Thị B"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, members);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPayer.setAdapter(adapter);

        // Sau này sẽ cần một adapter cho RecyclerView để chọn người tham gia chia tiền

        btnSaveGroupExpense.setOnClickListener(v -> {
            String desc = etExpenseDescription.getText().toString().trim();
            String amount = etExpenseAmount.getText().toString().trim();

            if (desc.isEmpty() || amount.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            } else {
                // Lưu vào Firestore
                Toast.makeText(this, "Đã thêm hóa đơn nhóm", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
