package com.example.mobile_android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PostListResponse {
    @SerializedName("total")
    private int total;
    @SerializedName("page")
    private int page;
    @SerializedName("page_size")
    private int pageSize;
    @SerializedName("items")
    private List<Post> items;

    // Getters
    public int getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public List<Post> getItems() { return items; }
}