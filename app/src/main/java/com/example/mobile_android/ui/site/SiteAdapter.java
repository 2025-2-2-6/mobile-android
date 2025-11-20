package com.example.mobile_android.ui.site;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.ui.post.PostListActivity;

import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.SiteViewHolder> {

    private List<Site> siteList;

    public interface OnSiteDeleteListener {
        void onDelete(Site site);
    }
    private OnSiteDeleteListener deleteListener;

    public void setOnDeleteListener(OnSiteDeleteListener listener) {
        this.deleteListener = listener;
    }
    public SiteAdapter(List<Site> siteList) {
        this.siteList = siteList;
    }

    @NonNull
    @Override
    public SiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_site, parent, false);
        return new SiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteViewHolder holder, int position) {
        Site site = siteList.get(position);

        holder.siteName.setText(site.getName());
        holder.siteUrl.setText(site.getUrl());
        holder.categoryTag.setText(site.getCategory());
        holder.lastUpdated.setText("• " + site.getUpdatedAt());

        // newPosts 같은 값은 백엔드에 아직 없음 → 숨기기
        holder.newPostBadge.setVisibility(View.GONE);

        // 클릭 시 PostListActivity 로 이동
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PostListActivity.class);
            intent.putExtra("SITE_ID", site.getId());
            intent.putExtra("SITE_NAME", site.getName());
            context.startActivity(intent);
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(site);
        });

    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    static class SiteViewHolder extends RecyclerView.ViewHolder {

        TextView siteName, siteUrl, categoryTag, lastUpdated, newPostBadge;
        ImageView editButton, deleteButton;

        public SiteViewHolder(@NonNull View itemView) {
            super(itemView);

            siteName = itemView.findViewById(R.id.tv_site_name);
            siteUrl = itemView.findViewById(R.id.tv_site_url);
            categoryTag = itemView.findViewById(R.id.tv_category_tag);
            lastUpdated = itemView.findViewById(R.id.tv_last_updated);
            newPostBadge = itemView.findViewById(R.id.tv_new_post_badge);
            editButton = itemView.findViewById(R.id.iv_edit);
            deleteButton = itemView.findViewById(R.id.iv_delete);
        }
    }
}
