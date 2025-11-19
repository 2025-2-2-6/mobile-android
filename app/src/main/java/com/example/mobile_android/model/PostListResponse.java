package com.example.mobile_android.model;

import java.util.List;

public class PostListResponse {

    // 서버에서 오는 응답 필드들 (post.py의 PostListResponse 기준)
    public int total;
    public int page;
    public int page_size;

    // 🔥 여기! 이제 public 이라서 SiteDetailFragment 에서 바로 접근 가능
    public List<PostItem> items;

    // 리스트 안에 들어가는 게시글 하나
    public static class PostItem {
        public String id;
        public String site_id;
        public String title;
        public String content;
        public String source_url;
        public String event_date;
        public String created_at;
        public String updated_at;
    }
}
