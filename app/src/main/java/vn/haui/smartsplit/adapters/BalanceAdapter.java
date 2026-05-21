package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.User;

public class BalanceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TRANSACTION = 1;
    private static final int TYPE_MEMBER = 2;

    private final List<User> members;
    private final Map<String, Double> balances;
    private final List<Transaction> optimizedTransactions;
    private final String currentUserId;
    private OnActionClickListener actionListener;

    private static final int[] AVATAR_COLORS = {
            Color.parseColor("#1CC29F"), Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"), Color.parseColor("#EC4899")
    };

    public interface OnActionClickListener {
        void onSettleUp(User user, double balance);
        void onRemind(User user, double balance);
        void onSettleTransaction(Transaction transaction);
    }

    public static class Transaction {
        public String fromId, fromName, toId, toName;
        public double amount;

        public Transaction(String fromId, String fromName, String toId, String toName, double amount) {
            this.fromId = fromId; this.fromName = fromName;
            this.toId = toId; this.toName = toName;
            this.amount = amount;
        }
    }

    public static class DebtCalculator {
        public static List<Transaction> solve(Map<String, Double> netBalances, List<User> members) {
            Map<String, User> userMap = new HashMap<>();
            for (User u : members) userMap.put(u.getUid(), u);

            List<DebtNode> debtors = new ArrayList<>();
            List<DebtNode> creditors = new ArrayList<>();

            for (Map.Entry<String, Double> entry : netBalances.entrySet()) {
                double val = entry.getValue();
                if (val < -1.0) debtors.add(new DebtNode(entry.getKey(), Math.abs(val)));
                else if (val > 1.0) creditors.add(new DebtNode(entry.getKey(), val));
            }

            Collections.sort(debtors, (a, b) -> Double.compare(b.amount, a.amount));
            Collections.sort(creditors, (a, b) -> Double.compare(b.amount, a.amount));

            List<Transaction> result = new ArrayList<>();
            int d = 0, c = 0;
            while (d < debtors.size() && c < creditors.size()) {
                DebtNode debtor = debtors.get(d);
                DebtNode creditor = creditors.get(c);
                double amount = Math.min(debtor.amount, creditor.amount);

                result.add(new Transaction(debtor.uid, userMap.get(debtor.uid).getName(),
                        creditor.uid, userMap.get(creditor.uid).getName(), amount));

                debtor.amount -= amount;
                creditor.amount -= amount;
                if (debtor.amount < 1.0) d++;
                if (creditor.amount < 1.0) c++;
            }
            return result;
        }

        private static class DebtNode {
            String uid; double amount;
            DebtNode(String uid, double amount) { this.uid = uid; this.amount = amount; }
        }
    }

    public BalanceAdapter(List<User> members, Map<String, Double> balances) {
        this.members = members;
        this.balances = balances;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.optimizedTransactions = DebtCalculator.solve(new HashMap<>(balances), members);
    }

    public void setActionListener(OnActionClickListener listener) {
        this.actionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (optimizedTransactions.isEmpty()) return TYPE_MEMBER;
        if (position == 0) return TYPE_HEADER;
        if (position <= optimizedTransactions.size()) return TYPE_TRANSACTION;
        if (position == optimizedTransactions.size() + 1) return TYPE_HEADER;
        return TYPE_MEMBER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_balance, parent, false);
        if (viewType == TYPE_HEADER) return new HeaderViewHolder(view);
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DecimalFormat fmt = new DecimalFormat("#,###");

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder hvh = (HeaderViewHolder) holder;
            String headerText = position == 0
                    ? hvh.itemView.getContext().getString(R.string.header_optimized_settlement)
                    : hvh.itemView.getContext().getString(R.string.header_detailed_balance);
            hvh.tvHeader.setText(headerText);
            return;
        }

        BalanceViewHolder vh = (BalanceViewHolder) holder;
        int viewType = getItemViewType(position);
        android.content.Context ctx = vh.itemView.getContext();

        if (viewType == TYPE_TRANSACTION) {
            Transaction t = optimizedTransactions.get(position - 1);
            boolean isFromMe = t.fromId.equals(currentUserId);
            boolean isToMe = t.toId.equals(currentUserId);

            String fromName = isFromMe ? ctx.getString(R.string.user_you_short) : t.fromName;
            String toName = isToMe ? ctx.getString(R.string.user_you_short) : t.toName;

            vh.tvUserName.setText(ctx.getString(R.string.transaction_direction_format, fromName, toName));
            vh.tvBalanceStatus.setText(ctx.getString(R.string.transfer_amount_format, fmt.format(t.amount)));
            vh.tvBalanceStatus.setTextColor(Color.parseColor("#3B82F6"));
            vh.tvAvatarInitial.setText(ctx.getString(R.string.transaction_arrow_initial));
            vh.viewAvatarBg.setBackgroundColor(Color.LTGRAY);

            if (isFromMe) {
                vh.btnBalanceAction.setVisibility(View.VISIBLE);
                vh.btnBalanceAction.setText(ctx.getString(R.string.settle_up_btn_text));
                vh.btnBalanceAction.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onSettleTransaction(t);
                });
            } else if (isToMe) {
                vh.btnBalanceAction.setVisibility(View.VISIBLE);
                vh.btnBalanceAction.setText(ctx.getString(R.string.remind_debt_btn_text));
                vh.btnBalanceAction.setOnClickListener(v -> {
                    if (actionListener != null) {
                        for (User u : members) if (u.getUid().equals(t.fromId)) {
                            actionListener.onRemind(u, t.amount);
                            break;
                        }
                    }
                });
            } else {
                vh.btnBalanceAction.setVisibility(View.GONE);
            }
        } else {
            int mPos = optimizedTransactions.isEmpty() ? position : position - optimizedTransactions.size() - 2;
            User user = members.get(mPos);
            double balance = balances.getOrDefault(user.getUid(), 0.0);
            double myBalance = balances.getOrDefault(currentUserId, 0.0);
            boolean isMe = user.getUid().equals(currentUserId);

            String name = user.getName() != null ? user.getName() : ctx.getString(R.string.default_username);
            vh.tvUserName.setText(isMe ? ctx.getString(R.string.username_with_you_suffix, name) : name);
            vh.tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            vh.viewAvatarBg.setBackgroundColor(AVATAR_COLORS[mPos % AVATAR_COLORS.length]);

            if (balance > 1.0) {
                vh.tvBalanceStatus.setText(ctx.getString(R.string.receive_amount_format, fmt.format(balance)));
                vh.tvBalanceStatus.setTextColor(Color.parseColor("#1CC29F"));

                if (!isMe && myBalance < -1.0) {
                    vh.btnBalanceAction.setVisibility(View.VISIBLE);
                    vh.btnBalanceAction.setText(ctx.getString(R.string.settle_up_btn_text));
                    vh.btnBalanceAction.setOnClickListener(v -> actionListener.onSettleUp(user, balance));
                } else {
                    vh.btnBalanceAction.setVisibility(View.GONE);
                }
            } else if (balance < -1.0) {
                double owe = Math.abs(balance);
                vh.tvBalanceStatus.setText(ctx.getString(R.string.owe_amount_format, fmt.format(owe)));
                vh.tvBalanceStatus.setTextColor(Color.parseColor("#E53935"));

                if (!isMe && myBalance > 1.0) {
                    vh.btnBalanceAction.setVisibility(View.VISIBLE);
                    vh.btnBalanceAction.setText(ctx.getString(R.string.remind_debt_btn_text));
                    vh.btnBalanceAction.setOnClickListener(v -> actionListener.onRemind(user, owe));
                } else {
                    vh.btnBalanceAction.setVisibility(View.GONE);
                }
            } else {
                vh.tvBalanceStatus.setText(ctx.getString(R.string.settled_up_status));
                vh.tvBalanceStatus.setTextColor(Color.GRAY);
                vh.btnBalanceAction.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (optimizedTransactions.isEmpty()) return members.size();
        return optimizedTransactions.size() + members.size() + 2;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvUserName);
            itemView.findViewById(R.id.tvAvatarInitial).setVisibility(View.GONE);
            itemView.findViewById(R.id.tvBalanceStatus).setVisibility(View.GONE);
            itemView.findViewById(R.id.viewAvatarBg).setVisibility(View.GONE);
            itemView.findViewById(R.id.btnBalanceAction).setVisibility(View.GONE);
            tvHeader.setTextSize(13);
            tvHeader.setTextColor(Color.DKGRAY);
            tvHeader.setPadding(16, 32, 16, 16);
        }
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