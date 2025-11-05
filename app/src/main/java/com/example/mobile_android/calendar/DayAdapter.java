package com.example.mobile_android.calendar;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_android.R;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.VH> {
    public interface OnDayClick { void onClick(LocalDate day); }
    private final List<LocalDate> items = new ArrayList<>();
    private final OnDayClick onDayClick;
    private final LocalDate today;
    private LocalDate monthAnchor;
    private LocalDate selected;

    public DayAdapter(OnDayClick onDayClick, LocalDate today) {
        this.onDayClick = onDayClick;
        this.today = today;
    }

    public void submit(List<LocalDate> days) {
        items.clear(); items.addAll(days);
        if (!items.isEmpty()) monthAnchor = items.get(15);
        notifyDataSetChanged();
    }
    public void setSelected(LocalDate selected) { this.selected = selected; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        LocalDate d = items.get(position);
        boolean inMonth = d.getMonth() == monthAnchor.getMonth();
        boolean isToday = d.equals(today);
        boolean isSelected = selected != null && d.equals(selected);

        h.tv.setText(String.valueOf(d.getDayOfMonth()));
        if (isSelected) {
            h.tv.setBackgroundResource(R.drawable.bg_day_selected);
            h.tv.setTextColor(Color.parseColor("#1A73E8"));
        } else if (isToday && inMonth) {
            h.tv.setBackgroundResource(R.drawable.bg_day_today);
            h.tv.setTextColor(Color.parseColor("#1A73E8"));
        } else {
            h.tv.setBackgroundResource(android.R.color.transparent);
            h.tv.setTextColor(inMonth ? Color.parseColor("#202124") : Color.parseColor("#BDC1C6"));
        }
        h.itemView.setOnClickListener(v -> { if (inMonth) { selected = d; notifyDataSetChanged(); onDayClick.onClick(d);} });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv; VH(@NonNull View itemView) { super(itemView); tv = itemView.findViewById(R.id.tvDay); }
    }
}
