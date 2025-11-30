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
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Site;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.SiteViewHolder> {

    private List<Site> siteList;
    private Context context;
    private PostDao postDao;

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

    public SiteAdapter(Context context, List<Site> siteList) {
        this.context = context;
        this.siteList = siteList;
        this.postDao = AppDatabase.getInstance(context).postDao();
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

        // 사이트 이름 표시 (항상 name 필드 사용, null이면 "이름 없음"으로 표시)
        String displayName = !TextUtils.isEmpty(site.getName()) ? site.getName() : "이름 없음";
        holder.siteName.setText(displayName);
        holder.siteUrl.setText(site.getUrl());

        // 카테고리가 없으면 "기타"로 표시
        String category = TextUtils.isEmpty(site.getCategory()) ? "기타" : site.getCategory();
        holder.categoryTag.setText(category);

        // 마지막 크롤링 날짜 표시 (게시물의 최근 날짜 우선, 없으면 사이트 날짜 사용)
        new Thread(() -> {
            // 1. 먼저 게시물의 최근 날짜 확인
            String latestPostDate = postDao.getLatestPostDateBySiteSync(site.getId());

            // 2. 표시할 날짜 결정 (우선순위: 게시물 날짜 > 사이트 updatedAt > 사이트 createdAt)
            String dateToDisplay = null;
            if (!TextUtils.isEmpty(latestPostDate)) {
                dateToDisplay = latestPostDate;
                android.util.Log.d("SiteAdapter", "게시물 날짜 사용: " + latestPostDate);
            } else if (!TextUtils.isEmpty(site.getUpdatedAt())) {
                dateToDisplay = site.getUpdatedAt();
                android.util.Log.d("SiteAdapter", "사이트 updatedAt 사용: " + site.getUpdatedAt());
            } else if (!TextUtils.isEmpty(site.getCreatedAt())) {
                dateToDisplay = site.getCreatedAt();
                android.util.Log.d("SiteAdapter", "사이트 createdAt 사용: " + site.getCreatedAt());
            } else {
                android.util.Log.e("SiteAdapter", "날짜 정보 없음 - Site ID: " + site.getId());
            }

            // 3. UI 업데이트 (메인 스레드)
            String finalDateToDisplay = dateToDisplay;
            holder.itemView.post(() -> {
                if (!TextUtils.isEmpty(finalDateToDisplay)) {
                    String formattedDate = formatDate(finalDateToDisplay);
                    if (!TextUtils.isEmpty(formattedDate)) {
                        holder.lastUpdated.setText("마지막 수집 · " + formattedDate);
                    } else {
                        android.util.Log.e("SiteAdapter", "formatDate 실패: " + finalDateToDisplay);
                        holder.lastUpdated.setText("수집 정보 없음");
                    }
                } else {
                    holder.lastUpdated.setText("수집 정보 없음");
                }
            });
        }).start();

        // 게시물 수 표시 (백그라운드 스레드에서 조회)
        holder.totalPosts.setText("총 0개");  // 기본값
        new Thread(() -> {
            int postCount = postDao.getPostCountBySiteSync(site.getId());
            // UI 업데이트는 메인 스레드에서
            holder.itemView.post(() -> {
                holder.totalPosts.setText("총 " + postCount + "개");
            });
        }).start();

        // newPosts 같은 값은 백엔드에 아직 없음 → 숨기기
        holder.newPostBadge.setVisibility(View.GONE);

        // 클릭 시 SiteDetailActivity로 이동
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, SiteDetailActivity.class);
            intent.putExtra("SITE_ID", site.getId());
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
     * 파싱 실패 시 원본 날짜의 앞 10자리 반환
     */
    private String formatDate(String isoDate) {
        if (TextUtils.isEmpty(isoDate)) {
            return "";
        }

        try {
            // ISO 8601 형식 파싱 시도
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.KOREA);
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(isoDate);

            if (date == null) {
                // 파싱 실패 시 원본의 앞 10자리 반환 (yyyy-MM-dd)
                return isoDate.length() >= 10 ? isoDate.substring(0, 10) : isoDate;
            }

            // 한국 시간대로 변환하여 포맷
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 M월 d일 HH시 mm분", Locale.KOREA);
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            // 파싱 실패 시 원본의 앞 10자리 반환 (yyyy-MM-dd)
            android.util.Log.w("SiteAdapter", "날짜 파싱 실패, 원본 사용: " + isoDate, e);
            return isoDate.length() >= 10 ? isoDate.substring(0, 10) : isoDate;
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
