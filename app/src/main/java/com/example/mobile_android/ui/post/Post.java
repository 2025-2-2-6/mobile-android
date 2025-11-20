package com.example.mobile_android.ui.post;
import android.os.Parcel;
import android.os.Parcelable;
// 새로운 API 응답 구조에 맞춘 예시 데이터 클래스
public class Post implements Parcelable {
    private String id;
    private String siteId;
    private String title;
    private String content;
    private String sourceUrl;
    private String eventDate;
    private String eventStartDate;
    private String eventEndDate;
    private String location;
    private String category;
    private String createdAt;
    private String updatedAt;

    public Post(String id, String siteId, String title, String content, String sourceUrl,
                String eventDate, String eventStartDate, String eventEndDate,
                String location, String category, String createdAt, String updatedAt) {
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
    }

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

    // Getter
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
}
