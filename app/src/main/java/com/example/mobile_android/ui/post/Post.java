package com.example.mobile_android.ui.post;
import android.os.Parcel;
import android.os.Parcelable;
// 새로운 API 응답 구조에 맞춘 예시 데이터 클래스
public class Post implements Parcelable {
    private String id;
    private String title;
    private String content;
    private String eventDate;
    private String location;
    private String sourceUrl;

    public Post(String id, String title, String content, String eventDate, String location, String sourceUrl) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.eventDate = eventDate;
        this.location = location;
        this.sourceUrl = sourceUrl;
    }

    protected Post(Parcel in) {
        id = in.readString();
        title = in.readString();
        content = in.readString();
        eventDate = in.readString();
        location = in.readString();
        sourceUrl = in.readString();
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(eventDate);
        dest.writeString(location);
        dest.writeString(sourceUrl);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getEventDate() { return eventDate; }
    public String getLocation() { return location; }
    public String getSourceUrl() { return sourceUrl; }
}