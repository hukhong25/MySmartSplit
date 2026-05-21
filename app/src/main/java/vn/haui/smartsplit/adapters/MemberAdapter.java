package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import vn.haui.smartsplit.R;
import vn.haui.smartsplit.models.User;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private final List<User> members;
    private final String adminId;
    private final String currentUserId;
    private final OnMemberClickListener listener;

    private static final int[] COLORS = {
            Color.parseColor("#1CC29F"), Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#F59E0B")
    };

    public interface OnMemberClickListener {
        void onRemoveMember(User user);
    }

    public MemberAdapter(List<User> members, String adminId, String currentUserId, OnMemberClickListener listener) {
        this.members = members;
        this.adminId = adminId;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_manage, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = members.get(position);
        if (user == null) return;
        
        boolean targetIsAdmin = user.getUid().equals(adminId);
        boolean viewerIsAdmin = currentUserId.equals(adminId);

        holder.tvName.setText(user.getName());
        holder.tvRole.setText(targetIsAdmin ? "Trưởng nhóm" : "Thành viên");
        
        String name = user.getName();
        holder.tvInitial.setText(name != null && !name.isEmpty() 
                ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
        holder.viewAvatarBg.setBackgroundColor(COLORS[position % COLORS.length]);

        // Load avatar if exists
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.tvInitial.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvInitial.setVisibility(View.VISIBLE);
        }

        // Chỉ hiện nút xóa nếu người đang xem là Admin và không được tự xóa chính mình
        holder.btnRemove.setVisibility(viewerIsAdmin && !targetIsAdmin ? View.VISIBLE : View.GONE);
        holder.btnRemove.setOnClickListener(v -> listener.onRemoveMember(user));
    }

    @Override
    public int getItemCount() { return members.size(); }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvInitial;
        View viewAvatarBg;
        ImageView ivAvatar;
        ImageButton btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvRole = itemView.findViewById(R.id.tvMemberRole);
            tvInitial = itemView.findViewById(R.id.tvAvatarInitial);
            viewAvatarBg = itemView.findViewById(R.id.viewAvatarBg);
            ivAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}
