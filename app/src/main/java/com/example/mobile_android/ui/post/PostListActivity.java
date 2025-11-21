package com.example.mobile_android.ui.post;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.data.local.AppDatabase;
import com.example.mobile_android.data.local.PostDao;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.PostListResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.network.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> currentPostList = new ArrayList<>();
    private ApiService apiService;
    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private String siteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_list);

        initDatabase();
        apiService = ApiClient.getClient().create(ApiService.class);

        // Intent에서 사이트 정보 가져오기
        String siteName = getIntent().getStringExtra("SITE_NAME");
        siteId = getIntent().getStringExtra("SITE_ID");

        setupToolbar(siteName);
        setupRecyclerView();

        if (siteId != null) {
            observePostsBySite();
            loadPostsFromServer();
        }
    }

    private void initDatabase() {
        postDao = AppDatabase.getInstance(this).postDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupToolbar(String siteName) {
        if (siteName != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(siteName);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            } else {
                setTitle(siteName);
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(this, currentPostList);
        recyclerView.setAdapter(postAdapter);
    }

    private void observePostsBySite() {
        LiveData<List<Post>> postsLiveData = postDao.getPostsBySite(siteId);
        postsLiveData.observe(this, posts -> {
            currentPostList.clear();
            currentPostList.addAll(posts);
            postAdapter.notifyDataSetChanged();
        });
    }

    private void loadPostsFromServer() {
        Call<PostListResponse> call = apiService.getPosts(1, 100, null, siteId, null, null, "created_at", "desc");

        call.enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    databaseExecutor.execute(() -> {
                        postDao.upsert(response.body().getItems());
                    });
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                Toast.makeText(PostListActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
