package com.example.mobile_android.ui.post;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // 사이트 이름 설정
        String siteName = getIntent().getStringExtra("SITE_NAME");
        String siteId   = getIntent().getStringExtra("SITE_ID");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (siteName != null) getSupportActionBar().setTitle(siteName);
        }

        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        postAdapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        // ★ 서버에서 데이터 로드
        if (siteId != null) {
            loadPosts(siteId);
        }
    }

    private void loadPosts(String siteId) {
        ApiClient.getApiService().getPosts(
                1,
                50,
                null,       // 검색어 q
                siteId,     // ★ site_id 전달
                null,
                null,
                "created_at",
                "desc"
        ).enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postList.clear();
                    postList.addAll(response.body().getItems());
                    postAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                Log.e("PostListActivity", "Failed to load posts", t);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
