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
    @SerializedName("location")
    private String location;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;

    // Constructors
    public Post(String id, String siteId, String title, String content, String sourceUrl, String eventDate, String location, String createdAt, String updatedAt) {
        this.id = id;
        this.siteId = siteId;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.eventDate = eventDate;
        this.location = location;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    protected Post(Parcel in) {
        id = in.readString();
        siteId = in.readString();
        title = in.readString();
        content = in.readString();
        sourceUrl = in.readString();
        eventDate = in.readString();
        location = in.readString();
        createdAt = in.readString();
        updatedAt = in.readString();
    }

    // Getters
    public String getId() { return id; }
    public String getSiteId() { return siteId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSourceUrl() { return sourceUrl; }
    public String getEventDate() { return eventDate; }
    public String getLocation() { return location; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(siteId);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(sourceUrl);
        dest.writeString(eventDate);
        dest.writeString(location);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
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
