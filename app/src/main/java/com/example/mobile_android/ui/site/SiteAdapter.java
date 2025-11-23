package com.example.mobile_android.ui.site;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.SiteViewHolder> {

    private List<Site> siteList;

    public interface OnSiteDeleteListener {
        void onDelete(Site site);
    }

    public interface OnSiteEditListener {
        void onEdit(Site site);
    }

    private OnSiteDeleteListener deleteListener;
    private OnSiteEditListener editListener;

    public void setOnDeleteListener(OnSiteDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnEditListener(OnSiteEditListener listener) {
        this.editListener = listener;
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

        // 사이트 이름이 비어있으면 URL을 표시
        String displayName = TextUtils.isEmpty(site.getName()) ? site.getUrl() : site.getName();
        holder.siteName.setText(displayName);
        holder.siteUrl.setText(site.getUrl());

        // 카테고리가 없으면 "기타"로 표시
        String category = TextUtils.isEmpty(site.getCategory()) ? "기타" : site.getCategory();
        holder.categoryTag.setText(category);

        String formattedDate = formatDate(site.getUpdatedAt());
        if (TextUtils.isEmpty(formattedDate)) {
            holder.lastUpdated.setText("업데이트 정보 없음");
        } else {
            holder.lastUpdated.setText("업데이트 · " + formattedDate);
        }

        // newPosts 같은 값은 백엔드에 아직 없음 → 숨기기
        holder.newPostBadge.setVisibility(View.GONE);

        // 클릭 시 PostListActivity 로 이동
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PostListActivity.class);
            intent.putExtra("SITE_ID", site.getId());
            intent.putExtra("SITE_NAME", displayName);
            context.startActivity(intent);
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(site);
        });

        holder.editButton.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(site);
        });
    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    /**
     * ISO 8601 날짜를 "년월일 시분초" 형식으로 포맷합니다.
     * 예: "2025-11-22T10:30:00Z" -> "2025년 11월 22일 10:30:00"
     */
    private String formatDate(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) {
            return "";
        }

        try {
            // ISO 8601 형식 파싱
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.KOREA);
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(isoDate);

            if (date == null) {
                return "";
            }

            // 한국 시간대로 변환하여 포맷
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 M월 d일 HH시 mm분", Locale.KOREA);
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    static class SiteViewHolder extends RecyclerView.ViewHolder {

        TextView siteName, siteUrl, categoryTag, totalPosts, lastUpdated, newPostBadge;
        ImageView editButton, deleteButton;

        public SiteViewHolder(@NonNull View itemView) {
            super(itemView);

            siteName = itemView.findViewById(R.id.tv_site_name);
            siteUrl = itemView.findViewById(R.id.tv_site_url);
            categoryTag = itemView.findViewById(R.id.tv_category_tag);
            totalPosts = itemView.findViewById(R.id.tv_total_posts);
            lastUpdated = itemView.findViewById(R.id.tv_last_updated);
            newPostBadge = itemView.findViewById(R.id.tv_new_posts_badge);
            editButton = itemView.findViewById(R.id.btn_edit);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}
