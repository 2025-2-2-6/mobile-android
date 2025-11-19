package com.example.mobile_android.ui.post;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.util.CalendarManager;
import com.example.mobile_android.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final CalendarManager calendarManager;
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        this.calendarManager = new CalendarManager(context);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.tvCategory.setText(resolveCategory(post));
        holder.tvTitle.setText(post.getTitle());
        holder.tvDate.setText(buildEventDateLabel(post));

        holder.badgeNew.setVisibility(shouldShowNewBadge(post) ? View.VISIBLE : View.GONE);

        String ddaySource = resolveDdaySource(post);
        if (!TextUtils.isEmpty(ddaySource)) {
            int daysLeft = getDaysUntil(ddaySource);
            if (daysLeft >= 0 && daysLeft <= 7) {
                holder.badgeDday.setVisibility(View.VISIBLE);
                holder.badgeDday.setText("D-" + daysLeft);
            } else {
                holder.badgeDday.setVisibility(View.GONE);
            }
        } else {
            holder.badgeDday.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(post.getLocation())) {
            holder.tvLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setText("📍 " + post.getLocation());
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        holder.switchCalendar.setOnCheckedChangeListener(null);
        String calendarAnchor = resolveCalendarAnchor(post);
        if (!TextUtils.isEmpty(calendarAnchor)) {
            holder.switchCalendar.setEnabled(true);
            holder.switchCalendar.setChecked(calendarManager.isEventRegistered(post.getId()));
            holder.switchCalendar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    calendarManager.addEventToCalendar(
                            post.getId(),
                            post.getTitle(),
                            calendarAnchor,
                            post.getLocation()
                    );
                } else {
                    calendarManager.removeEventFromCalendar(post.getId());
                }
            });
        } else {
            holder.switchCalendar.setEnabled(false);
            holder.switchCalendar.setChecked(false);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            } else {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("POST_ID", post.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private String resolveCategory(Post post) {
        if (!TextUtils.isEmpty(post.getCategoryName())) {
            return post.getCategoryName();
        }
        if (!TextUtils.isEmpty(post.getSiteName())) {
            return post.getSiteName();
        }
        return context.getString(R.string.app_name);
    }

    private String buildEventDateLabel(Post post) {
        String start = formatDateTime(post.getEventStartDate(), "MM월 dd일 (E) HH:mm");
        String end = formatDateTime(post.getEventEndDate(), "MM월 dd일 (E) HH:mm");
        String single = formatDateTime(post.getEventDate(), "MM월 dd일 (E) HH:mm");

        if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
            return start + " ~ " + end;
        }
        if (!TextUtils.isEmpty(start)) {
            return start;
        }
        if (!TextUtils.isEmpty(single)) {
            return single;
        }
        if (!TextUtils.isEmpty(end)) {
            return end;
        }
        return formatDateTime(post.getCreatedAt(), "MM월 dd일 (E) HH:mm");
    }

    private boolean shouldShowNewBadge(Post post) {
        if (post.getIsNew() != null) {
            return post.getIsNew();
        }
        Date created = DateTimeUtils.parseServerDate(post.getCreatedAt());
        if (created == null) return false;
        long diff = System.currentTimeMillis() - created.getTime();
        return TimeUnit.MILLISECONDS.toHours(diff) <= 24;
    }

    private String resolveDdaySource(Post post) {
        List<String> candidates = new ArrayList<>();
        candidates.add(post.getEventStartDate());
        candidates.add(post.getEventEndDate());
        candidates.add(post.getEventDate());

        long now = System.currentTimeMillis();
        String fallback = null;
        for (String candidate : candidates) {
            Date date = DateTimeUtils.parseServerDate(candidate);
            if (date == null) continue;
            if (fallback == null) {
                fallback = candidate;
            }
            if (date.getTime() >= now) {
                return candidate;
            }
        }
        return fallback;
    }

    private int getDaysUntil(String dateStr) {
        Date eventDate = DateTimeUtils.parseServerDate(dateStr);
        if (eventDate == null) return -1;
        long diff = eventDate.getTime() - System.currentTimeMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }

    private String resolveCalendarAnchor(Post post) {
        if (!TextUtils.isEmpty(post.getEventStartDate())) {
            return post.getEventStartDate();
        }
        if (!TextUtils.isEmpty(post.getEventDate())) {
            return post.getEventDate();
        }
        if (!TextUtils.isEmpty(post.getEventEndDate())) {
            return post.getEventEndDate();
        }
        return null;
    }

    private String formatDateTime(String dateTimeStr, String format) {
        return DateTimeUtils.formatServerDate(dateTimeStr, format);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvTitle;
        TextView tvDate;
        TextView tvLocation;
        TextView badgeNew;
        TextView badgeDday;
        SwitchCompat switchCalendar;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_post_category);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            tvLocation = itemView.findViewById(R.id.tv_post_location);
            badgeNew = itemView.findViewById(R.id.badge_new);
            badgeDday = itemView.findViewById(R.id.badge_dday);
            switchCalendar = itemView.findViewById(R.id.switch_calendar);
        }
    }
}
