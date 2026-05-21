package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private final OnMemberActionListener listener;

    private static final int[] COLORS = {
            Color.parseColor("#1CC29F"), Color.parseColor("#3B82F6"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#F59E0B")
    };

    public interface OnMemberActionListener {
        void onEditMember(User user);
        void onRemoveMember(User user);
    }

    public MemberAdapter(List<User> members, String adminId, String currentUserId, OnMemberActionListener listener) {
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

        android.content.Context ctx = holder.itemView.getContext();
        boolean targetIsAdmin = user.getUid().equals(adminId);
        boolean viewerIsAdmin = currentUserId.equals(adminId);

        // Hiển thị thông tin tên (Sử dụng string format động để đính kèm chữ "Bạn")
        String displayName = user.getName();
        if (user.getUid().equals(currentUserId)) {
            holder.tvName.setText(ctx.getString(R.string.username_with_you_suffix, displayName));
        } else {
            holder.tvName.setText(displayName);
        }

        // Hiển thị vai trò (Trưởng nhóm / Thành viên) từ tài nguyên đa ngôn ngữ
        String roleText = targetIsAdmin
                ? ctx.getString(R.string.role_admin)
                : ctx.getString(R.string.role_member);
        holder.tvRole.setText(roleText);

        // Đổ màu nền ngẫu nhiên cho vòng tròn Avatar tinh tế hơn
        if (holder.viewAvatarBg.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) holder.viewAvatarBg.getBackground();
            drawable.setColor(COLORS[position % COLORS.length]);
        } else {
            holder.viewAvatarBg.setBackgroundColor(COLORS[position % COLORS.length]);
        }

        // Load avatar nếu có
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

            String name = user.getName();
            holder.tvInitial.setText(name != null && !name.isEmpty()
                    ? String.valueOf(name.charAt(0)).toUpperCase()
                    : ctx.getString(R.string.default_initial_question_mark));
        }

        if (viewerIsAdmin && !targetIsAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnRemove.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnRemove.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditMember(user);
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveMember(user);
        });
    }

    @Override
    public int getItemCount() { return members.size(); }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvInitial;
        View viewAvatarBg;
        ImageView ivAvatar;
        ImageButton btnEdit, btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvRole = itemView.findViewById(R.id.tvMemberRole);
            tvInitial = itemView.findViewById(R.id.tvAvatarInitial);
            viewAvatarBg = itemView.findViewById(R.id.viewAvatarBg);
            ivAvatar = itemView.findViewById(R.id.ivMemberAvatar);
            btnEdit = itemView.findViewById(R.id.btnEditMember);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}