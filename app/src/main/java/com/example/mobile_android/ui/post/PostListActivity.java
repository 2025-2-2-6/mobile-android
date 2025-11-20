package com.example.mobile_android.ui.post;

import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private ApiService apiService;
    private String siteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        apiService = ApiClient.getClient().create(ApiService.class);

        // Intent에서 사이트 정보 가져오기
        String siteName = getIntent().getStringExtra("SITE_NAME");
        siteId = getIntent().getStringExtra("SITE_ID");

        if (siteName != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(siteName);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            } else {
                setTitle(siteName);
            }
        }

        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 초기 데이터는 Intent에서 가져오기 (있다면)
        postList = getIntent().getParcelableArrayListExtra("posts");
        if (postList == null) {
            postList = new ArrayList<>();
        }
      
        postAdapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        // API에서 최신 데이터 로드
        if (siteId != null) {
            loadPosts();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면으로 돌아올 때마다 새로고침
        if (siteId != null) {
            loadPosts();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadPosts() {
        Call<PostListResponse> call = apiService.getPosts(
                1, // page
                100, // page_size
                null, // query
                siteId, // site_id
                null, // since
                null, // until
                "created_at", // order_by
                "desc" // order
        );

        call.enqueue(new Callback<PostListResponse>() {
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
                Toast.makeText(PostListActivity.this,
                        "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
