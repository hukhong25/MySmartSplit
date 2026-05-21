package vn.haui.smartsplit.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

        boolean targetIsAdmin = user.getUid().equals(adminId);
        boolean viewerIsAdmin = currentUserId.equals(adminId);

        // Hiển thị thông tin tên và vai trò công khai
        holder.tvName.setText(user.getUid().equals(currentUserId) ? user.getName() + " (Bạn)" : user.getName());
        holder.tvRole.setText(targetIsAdmin ? "Trưởng nhóm" : "Thành viên");

        // Tạo avatar ký tự đầu
        holder.tvInitial.setText(user.getName() != null && !user.getName().isEmpty()
                ? String.valueOf(user.getName().charAt(0)).toUpperCase() : "?");

        // Đổ màu nền ngẫu nhiên cho vòng tròn Avatar tinh tế hơn
        if (holder.viewAvatarBg.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) holder.viewAvatarBg.getBackground();
            drawable.setColor(COLORS[position % COLORS.length]);
        } else {
            holder.viewAvatarBg.setBackgroundColor(COLORS[position % COLORS.length]);
        }

        // LOGIC ẨN HIỆN CỤM NÚT SỬA / XÓA:
        // Chỉ hiện lên nếu người đang xem màn hình là Admin và dòng hiện tại KHÔNG PHẢI là chính bản thân Admin đó
        if (viewerIsAdmin && !targetIsAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnRemove.setVisibility(View.VISIBLE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnRemove.setVisibility(View.GONE);
        }

        // BẮT SỰ KIỆN CLICK TRUYỀN NGƯỢC VỀ ACTIVITY XỬ LÝ DIALOG
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
        ImageButton btnEdit, btnRemove; // Khai báo thêm nút Sửa

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvRole = itemView.findViewById(R.id.tvMemberRole);
            tvInitial = itemView.findViewById(R.id.tvAvatarInitial);
            viewAvatarBg = itemView.findViewById(R.id.viewAvatarBg);

            // Ánh xạ chính xác theo ID trong file layout XML mới của bạn
            btnEdit = itemView.findViewById(R.id.btnEditMember);
            btnRemove = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}