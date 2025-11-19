package com.example.mobile_android.ui.site;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_android.R;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;
import com.example.mobile_android.network.ApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            Toast.makeText(this, "로그인 상태를 확인할 수 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        String userId = currentUser.getUid();
        String siteName = ""; // TODO: 필요한 경우 사이트 이름 입력

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

                    Log.d("AddSiteActivity", "Registration successful! siteId: " + body.getSiteId());
                    Log.d("AddSiteActivity", "Message: " + body.getMessage());

                    String toastMessage = body.getMessage() + "\n크롤링이 끝나면 알림으로 알려드릴게요.";
                    Toast.makeText(AddSiteActivity.this, toastMessage, Toast.LENGTH_LONG).show();

                    Intent resultIntent = new Intent();
                    if (body.getSiteId() != null) {
                        resultIntent.putExtra("SITE_ID", body.getSiteId());
                    }
                    setResult(RESULT_OK, resultIntent);
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
}
