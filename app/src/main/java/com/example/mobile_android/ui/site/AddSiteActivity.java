package com.example.mobile_android.ui.site;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Post;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.ui.post.PostListActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddSiteActivity extends AppCompatActivity {

    private EditText siteUrlEditText;
    private Button registerButton;
    private Button cancelButton;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_site);

        siteUrlEditText = findViewById(R.id.et_url);
        registerButton = findViewById(R.id.btn_register);
        cancelButton = findViewById(R.id.btn_cancel);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        registerButton.setOnClickListener(v -> {
            String siteUrl = siteUrlEditText.getText().toString().trim();
            if (siteUrl.isEmpty()) {
                siteUrlEditText.setError("URL을 입력해주세요.");
                return;
            }
            registerSite(siteUrl);
        });

        cancelButton.setOnClickListener(v -> {
            finish(); // Close the activity and go back to the previous one
        });
    }

    private void registerSite(String url) {
        showLoading(true);
        String userId = null; // TODO: 실제 사용자 ID 가져오는 로직 구현
        String siteName = extractDomainName(url);; // TODO: 필요하다면 사이트 이름 설정

        Log.d("AddSiteActivity", "Registering site with URL: " + url);

        SiteRegisterRequest request = new SiteRegisterRequest(url, siteName, userId);
        Call<SiteRegisterResponse> call = ApiClient.getApiService().registerSite(request);

        call.enqueue(new Callback<SiteRegisterResponse>() {
            @Override
            public void onResponse(Call<SiteRegisterResponse> call, Response<SiteRegisterResponse> response) {
                showLoading(false);
                Log.d("AddSiteActivity", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    SiteRegisterResponse body = response.body();
                    List<Post> posts = body.getPosts();

                    Log.d("AddSiteActivity", "Registration successful! Posts count: " + (posts != null ? posts.size() : 0));
                    Log.d("AddSiteActivity", "Message: " + body.getMessage());

                    Toast.makeText(AddSiteActivity.this, body.getMessage(), Toast.LENGTH_LONG).show();

                    // Navigate to PostListActivity and pass posts
                    Intent intent = new Intent(AddSiteActivity.this, PostListActivity.class);
                    if (posts != null) {
                        intent.putParcelableArrayListExtra("posts", new ArrayList<>(posts));
                    }
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "사이트 등록 실패: " + response.code() + " " + response.message();
                    Log.e("AddSiteActivity", errorMsg);

                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("AddSiteActivity", "Error body: " + errorBody);
                            errorMsg += "\n" + errorBody;
                        }
                    } catch (Exception e) {
                        Log.e("AddSiteActivity", "Error reading error body", e);
                    }

                    Toast.makeText(AddSiteActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SiteRegisterResponse> call, Throwable t) {
                showLoading(false);
                String errorMsg = "네트워크 오류: " + t.getMessage();
                Log.e("AddSiteActivity", errorMsg, t);
                Toast.makeText(AddSiteActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            cancelButton.setEnabled(false);
            siteUrlEditText.setEnabled(false);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            cancelButton.setEnabled(true);
            siteUrlEditText.setEnabled(true);
        }
    }
    private String extractDomainName(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return "사이트";

            // www 제거
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;
        } catch (Exception e) {
            return "사이트";
        }
    }
}

