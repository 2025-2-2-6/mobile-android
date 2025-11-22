package com.example.mobile_android.ui.calendar;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_android.R;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final OnDayClickListener listener;
    private List<LocalDate> days = Collections.emptyList();
    private List<LocalDate> eventDates = Collections.emptyList();
    private LocalDate selectedDate;

    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    public DayAdapter(OnDayClickListener listener, LocalDate selectedDate) {
        this.listener = listener;
        this.selectedDate = selectedDate;
    }

    public void submit(List<LocalDate> newDays) {
        this.days = newDays;
        notifyDataSetChanged();
    }

    public void setSelected(LocalDate newSelectedDate) {
        LocalDate oldSelectedDate = this.selectedDate;
        this.selectedDate = newSelectedDate;

        // 기존 선택된 날짜의 인덱스를 안전하게 확인
        int oldIndex = days.indexOf(oldSelectedDate);
        if (oldIndex >= 0) {
            notifyItemChanged(oldIndex);
        }

        // 새로 선택된 날짜의 인덱스를 안전하게 확인
        int newIndex = days.indexOf(newSelectedDate);
        if (newIndex >= 0) {
            notifyItemChanged(newIndex);
        }
    }

    public void setEventDates(@NonNull List<LocalDate> eventDates) {
        this.eventDates = eventDates.stream().distinct().collect(Collectors.toList());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        LocalDate date = days.get(position);
        boolean hasEvent = date != null && eventDates.contains(date);
        holder.bind(date, listener, selectedDate, hasEvent);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDay;
        private final View eventIndicator;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            eventIndicator = itemView.findViewById(R.id.event_indicator);
        }

        public void bind(LocalDate date, OnDayClickListener listener, LocalDate selectedDate, boolean hasEvent) {
            // null 날짜 처리 (빈 칸)
            if (date == null) {
                tvDay.setText("");
                itemView.setBackgroundResource(0);
                eventIndicator.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
                return;
            }

            // 날짜 표시
            tvDay.setText(String.valueOf(date.getDayOfMonth()));
            itemView.setClickable(true);

            // 선택된 날짜 스타일
            if (date.equals(selectedDate)) {
                itemView.setBackgroundResource(R.drawable.bg_day_selected);
                tvDay.setTextColor(Color.WHITE);
            } else {
                itemView.setBackgroundResource(0);
                tvDay.setTextColor(Color.BLACK);
            }

            // 이벤트 표시
            eventIndicator.setVisibility(hasEvent ? View.VISIBLE : View.GONE);

            // 클릭 리스너
            itemView.setOnClickListener(v -> listener.onDayClick(date));
        }
    }
}
