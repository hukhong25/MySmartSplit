package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.User;

public class BalanceAdapter extends RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder> {

    private final List<User> members;
    private final Map<String, Double> balances;
    private OnActionClickListener actionListener;

    // Rotating avatar colors
    private static final int[] AVATAR_COLORS = {
            Color.parseColor("#1CC29F"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#EC4899")
    };

    public interface OnActionClickListener {
        void onSettleUp(User user, double balance);
        void onRemind(User user, double balance);
    }

    public BalanceAdapter(List<User> members, Map<String, Double> balances) {
        this.members = members;
        this.balances = balances;
    }

    public void setActionListener(OnActionClickListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_balance, parent, false);
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        User user = members.get(position);
        double balance = balances.containsKey(user.getUid())
                ? balances.get(user.getUid()) : 0;

        DecimalFormat fmt = new DecimalFormat("#,###");

        // Avatar initial + color
        String name = user.getName() != null ? user.getName() : "?";
        holder.tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        int colorIdx = position % AVATAR_COLORS.length;
        holder.viewAvatarBg.setBackgroundColor(AVATAR_COLORS[colorIdx]);

        holder.tvUserName.setText(name);

        if (balance > 0) {
            // Họ nợ bạn → bạn được nợ
            holder.tvBalanceStatus.setText("Nợ bạn ₫" + fmt.format(balance));
            holder.tvBalanceStatus.setTextColor(Color.parseColor("#1CC29F"));
            holder.btnBalanceAction.setText("Nhắc nhở");
            holder.btnBalanceAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#E6F9F5")));
            holder.btnBalanceAction.setTextColor(Color.parseColor("#1CC29F"));
            holder.btnBalanceAction.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onRemind(user, balance);
            });
        } else if (balance < 0) {
            // Bạn nợ họ
            double owe = Math.abs(balance);
            holder.tvBalanceStatus.setText("Bạn nợ ₫" + fmt.format(owe));
            holder.tvBalanceStatus.setTextColor(Color.parseColor("#E53935"));
            holder.btnBalanceAction.setText("Tất toán");
            holder.btnBalanceAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#1CC29F")));
            holder.btnBalanceAction.setTextColor(Color.WHITE);
            holder.btnBalanceAction.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onSettleUp(user, owe);
            });
        } else {
            holder.tvBalanceStatus.setText("Đã tất toán");
            holder.tvBalanceStatus.setTextColor(Color.parseColor("#9E9E9E"));
            holder.btnBalanceAction.setText("Đã xong");
            holder.btnBalanceAction.setEnabled(false);
            holder.btnBalanceAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
            holder.btnBalanceAction.setTextColor(Color.parseColor("#9E9E9E"));
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class BalanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarInitial, tvUserName, tvBalanceStatus;
        View viewAvatarBg;
        MaterialButton btnBalanceAction;

        public BalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial  = itemView.findViewById(R.id.tvAvatarInitial);
            tvUserName       = itemView.findViewById(R.id.tvUserName);
            tvBalanceStatus  = itemView.findViewById(R.id.tvBalanceStatus);
            viewAvatarBg     = itemView.findViewById(R.id.viewAvatarBg);
            btnBalanceAction = itemView.findViewById(R.id.btnBalanceAction);
        }
    }
}
