package vn.haui.smartsplit.adapters;

import android.graphics.Color;
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
    private final OnExpenseClickListener listener;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public GroupExpenseAdapter(List<Expense> expenseList, OnExpenseClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
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

        String payerName = expense.getPayerName();
        if (payerName == null || payerName.isEmpty()) payerName = "Không rõ";
        String dateStr = DATE_FORMAT.format(new Date(expense.getTimestamp()));
        
        String statusSuffix = "";
        if (expense.isSettlement()) {
            if (Expense.STATUS_PENDING.equals(expense.getStatus())) {
                statusSuffix = " (Chờ xác nhận)";
                holder.tvAmount.setTextColor(Color.parseColor("#FFA500")); // Orange
            } else if (Expense.STATUS_REJECTED.equals(expense.getStatus())) {
                statusSuffix = " (Bị từ chối)";
                holder.tvAmount.setTextColor(Color.RED);
            } else {
                holder.tvAmount.setTextColor(Color.parseColor("#2E7D32")); // Green
            }
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#E91E63")); // Standard expense color
        }

        holder.tvNote.setText("Trả bởi: " + payerName + "  •  " + dateStr + statusSuffix);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });
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
