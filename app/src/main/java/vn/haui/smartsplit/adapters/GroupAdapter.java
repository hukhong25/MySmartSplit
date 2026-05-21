package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Group;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder>
        implements Filterable {

    private List<Group> groupList;
    private List<Group> groupListFull;
    private final OnGroupClickListener listener;

    // Background colors for group icon (cycle through)
    private static final int[] ICON_COLORS = {
            Color.parseColor("#1CC29F"),
            Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#EC4899")
    };

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public GroupAdapter(List<Group> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.groupListFull = new ArrayList<>(groupList);
        this.listener = listener;
    }

    public void updateFullList(List<Group> newList) {
        groupListFull = new ArrayList<>(newList);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        holder.tvGroupName.setText(group.getName());
        int memberCount = group.getMemberIds() != null ? group.getMemberIds().size() : 0;

        // Sử dụng chuỗi định dạng động cho số lượng thành viên
        holder.tvMemberCount.setText(ctx.getString(R.string.group_member_count_format, memberCount));

        // Default balance display (no per-user balance in Group model currently)
        holder.tvGroupBalance.setText(ctx.getString(R.string.default_currency_zero));
        holder.tvGroupStatus.setText(ctx.getString(R.string.status_settled_up));
        holder.tvGroupStatus.setTextColor(Color.parseColor("#9E9E9E"));
        if (holder.viewStatusDot != null) {
            holder.viewStatusDot.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }

        // Rotate icon background color
        int colorIdx = position % ICON_COLORS.length;
        if (holder.ivGroupIcon != null) {
            holder.ivGroupIcon.setColorFilter(ICON_COLORS[colorIdx]);
        }

        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group));
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    @Override
    public Filter getFilter() {
        return groupFilter;
    }

    private final Filter groupFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Group> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(groupListFull);
            } else {
                String pattern = constraint.toString().toLowerCase().trim();
                for (Group group : groupListFull) {
                    if (group.getName() != null &&
                            group.getName().toLowerCase().contains(pattern)) {
                        filteredList.add(group);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            groupList.clear();
            groupList.addAll((List<Group>) results.values);
            notifyDataSetChanged();
        }
    };

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvMemberCount, tvGroupBalance, tvGroupStatus;
        ImageView ivGroupIcon;
        View viewStatusDot;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName    = itemView.findViewById(R.id.tvGroupName);
            tvMemberCount  = itemView.findViewById(R.id.tvMemberCount);
            tvGroupBalance = itemView.findViewById(R.id.tvGroupBalance);
            tvGroupStatus  = itemView.findViewById(R.id.tvGroupStatus);
            ivGroupIcon    = itemView.findViewById(R.id.ivGroupIcon);
            viewStatusDot  = itemView.findViewById(R.id.viewStatusDot);
        }
    }
}