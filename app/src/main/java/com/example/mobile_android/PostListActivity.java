package com.example.mobile_android;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

public class PostListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String siteName = getIntent().getStringExtra("SITE_NAME");
        if (siteName != null) {
            getSupportActionBar().setTitle(siteName);
        }

        RecyclerView recyclerView = findViewById(R.id.rv_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // [수정] 새로운 API 응답 구조에 맞춘 예시 데이터 생성
        List<Post> postList = new ArrayList<>();
        postList.add(new Post("post-id-1", "[장학] 2024학년도 2학기 국가장학금", "2024학년도 2학기 국가장학금 1차 신청 기간을 아래와 같이 안내합니다...", "2024-06-20", null, "https://university.ac.kr/scholarship/123"));
        postList.add(new Post("post-id-2", "[공지] 2024학년도 하계 계절수업", "2024학년도 하계 계절수업 수강신청을 다음과 같이 안내합니다...", null, "온라인", "https://university.ac.kr/notice/456"));
        postList.add(new Post("post-id-3", "[행사] 2024년 상반기 SW 경진대회", "소프트웨어 중심대학 사업단에서는 다음과 같이 2024년 상반기 SW 경진대회를 개최합니다...", "2024-07-15", "공학관 101호", "https://university.ac.kr/sw/contest/789"));

        PostAdapter adapter = new PostAdapter(postList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
