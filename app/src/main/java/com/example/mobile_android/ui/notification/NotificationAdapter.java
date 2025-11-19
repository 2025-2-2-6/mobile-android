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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.NotificationEntity;
import com.example.mobile_android.model.Notification;
import com.example.mobile_android.util.DateTimeUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NOTIFICATION = 0;
    private static final int VIEW_TYPE_TIP = 1;

    private final Context context;
    private final List<NotificationEntity> notifications = new ArrayList<>();
    private final Map<String, NotificationStyle> styleMap = new HashMap<>();
    private final NotificationStyle defaultStyle;

    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationEntity notification);
        void onDeleteClick(NotificationEntity notification);
    }

    public NotificationAdapter(Context context) {
        this.context = context;
        defaultStyle = new NotificationStyle(
                R.string.notification_label_crawling,
                R.drawable.ic_notification_new,
                R.color.notification_card_blue,
                R.color.notification_label_blue_bg,
                R.color.notification_icon_blue_tint
        );
        styleMap.put(Notification.Type.NEW_POST, new NotificationStyle(
                R.string.notification_label_new_post,
                R.drawable.ic_notification_post,
                R.color.notification_card_green,
                R.color.notification_label_green_bg,
                R.color.notification_icon_green_tint
        ));
        styleMap.put(Notification.Type.CRAWLING_COMPLETE, new NotificationStyle(
                R.string.notification_label_crawling,
                R.drawable.ic_notification_new,
                R.color.notification_card_blue,
                R.color.notification_label_blue_bg,
                R.color.notification_icon_blue_tint
        ));
        NotificationStyle scheduleStyle = new NotificationStyle(
                R.string.notification_label_schedule,
                R.drawable.ic_notification_calendar,
                R.color.notification_card_purple,
                R.color.notification_label_purple_bg,
                R.color.notification_icon_purple_tint
        );
        styleMap.put(Notification.Type.SCHEDULE_REMINDER, scheduleStyle);
        styleMap.put(Notification.Type.EVENT_REMINDER, scheduleStyle);
        styleMap.put(Notification.Type.DEADLINE, scheduleStyle);
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
        } else {
            View view = inflater.inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_TIP) {
            // static content handled in layout
            return;
        }

        NotificationEntity notification = notifications.get(position);
        NotificationViewHolder vh = (NotificationViewHolder) holder;

        vh.tvTitle.setText(notification.getTitle());
        vh.tvBody.setText(notification.getMessage());
        vh.tvTime.setText(getRelativeTimeString(notification));
        vh.ivNewBadge.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        NotificationStyle style = resolveStyle(notification.getType());
        vh.tvLabel.setText(context.getString(style.labelTextRes));
        vh.cardView.setCardBackgroundColor(ContextCompat.getColor(context, style.cardColorRes));
        Drawable labelBg = DrawableCompat.wrap(vh.tvLabel.getBackground().mutate());
        DrawableCompat.setTint(labelBg, ContextCompat.getColor(context, style.labelColorRes));
        vh.tvLabel.setBackground(labelBg);

        Drawable iconBg = DrawableCompat.wrap(vh.iconContainer.getBackground().mutate());
        DrawableCompat.setTint(iconBg, ContextCompat.getColor(context, style.labelColorRes));
        vh.iconContainer.setBackground(iconBg);

        vh.ivIcon.setImageResource(style.iconRes);
        vh.ivIcon.setColorFilter(ContextCompat.getColor(context, style.iconTintRes));

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

    @Override
    public int getItemCount() {
        // Always show tip card at the bottom
        return notifications.size() + 1;
    }

    private NotificationStyle resolveStyle(String type) {
        if (type == null) {
            return defaultStyle;
        }
        NotificationStyle style = styleMap.get(type);
        return style != null ? style : defaultStyle;
    }

    private String getRelativeTimeString(NotificationEntity notification) {
        long reference = notification.getReceivedAt();
        if (notification.getCreatedAt() != null) {
            Date created = DateTimeUtils.parseServerDate(notification.getCreatedAt());
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
            return DateTimeUtils.formatServerDate(notification.getCreatedAt(), "MM월 dd일");
        }
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
        ImageView ivNewBadge;

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
            ivNewBadge = itemView.findViewById(R.id.iv_new_badge);
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
        @ColorRes final int cardColorRes;
        @ColorRes final int labelColorRes;
        @ColorRes final int iconTintRes;

        NotificationStyle(@StringRes int labelTextRes,
                          @DrawableRes int iconRes,
                          @ColorRes int cardColorRes,
                          @ColorRes int labelColorRes,
                          @ColorRes int iconTintRes) {
            this.labelTextRes = labelTextRes;
            this.iconRes = iconRes;
            this.cardColorRes = cardColorRes;
            this.labelColorRes = labelColorRes;
            this.iconTintRes = iconTintRes;
        }
    }
}
