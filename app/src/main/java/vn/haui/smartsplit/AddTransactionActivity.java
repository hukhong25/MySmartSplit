package vn.haui.smartsplit;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddTransactionActivity extends BaseActivity {

    private EditText etAmount, etNote, etDate;
    private Spinner spCategory;
    private Button btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etAmount = findViewById(R.id.etAmount);
        spCategory = findViewById(R.id.spCategory);
        etNote = findViewById(R.id.etNote);
        etDate = findViewById(R.id.etDate);
        btnSave = findViewById(R.id.btnSave);

        // Thiết lập Spinner danh mục
        String[] categories = {"Ăn uống", "Di chuyển", "Mua sắm", "Giải trí", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        // Hiển thị DatePicker khi chọn ngày
        Calendar calendar = Calendar.getInstance();
        String today = calendar.get(Calendar.DAY_OF_MONTH) + "/" +
                (calendar.get(Calendar.MONTH) + 1) + "/" +
                calendar.get(Calendar.YEAR);
        etDate.setText(today);

        etDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(this, (view, y, m, d) -> {
                String date = d + "/" + (m + 1) + "/" + y;
                etDate.setText(date);
            }, year, month, day).show();
        });

        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("uid", uid);
        transaction.put("amount", amount);
        transaction.put("category", category);
        transaction.put("note", note);
        transaction.put("date", date);
        transaction.put("timestamp", System.currentTimeMillis());

        btnSave.setEnabled(false);
        db.collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Đã lưu giao dịch thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu giao dịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
