package com.example.mobile_android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

@Entity(tableName = "post")
public class Post implements Serializable, Parcelable {

    @PrimaryKey
    @NonNull
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

    @ColumnInfo(defaultValue = "0")
    public boolean isSaved = false;

    public Post() {}

    @Ignore // Room will ignore this constructor
    public Post(@NonNull String id, String siteId, String title, String content, String sourceUrl,
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
        isSaved = in.readByte() != 0;
    }

    // ... Getters and Setters ...
    @NonNull
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
    public boolean isSaved() { return isSaved; }

    public void setId(@NonNull String id) { this.id = id;}
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    public void setEventStartDate(String eventStartDate) { this.eventStartDate = eventStartDate; }
    public void setEventEndDate(String eventEndDate) { this.eventEndDate = eventEndDate; }
    public void setLocation(String location) { this.location = location; }
    public void setCategory(String category) { this.category = category; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public void setIsNew(Boolean isNew) { this.isNew = isNew; }
    public void setSaved(boolean saved) { isSaved = saved; }

    /**
     * 캘린더 표시에 사용할 대표 날짜 문자열을 반환합니다.
     * 우선순위: eventStartDate > eventDate > eventEndDate
     */
    @Ignore
    public String getCalendarAnchorDate() {
        if (!TextUtils.isEmpty(eventStartDate)) {
            return eventStartDate;
        }
        if (!TextUtils.isEmpty(eventDate)) {
            return eventDate;
        }
        return eventEndDate;
    }

    // ... Parcelable implementation ...
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
        dest.writeByte((byte) (isSaved ? 1 : 0));
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
