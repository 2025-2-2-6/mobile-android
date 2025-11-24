package com.example.mobile_android.ui.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.CalendarEvent;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CalendarEvent를 표시하는 RecyclerView 어댑터
 */
public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    private List<CalendarEvent> events = new ArrayList<>();
    private OnEventClickListener onEditListener;
    private OnEventClickListener onDeleteListener;

    public interface OnEventClickListener {
        void onClick(CalendarEvent event);
    }

    public CalendarEventAdapter(OnEventClickListener onEditListener, OnEventClickListener onDeleteListener) {
        this.onEditListener = onEditListener;
        this.onDeleteListener = onDeleteListener;
    }

    public void submitList(List<CalendarEvent> newEvents) {
        this.events = newEvents != null ? newEvents : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = events.get(position);
        holder.bind(event, onEditListener, onDeleteListener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private View categoryIndicator;
        private TextView tvTitle;
        private TextView tvTime;
        private TextView tvCategory;
        private TextView tvMemo;
        private ImageButton btnEdit;
        private ImageButton btnDelete;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_event);
            categoryIndicator = itemView.findViewById(R.id.category_indicator);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvCategory = itemView.findViewById(R.id.tv_event_category);
            tvMemo = itemView.findViewById(R.id.tv_event_memo);
            btnEdit = itemView.findViewById(R.id.btn_edit_event);
            btnDelete = itemView.findViewById(R.id.btn_delete_event);
        }

        public void bind(CalendarEvent event, OnEventClickListener onEditListener, OnEventClickListener onDeleteListener) {
            tvTitle.setText(event.getTitle());

            // 시간 표시
            if (event.getEventTime() != null && !event.getEventTime().isEmpty()) {
                tvTime.setText(formatTime(event.getEventTime()));
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }

            // 카테고리 표시
            if (event.getCategory() != null && !event.getCategory().isEmpty()) {
                tvCategory.setText(event.getCategory());
                tvCategory.setVisibility(View.VISIBLE);

                // 카테고리별 색상
                int color = getCategoryColor(event.getCategory());
                categoryIndicator.setBackgroundColor(color);
                tvCategory.setTextColor(color);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            // 메모 표시
            if (event.getMemo() != null && !event.getMemo().isEmpty()) {
                tvMemo.setText(event.getMemo());
                tvMemo.setVisibility(View.VISIBLE);
            } else {
                tvMemo.setVisibility(View.GONE);
            }

            // 수정 버튼
            btnEdit.setOnClickListener(v -> {
                if (onEditListener != null) {
                    onEditListener.onClick(event);
                }
            });

            // 삭제 버튼
            btnDelete.setOnClickListener(v -> {
                if (onDeleteListener != null) {
                    onDeleteListener.onClick(event);
                }
            });

            // 카드 클릭 시에도 수정
            cardView.setOnClickListener(v -> {
                if (onEditListener != null) {
                    onEditListener.onClick(event);
                }
            });
        }

        private String formatTime(String timeStr) {
            try {
                String[] parts = timeStr.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                String amPm = hour < 12 ? "오전" : "오후";
                int displayHour = hour % 12;
                if (displayHour == 0) displayHour = 12;
                return String.format(Locale.KOREAN, "%s %d:%02d", amPm, displayHour, minute);
            } catch (Exception e) {
                return timeStr;
            }
        }

        private int getCategoryColor(String category) {
            switch (category) {
                case "창업":
                    return 0xFFFF4458;  // 빨강
                case "공모전":
                    return 0xFF4C84FF;  // 파랑
                case "교육":
                    return 0xFF2AA876;  // 초록
                case "기타":
                default:
                    return 0xFF999999;  // 회색
            }
        }
    }
}
