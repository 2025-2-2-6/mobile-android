package com.example.mobile_android.ui.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.NotificationEntity;
import com.example.mobile_android.util.DateTimeUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.example.mobile_android.fcm.NotificationType;
import com.example.mobile_android.fcm.CrawlStatus;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NOTIFICATION = 0;
    private static final int VIEW_TYPE_TIP = 1;

    private final Context context;
    private final List<NotificationEntity> notifications = new ArrayList<>();
    private final EnumMap<NotificationType, NotificationStyle> styleMap = new EnumMap<>(NotificationType.class);

    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationEntity notification);
        void onDeleteClick(NotificationEntity notification);
    }

    public NotificationAdapter(Context context) {
        this.context = context;
        // 일정 알림만 styleMap에 넣고, crawl은 동적으로 처리
        styleMap.put(NotificationType.CALENDAR_REMINDER, new NotificationStyle(
                R.string.notification_label_calendar,
                android.R.drawable.ic_menu_my_calendar,
                R.color.notification_unread_calendar,
                R.color.notification_label_calendar,
                R.color.notification_label_bg_calendar
        ));
        styleMap.put(NotificationType.UNKNOWN, new NotificationStyle(
                R.string.notification_label_unknown,
                android.R.drawable.ic_dialog_info,
                R.color.notification_unread_unknown,
                R.color.notification_label_unknown,
                R.color.notification_label_bg_unknown
        ));
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<NotificationEntity> items) {
        notifications.clear();
        if (items != null) {
            notifications.addAll(items.subList(0, Math.min(items.size(), 5))); // 최대 5개로 제한
        }
        notifyDataSetChanged();
    }

    public void submitDummyData() {
        List<NotificationEntity> dummyNotifications = new ArrayList<>();
        // image1과 동일한 더미 데이터 (백엔드 구조 반영)
        NotificationEntity notif1 = new NotificationEntity("1", "calendar_reminder", "일정 알림", "2024 전국 창업 공모전 마감까지 D-2일 남았습니다", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
        dummyNotifications.add(notif1);

        NotificationEntity notif2 = new NotificationEntity("2", "crawl_new_posts", "새 게시물", "React 공식 사이트에서 새로운 게시물이 수집되었습니다", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        notif2.setCrawlStatus("new_post");
        dummyNotifications.add(notif2);

        NotificationEntity notif3 = new NotificationEntity("3", "crawl_new_posts", "수집 완료", "서울대학교 공지사항 수집이 정상 완료되었습니다", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));
        notif3.setCrawlStatus("success");
        dummyNotifications.add(notif3);

        NotificationEntity notif4 = new NotificationEntity("4", "crawl_new_posts", "수집 실패", "창업진흥원 사이트 수집 중 오류가 발생했습니다", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5));
        notif4.setCrawlStatus("failed");
        dummyNotifications.add(notif4);

        NotificationEntity notif5 = new NotificationEntity("5", "unknown", "알 수 없는 상태", "일부 사이트의 수집 상태를 확인할 수 없습니다", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7));
        dummyNotifications.add(notif5);

        submitList(dummyNotifications);
    }

    @Override
    public int getItemCount() {
        return notifications.size() + 1; // include tip card
    }

    @Override
    public int getItemViewType(int position) {
        return position == notifications.size() ? VIEW_TYPE_TIP : VIEW_TYPE_NOTIFICATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_TIP) {
            View view = inflater.inflate(R.layout.item_notification_tip, parent, false);
            return new TipViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_TIP) {
            return;
        }

        NotificationEntity notification = notifications.get(position);
        NotificationViewHolder vh = (NotificationViewHolder) holder;

        vh.tvTitle.setText(notification.getTitle());
        vh.tvBody.setText(notification.getMessage());
        vh.tvTime.setText(getRelativeTimeString(notification));

        NotificationStyle style = resolveStyle(notification);
        int labelTextColor = ContextCompat.getColor(context, style.labelTextColorRes);
        int labelBgColor = ContextCompat.getColor(context, style.labelBgColorRes);
        int unreadBgColor = ContextCompat.getColor(context, style.unreadColorRes);
        int cardColor = notification.isRead()
                ? ContextCompat.getColor(context, R.color.notification_bg_read)
                : unreadBgColor;

        vh.cardView.setCardBackgroundColor(cardColor);
        vh.cardView.setCardElevation(notification.isRead() ? dpToPx(2) : dpToPx(8));
        vh.cardView.setStrokeWidth(0);

        vh.leftColorIndicator.setBackgroundColor(labelTextColor);

        vh.tvLabel.setText(context.getString(style.labelTextRes));
        vh.tvLabel.setBackgroundColor(labelBgColor);
        vh.tvLabel.setTextColor(labelTextColor);

        vh.itemView.setOnClickListener(v -> {
            // 읽음 처리
            if (!notification.isRead()) {
                notification.setRead(true);
                notifyItemChanged(position);
            }
            // 항상 네비게이션 처리 (읽음 여부와 상관없이)
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
        vh.itemView.setClickable(true);

        vh.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(notification);
            }
        });
    }

    private NotificationStyle resolveStyle(NotificationEntity notification) {
        NotificationType type = NotificationType.fromString(notification.getType());

        // crawl_new_posts인 경우 status에 따라 동적으로 스타일 결정
        if (type == NotificationType.CRAWL_NEW_POSTS) {
            CrawlStatus status = CrawlStatus.fromString(notification.getCrawlStatus());
            switch (status) {
                case NEW_POST:
                    return new NotificationStyle(
                            R.string.notification_label_new,
                            android.R.drawable.ic_input_add,
                            R.color.notification_unread_new,
                            R.color.notification_label_new,
                            R.color.notification_label_bg_new
                    );
                case SUCCESS:
                    return new NotificationStyle(
                            R.string.notification_label_success,
                            android.R.drawable.checkbox_on_background,
                            R.color.notification_unread_success,
                            R.color.notification_label_success,
                            R.color.notification_label_bg_success
                    );
                case FAILED:
                    return new NotificationStyle(
                            R.string.notification_label_failed,
                            android.R.drawable.ic_delete,
                            R.color.notification_unread_failed,
                            R.color.notification_label_failed,
                            R.color.notification_label_bg_failed
                    );
                default:
                    return styleMap.get(NotificationType.UNKNOWN);
            }
        }

        // calendar_reminder나 unknown은 기존대로
        NotificationStyle style = styleMap.get(type);
        return style != null ? style : styleMap.get(NotificationType.UNKNOWN);
    }

    private String getRelativeTimeString(NotificationEntity notification) {
        long reference = notification.getReceivedAt();
        if (notification.getCreatedAt() != null) {
            java.util.Date created = DateTimeUtils.parseServerDate(notification.getCreatedAt());
            if (created != null) {
                reference = created.getTime();
            }
        }
        if (reference == 0) {
            return "";
        }
        long diff = System.currentTimeMillis() - reference;
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days > 0) {
            return context.getResources().getQuantityString(R.plurals.notification_days_ago, (int) days, days);
        }
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours > 0) {
            return context.getResources().getQuantityString(R.plurals.notification_hours_ago, (int) hours, hours);
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        return context.getResources().getQuantityString(R.plurals.notification_minutes_ago, (int) minutes, minutes);
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    // ViewHolder 및 Style 클래스 정의
    public static class NotificationStyle {
        @StringRes public final int labelTextRes;
        @DrawableRes public final int iconRes;
        @ColorRes public final int unreadColorRes;
        @ColorRes public final int labelTextColorRes;
        @ColorRes public final int labelBgColorRes;
        public NotificationStyle(@StringRes int labelTextRes, @DrawableRes int iconRes, @ColorRes int unreadColorRes, @ColorRes int labelTextColorRes, @ColorRes int labelBgColorRes) {
            this.labelTextRes = labelTextRes;
            this.iconRes = iconRes;
            this.unreadColorRes = unreadColorRes;
            this.labelTextColorRes = labelTextColorRes;
            this.labelBgColorRes = labelBgColorRes;
        }
    }
    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        View leftColorIndicator;
        TextView tvLabel;
        TextView tvTitle;
        TextView tvBody;
        TextView tvTime;
        ImageView ivDelete;
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            leftColorIndicator = itemView.findViewById(R.id.left_color_indicator);
            tvLabel = itemView.findViewById(R.id.tv_notification_label);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
    public static class TipViewHolder extends RecyclerView.ViewHolder {
        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
