package vn.haui.smartsplit;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettleUpActivity extends AppCompatActivity {

    private Spinner spFromUser, spToUser;
    private EditText etSettleAmount;
    private Button btnConfirmSettle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_up);

        spFromUser = findViewById(R.id.spFromUser);
        spToUser = findViewById(R.id.spToUser);
        etSettleAmount = findViewById(R.id.etSettleAmount);
        btnConfirmSettle = findViewById(R.id.btnConfirmSettle);

        // Dữ liệu mẫu
        String[] members = {"Tôi", "Nguyễn Văn A", "Trần Thị B"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, members);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spFromUser.setAdapter(adapter);
        spToUser.setAdapter(adapter);

        btnConfirmSettle.setOnClickListener(v -> {
            String amount = etSettleAmount.getText().toString().trim();
            if (amount.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            } else {
                // Logic cập nhật số dư trên Firebase
                Toast.makeText(this, "Xác nhận trả nợ thành công", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
