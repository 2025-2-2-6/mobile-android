package com.example.mobile_android.ui.site;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Site;
import com.example.mobile_android.ui.post.PostListActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.SiteViewHolder> {

    private final List<Site> siteList;
    private final boolean manageMode;   // true = 등록 사이트 관리 화면
    private boolean selectionMode = false; // true = 체크박스 보이기

    // 선택된 사이트 id 집합
    private final Set<String> selectedIds = new HashSet<>();

    public interface OnSiteDeleteListener {
        void onDelete(Site site);
    }

    public interface OnItemClickListener {
        void onItemClick(Site site);
    }

    private OnSiteDeleteListener deleteListener;
    private OnItemClickListener itemClickListener;

    // 홈 화면에서 사용
    public SiteAdapter(List<Site> siteList) {
        this(siteList, false);
    }

    // 관리 화면에서 사용
    public SiteAdapter(List<Site> siteList, boolean manageMode) {
        this.siteList = siteList;
        this.manageMode = manageMode;
    }

    public void setOnDeleteListener(OnSiteDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    /** 선택 모드 on/off */
    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!enabled) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    /** 전체 선택 */
    public void selectAll() {
        selectedIds.clear();
        for (Site s : siteList) {
            if (s.getId() != null) {
                selectedIds.add(s.getId());
            }
        }
        notifyDataSetChanged();
    }

    /** 선택 해제 */
    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    /** 선택된 사이트 목록 */
    public List<Site> getSelectedSites() {
        List<Site> list = new ArrayList<>();
        for (Site s : siteList) {
            if (s.getId() != null && selectedIds.contains(s.getId())) {
                list.add(s);
            }
        }
        return list;
    }

    /** 한 아이템 토글 */
    private void toggleSelect(Site site) {
        if (site.getId() == null) return;
        if (selectedIds.contains(site.getId())) {
            selectedIds.remove(site.getId());
        } else {
            selectedIds.add(site.getId());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_site, parent, false);
        return new SiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteViewHolder holder, int position) {
        Site site = siteList.get(position);

        holder.siteName.setText(site.getName());
        holder.siteUrl.setText(site.getUrl());
        holder.categoryTag.setText(site.getCategory() != null ? site.getCategory() : "");
        holder.lastUpdated.setText("• " + (site.getUpdatedAt() != null ? site.getUpdatedAt() : ""));

        // 새 글 뱃지는 일단 숨김
        holder.newPostBadge.setVisibility(View.GONE);

        // ---- 선택 모드/관리 모드에 따른 UI ----
        if (manageMode && selectionMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            boolean checked = site.getId() != null && selectedIds.contains(site.getId());
            holder.checkBox.setChecked(checked);
        } else {
            holder.checkBox.setVisibility(View.GONE);
        }

        // 관리 화면이 아니면 연필/휴지통 숨기고, 카드 클릭 시 바로 PostList 로
        if (!manageMode) {
            holder.editButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, PostListActivity.class);
                intent.putExtra("SITE_ID", site.getId());
                intent.putExtra("SITE_NAME", site.getName());
                context.startActivity(intent);
            });
            return;
        }

        // --- 여기부터는 "등록 사이트 관리" 화면 전용 ---

        // 카드 클릭
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                // 선택 모드면 체크 토글
                toggleSelect(site);
            } else {
                // 선택 모드 아니면 상세보기 콜백
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(site);
                }
            }
        });

        // 체크박스 직접 눌렀을 때
        holder.checkBox.setOnClickListener(v -> toggleSelect(site));

        // 연필 = 상세 수정/보기
        holder.editButton.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(site);
            }
        });

        // 휴지통 = 개별 삭제
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(site);
            }
        });
    }

    @Override
    public int getItemCount() {
        return siteList.size();
    }

    static class SiteViewHolder extends RecyclerView.ViewHolder {

        CheckBox checkBox;
        TextView siteName, siteUrl, categoryTag, lastUpdated, newPostBadge;
        ImageView editButton, deleteButton;

        SiteViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cb_select);
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
