package vn.haui.smartsplit;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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

        // Đọc mảng danh mục trực tiếp từ tài nguyên ngôn ngữ hệ thống
        String[] categories = {
                getString(R.string.cat_food),
                getString(R.string.cat_travel),
                getString(R.string.cat_shopping),
                getString(R.string.cat_entertainment),
                getString(R.string.cat_other)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        // Thiết lập DatePicker và định dạng ngày hiển thị mặc định
        Calendar calendar = Calendar.getInstance();
        String today = getString(R.string.date_format,
                calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.MONTH) + 1),
                calendar.get(Calendar.YEAR));
        etDate.setText(today);

        etDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(this, (view, y, m, d) -> {
                String date = getString(R.string.date_format, d, (m + 1), y);
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
            Toast.makeText(this, getString(R.string.toast_missing_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.toast_invalid_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, getString(R.string.toast_not_logged_in), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, getString(R.string.toast_save_transaction_success), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, getString(R.string.toast_save_transaction_error_prefix, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }
}