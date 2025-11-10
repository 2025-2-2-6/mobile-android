package com.example.mobile_android.ui.login;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.PostAdapter;

import java.util.ArrayList;
import java.util.List;

public class PostListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_list);

        recyclerView = findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get posts from Intent extras (passed from ProfileActivity)
        postList = getIntent().getParcelableArrayListExtra("posts");

        if (postList == null) {
            postList = new ArrayList<>(); // Handle case where no posts are passed
        }

        postAdapter = new PostAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);
    }
}
