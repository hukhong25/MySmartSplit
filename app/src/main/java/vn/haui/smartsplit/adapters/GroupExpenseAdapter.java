package vn.haui.smartsplit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;

public class GroupExpenseAdapter extends RecyclerView.Adapter<GroupExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenseList;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public GroupExpenseAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);

        holder.tvDescription.setText(expense.getDescription());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "%,.0f VND", expense.getAmount()));

        // Hiển thị người trả và thời gian
        String payerName = expense.getPayerName();
        if (payerName == null || payerName.isEmpty()) payerName = "Không rõ";
        String dateStr = DATE_FORMAT.format(new Date(expense.getTimestamp()));
        holder.tvNote.setText("Trả bởi: " + payerName + "  •  " + dateStr);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvNote;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}
