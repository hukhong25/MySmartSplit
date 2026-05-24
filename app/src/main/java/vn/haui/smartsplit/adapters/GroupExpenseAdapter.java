package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Expense;

public class GroupExpenseAdapter extends RecyclerView.Adapter<GroupExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenseList;
    private final OnExpenseActionListener listener;
    private final String currentUserId;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnExpenseActionListener {
        void onExpenseClick(Expense expense);
        void onExpenseEdit(Expense expense);
        void onExpenseDelete(Expense expense);
    }

    public GroupExpenseAdapter(List<Expense> expenseList, OnExpenseActionListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
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
        android.content.Context ctx = holder.itemView.getContext();

        holder.tvDescription.setText(expense.getDescription());
        holder.tvAmount.setText(ctx.getString(R.string.currency_vnd_format, expense.getAmount()));

        String payerName = expense.getPayerName();
        if (payerName == null || payerName.isEmpty()) {
            payerName = ctx.getString(R.string.status_unknown);
        }
        String dateStr = DATE_FORMAT.format(new Date(expense.getTimestamp()));

        String statusSuffix = "";
        if (expense.isSettlement()) {
            if (Expense.STATUS_PENDING.equals(expense.getStatus())) {
                statusSuffix = ctx.getString(R.string.status_pending_suffix);
                holder.tvAmount.setTextColor(Color.parseColor("#FFA500"));
            } else if (Expense.STATUS_REJECTED.equals(expense.getStatus())) {
                statusSuffix = ctx.getString(R.string.status_rejected_suffix);
                holder.tvAmount.setTextColor(Color.RED);
            } else {
                holder.tvAmount.setTextColor(Color.parseColor("#2E7D32"));
            }
            holder.tvShare.setVisibility(View.GONE);
            holder.btnOptions.setVisibility(View.GONE);
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#E91E63"));
            holder.btnOptions.setVisibility(View.VISIBLE);
            holder.tvShare.setVisibility(View.VISIBLE);

            // Sửa lỗi: Sử dụng Map<String, Object> và ép kiểu an toàn
            Map<String, Object> split = expense.getSplitDetails();
            boolean isPayer = currentUserId != null && currentUserId.equals(expense.getPayerId());

            if (isPayer) {
                double myShare = 0;
                if (split != null && split.containsKey(currentUserId)) {
                    try {
                        Object val = split.get(currentUserId);
                        if (val != null) myShare = Double.parseDouble(val.toString());
                    } catch (Exception ignored) {}
                }
                double othersOwe = expense.getAmount() - myShare;
                holder.tvShare.setText(ctx.getString(R.string.expense_lend_format, othersOwe));
                holder.tvShare.setTextColor(Color.parseColor("#2E7D32"));
            } else if (split != null && split.containsKey(currentUserId)) {
                double myDebt = 0;
                try {
                    Object val = split.get(currentUserId);
                    if (val != null) myDebt = Double.parseDouble(val.toString());
                } catch (Exception ignored) {}
                holder.tvShare.setText(ctx.getString(R.string.expense_owe_format, myDebt));
                holder.tvShare.setTextColor(Color.parseColor("#E91E63"));
            } else {
                holder.tvShare.setText(ctx.getString(R.string.expense_not_involved));
                holder.tvShare.setTextColor(Color.GRAY);
            }
        }

        holder.tvNote.setText(ctx.getString(R.string.expense_note_format, payerName, dateStr, statusSuffix));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onExpenseClick(expense);
        });

        holder.btnOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            String menuEdit = ctx.getString(R.string.menu_edit);
            String menuDelete = ctx.getString(R.string.menu_delete);

            popup.getMenu().add(menuEdit);
            popup.getMenu().add(menuDelete);
            popup.setOnMenuItemClickListener(item -> {
                CharSequence title = item.getTitle();
                if (menuEdit.equals(title)) {
                    if (listener != null) listener.onExpenseEdit(expense);
                } else if (menuDelete.equals(title)) {
                    if (listener != null) listener.onExpenseDelete(expense);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvAmount, tvNote, tvShare;
        ImageButton btnOptions;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvTransactionDesc);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvNote = itemView.findViewById(R.id.tvTransactionPayer);
            tvShare = itemView.findViewById(R.id.tvTransactionShare);
            btnOptions = itemView.findViewById(R.id.btnExpenseOptions);
        }
    }
}
