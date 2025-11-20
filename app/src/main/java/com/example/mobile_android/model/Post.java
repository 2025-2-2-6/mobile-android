package com.example.mobile_android.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Post implements Serializable, Parcelable {
    @SerializedName("id")
    private String id;
    @SerializedName("site_id")
    private String siteId;
    @SerializedName("title")
    private String title;
    @SerializedName("content")
    private String content;
    @SerializedName("source_url")
    private String sourceUrl;
    @SerializedName("event_date")
    private String eventDate;
    @SerializedName("event_start_date")
    private String eventStartDate;
    @SerializedName("event_end_date")
    private String eventEndDate;
    @SerializedName("location")
    private String location;
    @SerializedName("category")
    private String category;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;
    @SerializedName("category_name")
    private String categoryName;
    @SerializedName("site_name")
    private String siteName;
    @SerializedName("is_new")
    private Boolean isNew;

    // Constructors
    public Post(String id, String siteId, String title, String content, String sourceUrl,
                String eventDate, String eventStartDate, String eventEndDate,
                String location, String category, String createdAt, String updatedAt,
                String categoryName, String siteName, Boolean isNew) {
        this.id = id;
        this.siteId = siteId;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.eventDate = eventDate;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
        this.location = location;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.categoryName = categoryName;
        this.siteName = siteName;
        this.isNew = isNew;
    }

    protected Post(Parcel in) {
        id = in.readString();
        siteId = in.readString();
        title = in.readString();
        content = in.readString();
        sourceUrl = in.readString();
        eventDate = in.readString();
        eventStartDate = in.readString();
        eventEndDate = in.readString();
        location = in.readString();
        category = in.readString();
        createdAt = in.readString();
        updatedAt = in.readString();
        categoryName = in.readString();
        siteName = in.readString();
        byte tmpIsNew = in.readByte();
        isNew = tmpIsNew == 0 ? null : tmpIsNew == 1;
    }

    // Getters
    public String getId() { return id; }
    public String getSiteId() { return siteId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceUrl() { return sourceUrl; }
    public String getEventDate() { return eventDate; }
    public String getEventStartDate() { return eventStartDate; }
    public String getEventEndDate() { return eventEndDate; }
    public String getLocation() { return location; }
    public String getCategory() { return category; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCategoryName() { return categoryName; }
    public String getSiteName() { return siteName; }
    public Boolean getIsNew() { return isNew; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(siteId);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(sourceUrl);
        dest.writeString(eventDate);
        dest.writeString(eventStartDate);
        dest.writeString(eventEndDate);
        dest.writeString(location);
        dest.writeString(category);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
        dest.writeString(categoryName);
        dest.writeString(siteName);
        dest.writeByte((byte) (isNew == null ? 0 : isNew ? 1 : 2));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };
}
