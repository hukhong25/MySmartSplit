package vn.haui.smartsplit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import vn.haui.smartsplit.R;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<String> emailList;

    public MemberAdapter(List<String> emailList) {
        this.emailList = emailList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        // Ở đây t dùng tạm layout mặc định của Android cho nhanh, m có thể tạo layout riêng nếu muốn đẹp hơn
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        String email = emailList.get(position);
        ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(email);
    }

    @Override
    public int getItemCount() {
        return emailList.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
