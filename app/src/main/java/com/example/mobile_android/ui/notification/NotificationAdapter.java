package com.example.mobile_android.ui.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.example.mobile_android.model.Notification;
import com.example.mobile_android.util.DateTimeUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NOTIFICATION = 0;
    private static final int VIEW_TYPE_TIP = 1;

    private final Context context;
    private final List<NotificationEntity> notifications = new ArrayList<>();
    private final EnumMap<NotificationVariant, NotificationStyle> styleMap = new EnumMap<>(NotificationVariant.class);

    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationEntity notification);
        void onDeleteClick(NotificationEntity notification);
    }

    public NotificationAdapter(Context context) {
        this.context = context;
        styleMap.put(NotificationVariant.CALENDAR, new NotificationStyle(
                R.string.notification_label_calendar,
                android.R.drawable.ic_menu_my_calendar,
                R.color.notification_unread_calendar,
                R.color.notification_icon_calendar
        ));
        styleMap.put(NotificationVariant.CRAWL_SUCCESS, new NotificationStyle(
                R.string.notification_label_crawl_success,
                android.R.drawable.checkbox_on_background,
                R.color.notification_unread_crawl_success,
                R.color.notification_icon_success
        ));
        styleMap.put(NotificationVariant.CRAWL_FAILED, new NotificationStyle(
                R.string.notification_label_crawl_failed,
                android.R.drawable.ic_delete,
                R.color.notification_unread_crawl_failed,
                R.color.notification_icon_failed
        ));
        styleMap.put(NotificationVariant.CRAWL_NEW_POST, new NotificationStyle(
                R.string.notification_label_crawl_new,
                android.R.drawable.ic_input_add,
                R.color.notification_unread_crawl_new,
                R.color.notification_icon_new
        ));
        styleMap.put(NotificationVariant.DEFAULT, new NotificationStyle(
                R.string.notification_label_default,
                android.R.drawable.ic_dialog_info,
                R.color.notification_unread_default,
                R.color.notification_icon_default
        ));
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<NotificationEntity> items) {
        notifications.clear();
        if (items != null) {
            notifications.addAll(items);
        }
        notifyDataSetChanged();
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

        NotificationStyle style = resolveStyle(notification.getType());
        int accentColor = ContextCompat.getColor(context, style.accentColorRes);
        int unreadColor = ContextCompat.getColor(context, style.unreadColorRes);
        int cardColor = notification.isRead()
                ? ContextCompat.getColor(context, R.color.notification_bg_read)
                : unreadColor;

        vh.cardView.setCardBackgroundColor(cardColor);
        vh.cardView.setStrokeColor(notification.isRead()
                ? ContextCompat.getColor(context, R.color.notification_icon_default)
                : ColorUtils.setAlphaComponent(accentColor, 80));
        vh.cardView.setStrokeWidth(notification.isRead() ? dpToPx(1) : 0);

        vh.tvLabel.setText(context.getString(style.labelTextRes));
        Drawable labelBg = DrawableCompat.wrap(vh.tvLabel.getBackground().mutate());
        DrawableCompat.setTint(labelBg, ColorUtils.setAlphaComponent(accentColor, 70));
        vh.tvLabel.setBackground(labelBg);
        vh.tvLabel.setTextColor(accentColor);

        Drawable iconBg = DrawableCompat.wrap(vh.iconContainer.getBackground().mutate());
        DrawableCompat.setTint(iconBg, ColorUtils.setAlphaComponent(accentColor, notification.isRead() ? 40 : 90));
        vh.iconContainer.setBackground(iconBg);

        vh.ivIcon.setImageResource(style.iconRes);
        vh.ivIcon.setColorFilter(notification.isRead()
                ? ContextCompat.getColor(context, R.color.notification_icon_default)
                : accentColor);

        vh.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });

        vh.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(notification);
            }
        });
    }

    private NotificationStyle resolveStyle(String rawType) {
        NotificationVariant variant = resolveVariant(rawType);
        NotificationStyle style = styleMap.get(variant);
        return style != null ? style : styleMap.get(NotificationVariant.DEFAULT);
    }

    private NotificationVariant resolveVariant(String rawType) {
        if (rawType == null) {
            return NotificationVariant.DEFAULT;
        }
        String type = rawType.toLowerCase(Locale.ROOT);
        if (type.contains("calendar") || type.contains("schedule") || type.contains("deadline")) {
            return NotificationVariant.CALENDAR;
        }
        if (type.startsWith("crawl_new_posts")) {
            if (type.contains("failed") || type.contains("failure") || type.contains("error")) {
                return NotificationVariant.CRAWL_FAILED;
            }
            if (type.contains("success")) {
                return NotificationVariant.CRAWL_SUCCESS;
            }
            return NotificationVariant.CRAWL_NEW_POST;
        }
        if (Notification.Type.CRAWLING_COMPLETE.equals(rawType)) {
            return NotificationVariant.CRAWL_SUCCESS;
        }
        if (Notification.Type.NEW_POST.equals(rawType)) {
            return NotificationVariant.CRAWL_NEW_POST;
        }
        if (Notification.Type.SCHEDULE_REMINDER.equals(rawType)
                || Notification.Type.EVENT_REMINDER.equals(rawType)
                || Notification.Type.DEADLINE.equals(rawType)) {
            return NotificationVariant.CALENDAR;
        }
        return NotificationVariant.DEFAULT;
    }

    private String getRelativeTimeString(NotificationEntity notification) {
        long reference = notification.getReceivedAt();
        if (notification.getCreatedAt() != null) {
            java.util.Date created = DateTimeUtils.parseServerDate(notification.getCreatedAt());
            if (created != null) {
                reference = created.getTime();
            }
        }
        if (reference <= 0) {
            return "";
        }

        long diffMillis = System.currentTimeMillis() - reference;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (minutes < 1) {
            return "방금 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            String formatted = DateTimeUtils.formatServerDate(notification.getCreatedAt(), "M월 d일");
            return formatted != null ? formatted : "";
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        FrameLayout iconContainer;
        ImageView ivIcon;
        TextView tvLabel;
        TextView tvTitle;
        TextView tvBody;
        TextView tvTime;
        ImageView ivDelete;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            tvLabel = itemView.findViewById(R.id.tv_notification_label);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }

    static class TipViewHolder extends RecyclerView.ViewHolder {
        TipViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class NotificationStyle {
        @StringRes final int labelTextRes;
        @DrawableRes final int iconRes;
        @ColorRes final int unreadColorRes;
        @ColorRes final int accentColorRes;

        NotificationStyle(@StringRes int labelTextRes,
                          @DrawableRes int iconRes,
                          @ColorRes int unreadColorRes,
                          @ColorRes int accentColorRes) {
            this.labelTextRes = labelTextRes;
            this.iconRes = iconRes;
            this.unreadColorRes = unreadColorRes;
            this.accentColorRes = accentColorRes;
        }
    }

    private enum NotificationVariant {
        CALENDAR,
        CRAWL_SUCCESS,
        CRAWL_FAILED,
        CRAWL_NEW_POST,
        DEFAULT
    }
}
