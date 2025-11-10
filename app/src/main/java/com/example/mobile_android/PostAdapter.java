package com.example.mobile_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.postTitle.setText(post.getTitle());
        holder.postContent.setText(post.getContent());

        // [수정] eventDate가 있으면 표시, 없으면 숨김
        if (post.getEventDate() != null) {
            holder.postDate.setText(post.getEventDate());
            holder.postDate.setVisibility(View.VISIBLE);
        } else {
            holder.postDate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle, postContent, postDate;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postTitle = itemView.findViewById(R.id.tv_post_title);
            postContent = itemView.findViewById(R.id.tv_post_content);
            postDate = itemView.findViewById(R.id.tv_post_date);
        }
    }
}
