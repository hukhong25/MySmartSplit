package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.User;

public class BalanceAdapter extends RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder> {

    private List<User> members;
    private Map<String, Double> balances; // UID -> Số tiền (dương là được trả, âm là nợ)

    public BalanceAdapter(List<User> members, Map<String, Double> balances) {
        this.members = members;
        this.balances = balances;
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_balance, parent, false);
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        User user = members.get(position);
        double balance = balances.containsKey(user.getUid()) ? balances.get(user.getUid()) : 0;

        holder.tvUserName.setText(user.getName());
        
        if (balance > 0) {
            holder.tvBalanceStatus.setText("được trả");
            holder.tvBalanceAmount.setText(String.format("+%,.0f VNĐ", balance));
            holder.tvBalanceAmount.setTextColor(Color.parseColor("#388E3C")); // Xanh lá
        } else if (balance < 0) {
            holder.tvBalanceStatus.setText("nợ");
            holder.tvBalanceAmount.setText(String.format("%,.0f VNĐ", balance));
            holder.tvBalanceAmount.setTextColor(Color.parseColor("#E53935")); // Đỏ
        } else {
            holder.tvBalanceStatus.setText("đã thanh toán hết");
            holder.tvBalanceAmount.setText("0 VNĐ");
            holder.tvBalanceAmount.setTextColor(Color.GRAY);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class BalanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvBalanceStatus, tvBalanceAmount;
        public BalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvBalanceStatus = itemView.findViewById(R.id.tvBalanceStatus);
            tvBalanceAmount = itemView.findViewById(R.id.tvBalanceAmount);
        }
    }
}
