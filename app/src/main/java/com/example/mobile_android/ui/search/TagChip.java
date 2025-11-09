package com.example.mobile_android.ui.search;

public class TagChip {
    public final String label;
    public final int count;
    public boolean selected;

    public TagChip(String label, int count, boolean selected) {
        this.label = label;
        this.count = count;
        this.selected = selected;
    }
}
