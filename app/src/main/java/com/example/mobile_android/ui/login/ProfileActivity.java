package com.example.mobile_android.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
// PostListActivity의 정확한 경로를 임포트합니다.
import com.example.mobile_android.PostListActivity;
import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;
import com.example.mobile_android.network.ApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    private ShapeableImageView imageView;
    private TextView name, mail;
    private EditText siteUrlEditText;
    private Button registerSiteButton;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        imageView = findViewById(R.id.profileImage);
        name = findViewById(R.id.nameTV);
        mail = findViewById(R.id.mailTV);
        siteUrlEditText = findViewById(R.id.siteUrlEditText);
        registerSiteButton = findViewById(R.id.registerSiteButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Configure Google Sign In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        if (currentUser != null) {
            Glide.with(this).load(currentUser.getPhotoUrl()).into(imageView);
            name.setText(currentUser.getDisplayName());
            mail.setText(currentUser.getEmail());
        }

        registerSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String siteUrl = siteUrlEditText.getText().toString().trim();
                if (siteUrl.isEmpty()) {
                    siteUrlEditText.setError("URL을 입력해주세요.");
                    return;
                }
                registerSite(siteUrl);
            }
        });

        Button signOutBtn = findViewById(R.id.signOut);
        signOutBtn.setOnClickListener(v -> {
            // Sign out from Firebase
            auth.signOut();
            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Toast.makeText(ProfileActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                // Go back to Login activity
                Intent intent = new Intent(ProfileActivity.this, Login.class);
                startActivity(intent);
                finish();
            });
        });
    }

    private void registerSite(String url) {
        showLoading(true);
        String userId = null; // TODO: 실제 사용자 ID 가져오도록 수정
        String siteName = ""; // TODO: 필요하다면 사이트 이름 설정

        SiteRegisterRequest request = new SiteRegisterRequest(url, siteName, userId);
        Call<SiteRegisterResponse> call = ApiClient.getApiService().registerSite(request);

        call.enqueue(new Callback<SiteRegisterResponse>() {
            @Override
            public void onResponse(Call<SiteRegisterResponse> call, Response<SiteRegisterResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Post> posts = response.body().getPosts();
                    Toast.makeText(ProfileActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();

                    // Navigate to PostListActivity and pass posts
                    Intent intent = new Intent(ProfileActivity.this, PostListActivity.class);
                    intent.putParcelableArrayListExtra("posts", new ArrayList<>(posts));
                    startActivity(intent);
                } else {
                    // TODO: 에러 응답 처리
                    Toast.makeText(ProfileActivity.this, "사이트 등록 실패: " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SiteRegisterResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ProfileActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            registerSiteButton.setEnabled(false);
            siteUrlEditText.setEnabled(false);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            registerSiteButton.setEnabled(true);
            siteUrlEditText.setEnabled(true);
        }
    }
}
