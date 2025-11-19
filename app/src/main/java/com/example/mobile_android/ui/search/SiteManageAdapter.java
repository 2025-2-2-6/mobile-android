// app/src/main/java/com/example/mobile_android/ui/search/SiteManageAdapter.java
package com.example.mobile_android.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.SiteSummary;

import java.util.List;

public class SiteManageAdapter extends RecyclerView.Adapter<SiteManageAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(SiteSummary site);
    }

    private final List<SiteSummary> data;
    private final OnItemClickListener listener;

    public SiteManageAdapter(List<SiteSummary> data, OnItemClickListener listener) {
        this.data = data;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvBadge, tvStats;
        Button btnPrimary;

        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_title);
            tvDesc = v.findViewById(R.id.tv_desc);
            tvBadge = v.findViewById(R.id.tv_badge);
            tvStats = v.findViewById(R.id.tv_stats);
            btnPrimary = v.findViewById(R.id.btn_primary);
        }

        void bind(SiteSummary s, OnItemClickListener listener) {
            itemView.setOnClickListener(v -> listener.onClick(s));
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_site_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SiteSummary s = data.get(position);
        h.tvTitle.setText(s.name);
        h.tvDesc.setText(s.description != null ? s.description : "");
        h.tvBadge.setText(s.category != null ? s.category : "");
        h.tvStats.setText(s.subscriber_text != null ? s.subscriber_text : "");
        h.btnPrimary.setText(s.subscribed ? "등록됨" : "+ 추가");
        h.bind(s, listener);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
