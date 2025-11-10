package com.example.mobile_android;

// 예시 데이터를 위한 임시 데이터 클래스
public class Site {
    private String name;
    private String category;
    private String url;
    private String lastUpdated;
    private int newPosts;

    public Site(String name, String category, String url, String lastUpdated, int newPosts) {
        this.name = name;
        this.category = category;
        this.url = url;
        this.lastUpdated = lastUpdated;
        this.newPosts = newPosts;
    }

    // Getter 메서드들
    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUrl() {
        return url;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public int getNewPosts() {
        return newPosts;
    }
}
