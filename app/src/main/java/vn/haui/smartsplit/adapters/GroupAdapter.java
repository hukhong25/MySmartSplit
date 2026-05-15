package vn.haui.smartsplit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.Group;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> implements Filterable {

    private List<Group> groupList;         // danh sách hiện tại (đã lọc)
    private List<Group> groupListFull;     // danh sách gốc đầy đủ
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public GroupAdapter(List<Group> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.groupListFull = new ArrayList<>(groupList);
        this.listener = listener;
    }

    /**
     * Gọi sau khi dữ liệu từ Firestore được nạp lại để đồng bộ danh sách gốc.
     */
    public void updateFullList(List<Group> newList) {
        groupListFull = new ArrayList<>(newList);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.tvGroupName.setText(group.getName());
        holder.tvMemberCount.setText(group.getMemberIds().size() + " thành viên");
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
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Group group : groupListFull) {
                    if (group.getName() != null &&
                            group.getName().toLowerCase().contains(filterPattern)) {
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
        TextView tvGroupName, tvMemberCount;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
        }
    }
}
