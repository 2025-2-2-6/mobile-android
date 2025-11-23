package com.example.mobile_android.ui.post;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.mobile_android.util.TokenManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> currentPostList = new ArrayList<>();
    private List<Post> allPostList = new ArrayList<>();
    private ApiService apiService;
    private PostDao postDao;
    private ExecutorService databaseExecutor;
    private String siteId;
    private EditText searchEditText;
    private ImageButton clearSearchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_detail);

        initDatabase();
        apiService = ApiClient.getClient().create(ApiService.class);

        // Intent에서 사이트 정보 가져오기
        String siteName = getIntent().getStringExtra("SITE_NAME");
        siteId = getIntent().getStringExtra("SITE_ID");

        setupToolbar(siteName);
        setupRecyclerView();
        setupSearchView();

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

    private void setupSearchView() {
        searchEditText = findViewById(R.id.searchEditText);
        clearSearchButton = findViewById(R.id.clearSearchButton);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPosts(s.toString());
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.clearFocus();
        });
    }

    private void filterPosts(String query) {
        if (query.isEmpty()) {
            currentPostList.clear();
            currentPostList.addAll(allPostList);
        } else {
            String lowerQuery = query.toLowerCase();
            List<Post> filtered = allPostList.stream()
                    .filter(post -> {
                        String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
                        String content = post.getContent() != null ? post.getContent().toLowerCase() : "";
                        return title.contains(lowerQuery) || content.contains(lowerQuery);
                    })
                    .collect(Collectors.toList());
            currentPostList.clear();
            currentPostList.addAll(filtered);
        }
        postAdapter.notifyDataSetChanged();
    }

    private void observePostsBySite() {
        LiveData<List<Post>> postsLiveData = postDao.getPostsBySite(siteId);
        postsLiveData.observe(this, posts -> {
            allPostList.clear();
            allPostList.addAll(posts);

            // 검색어가 있으면 필터링, 없으면 전체 표시
            String query = searchEditText.getText().toString();
            filterPosts(query);
        });
    }

    private void loadPostsFromServer() {
        String token = TokenManager.getBearerToken(this);
        Call<PostListResponse> call = apiService.getPosts(token, 1, 100, null, siteId, null, null, "created_at", "desc");

        call.enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    databaseExecutor.execute(() -> {
                        // 특정 사이트의 게시물만 덮어쓰기 (전체 DB를 지우지 않음)
                        postDao.upsertBySite(siteId, response.body().getItems());
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
