package com.example.mobile_android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile_android.model.Post;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // ViewHolder의 TextView에 데이터를 설정합니다.
        holder.title.setText(post.getTitle());
        holder.content.setText(post.getContent());
        holder.date.setText(post.getEventDate()); // TODO: 날짜 형식 변경 고려 (예: ISO 8601 -> yyyy-MM-dd)
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // ViewHolder 클래스
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        // item_post.xml의 ID와 일치하는 변수들
        TextView title;
        TextView content;
        TextView date;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_post.xml의 ID를 사용하여 TextView를 찾습니다.
            title = itemView.findViewById(R.id.tv_post_title);
            content = itemView.findViewById(R.id.tv_post_content);
            date = itemView.findViewById(R.id.tv_post_date);
        }
    }
}
