package com.example.mobile_android;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.SiteViewHolder> {

    private List<Site> siteList;

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
        holder.categoryTag.setText(site.getCategory());
        holder.siteUrl.setText(site.getUrl());
        holder.lastUpdated.setText("• " + site.getLastUpdated());

        if (site.getNewPosts() > 0) {
            holder.newPostBadge.setText(site.getNewPosts() + " 새글");
            holder.newPostBadge.setVisibility(View.VISIBLE);
        } else {
            holder.newPostBadge.setVisibility(View.GONE);
        }

        // [추가] 각 아이템 뷰에 클릭 리스너 설정
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PostListActivity.class);
            // 다음 액티비티로 사이트 이름을 전달
            intent.putExtra("SITE_NAME", site.getName());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    static class SiteViewHolder extends RecyclerView.ViewHolder {
        TextView siteName, newPostBadge, categoryTag, lastUpdated, siteUrl;

        public SiteViewHolder(@NonNull View itemView) {
            super(itemView);
            siteName = itemView.findViewById(R.id.tv_site_name);
            newPostBadge = itemView.findViewById(R.id.tv_new_post_badge);
            categoryTag = itemView.findViewById(R.id.tv_category_tag);
            lastUpdated = itemView.findViewById(R.id.tv_last_updated);
            siteUrl = itemView.findViewById(R.id.tv_site_url);
        }
    }
}
