package com.example.mobile_android.ui.post;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.util.CalendarEventHelper;
import com.example.mobile_android.util.DateTimeUtils;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<Post> postList;
    private final PostDao postDao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        this.postDao = AppDatabase.getInstance(context).postDao();
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
            holder.tvLocation.setText(post.getLocation());
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        holder.switchCalendar.setOnCheckedChangeListener(null);
        String calendarAnchor = resolveCalendarAnchor(post);
        if (!TextUtils.isEmpty(calendarAnchor)) {
            holder.notificationArea.setVisibility(View.VISIBLE);
            holder.switchCalendar.setEnabled(true);
            // 이제 데이터베이스의 isSaved 필드를 사용해 토글 상태를 결정
            holder.switchCalendar.setChecked(post.isSaved());
            bindCalendarStatus(holder, post.isSaved());
            holder.switchCalendar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Add to calendar
                    CalendarEventHelper.addPostToCalendar(context, post,
                        new CalendarEventHelper.OnCalendarEventCallback() {
                            @Override
                            public void onSuccess() {
                                post.setSaved(true);
                                bindCalendarStatus(holder, true);
                                // Update database
                                databaseExecutor.execute(() -> {
                                    postDao.updateSaveState(post.getId(), true);
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                // Revert UI on failure
                                post.setSaved(false);
                                holder.switchCalendar.setChecked(false);
                                bindCalendarStatus(holder, false);
                                Toast.makeText(context, "캘린더 추가 실패: " + error,
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    // Remove from calendar
                    CalendarEventHelper.removePostFromCalendar(context, post,
                        new CalendarEventHelper.OnCalendarEventCallback() {
                            @Override
                            public void onSuccess() {
                                post.setSaved(false);
                                bindCalendarStatus(holder, false);
                                // Update database
                                databaseExecutor.execute(() -> {
                                    postDao.updateSaveState(post.getId(), false);
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                // Revert UI on failure
                                post.setSaved(true);
                                holder.switchCalendar.setChecked(true);
                                bindCalendarStatus(holder, true);
                                Toast.makeText(context, "캘린더 제거 실패: " + error,
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            });
        } else {
            holder.notificationArea.setVisibility(View.GONE);
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
        // Priority: category > categoryName > siteName > app name
        if (!TextUtils.isEmpty(post.getCategory())) {
            return post.getCategory();
        }
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
        // Post 모델의 isActuallyNew() 메서드를 사용하여 일관성 유지
        boolean result = post.isActuallyNew();

        // 디버깅 로그 (DEBUG 빌드에서만)
        if (com.example.mobile_android.BuildConfig.DEBUG) {
            android.util.Log.d("PostAdapter", "shouldShowNewBadge - " +
                "Title: " + post.getTitle() +
                ", is_new(backend): " + post.getIsNew() +
                ", created_at: " + post.getCreatedAt() +
                ", result: " + result);
        }

        return result;
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

    private void bindCalendarStatus(PostViewHolder holder, boolean isSaved) {
        // notification bell 아이콘 사용: 활성화는 파란색, 비활성화는 회색
        holder.calendarStatusIcon.setImageResource(isSaved ? R.drawable.ic_bell_enabled : R.drawable.ic_bell_disabled);
        // 아이콘 자체에 색상이 포함되어 있으므로 ColorFilter는 제거
        holder.calendarStatusIcon.setColorFilter(null);

        // 텍스트 색상 및 내용 업데이트
        int textColor = ContextCompat.getColor(context, isSaved ? R.color.calendar_status_on : R.color.calendar_status_off);
        holder.calendarStatusText.setText(isSaved ? "알림 켜짐" : "알림 켜기");
        holder.calendarStatusText.setTextColor(textColor);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvTitle;
        TextView tvDate;
        TextView tvLocation;
        TextView badgeNew;
        TextView badgeDday;
        ImageView calendarStatusIcon;
        TextView calendarStatusText;
        SwitchCompat switchCalendar;
        View notificationArea;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_post_category);
            tvTitle = itemView.findViewById(R.id.tv_post_title);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            tvLocation = itemView.findViewById(R.id.tv_post_location);
            badgeNew = itemView.findViewById(R.id.badge_new);
            badgeDday = itemView.findViewById(R.id.badge_dday);
            calendarStatusIcon = itemView.findViewById(R.id.iv_notification_icon);
            calendarStatusText = itemView.findViewById(R.id.tv_calendar_status);
            switchCalendar = itemView.findViewById(R.id.switch_calendar);
            notificationArea = itemView.findViewById(R.id.notification_area);
        }
    }
}
