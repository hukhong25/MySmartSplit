package vn.haui.smartsplit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.User;

public class SplitMemberAdapter extends RecyclerView.Adapter<SplitMemberAdapter.MemberViewHolder> {

    private List<User> members;
    private List<String> selectedUserIds;

    public SplitMemberAdapter(List<User> members, List<String> selectedUserIds) {
        this.members = members;
        this.selectedUserIds = selectedUserIds;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_split_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = members.get(position);
        holder.tvMemberName.setText(user.getName());
        
        holder.cbSelectMember.setOnCheckedChangeListener(null);
        holder.cbSelectMember.setChecked(selectedUserIds.contains(user.getUid()));
        
        holder.cbSelectMember.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedUserIds.contains(user.getUid())) {
                    selectedUserIds.add(user.getUid());
                }
            } else {
                selectedUserIds.remove(user.getUid());
            }
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectMember;
        TextView tvMemberName;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectMember = itemView.findViewById(R.id.cbSelectMember);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
        }
    }
}
