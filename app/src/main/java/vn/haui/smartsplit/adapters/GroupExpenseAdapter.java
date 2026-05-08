package vn.haui.smartsplit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;

public class GroupExpenseAdapter extends RecyclerView.Adapter<GroupExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;

    public GroupExpenseAdapter(List<Expense> expenseList) {
        this.expenseList = expenseList;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.tvDescription.setText(expense.getDescription());
        holder.tvAmount.setText(String.format("%,.0f VNĐ", expense.getAmount()));
        // Có thể thêm logic hiển thị ai là người trả tiền ở đây
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount;
        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvCategoryName); // Dùng lại ID từ item_transaction
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
