package com.example.mobile_android.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_android.R;
import java.util.List;

public class TagChipAdapter extends RecyclerView.Adapter<TagChipAdapter.VH> {

    public interface OnTagClick {
        void onClick(int position, TagChip item);
    }

    private final List<TagChip> items;
    private final OnTagClick listener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public TagChipAdapter(List<TagChip> items, OnTagClick listener) {
        this.items = items;
        this.listener = listener;
        // 초기 선택값: 첫 번째가 true면 반영
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).selected) { selectedPos = i; break; }
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tag_chip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TagChip it = items.get(position);
        h.tvLabel.setText(it.label);
        h.tvCount.setText(String.valueOf(it.count));

        h.root.setSelected(it.selected);
        h.tvLabel.setSelected(it.selected);
        h.tvCount.setSelected(it.selected);

        h.root.setOnClickListener(v -> {
            int currentPosition = h.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                return;
            }
            
            if (selectedPos != RecyclerView.NO_POSITION && selectedPos != currentPosition) {
                items.get(selectedPos).selected = false;
                notifyItemChanged(selectedPos);
            }
            it.selected = true;
            notifyItemChanged(currentPosition);
            selectedPos = currentPosition;
            if (listener != null) listener.onClick(currentPosition, it);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView tvLabel, tvCount;
        VH(@NonNull View itemView) {
            super(itemView);
            root = (LinearLayout) itemView;
            tvLabel = itemView.findViewById(R.id.tvLabel);
            tvCount = itemView.findViewById(R.id.tvCount);
        }
    }
}
