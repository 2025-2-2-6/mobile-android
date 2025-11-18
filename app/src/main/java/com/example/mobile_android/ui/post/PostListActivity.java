package com.example.mobile_android.ui.post;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;

import java.util.ArrayList;
import java.util.List;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뒤로가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Intent에서 사이트 이름 가져오기
        String siteName = getIntent().getStringExtra("SITE_NAME");
        if (siteName != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(siteName);
        }

        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get posts from Intent extras (passed from AddSiteActivity or other activities)
        postList = getIntent().getParcelableArrayListExtra("posts");

        if (postList == null) {
            postList = new ArrayList<>(); // Handle case where no posts are passed
        }

        postAdapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

