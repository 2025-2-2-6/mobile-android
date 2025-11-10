package com.example.mobile_android;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

// PostAdapter의 올바른 경로로 수정합니다.
import com.example.mobile_android.model.Post;
import com.example.mobile_android.PostAdapter;

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

        // ProfileActivity에서 전달받은 실제 데이터를 사용합니다.
        List<Post> postList = getIntent().getParcelableArrayListExtra("posts");
        if (postList == null) {
            // 데이터가 없는 경우, 앱이 비정상 종료되지 않도록 빈 리스트로 초기화합니다.
            postList = new ArrayList<>();
        }

        // 어댑터 생성자에 Context(this)와 게시물 리스트를 전달합니다.
        PostAdapter adapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
