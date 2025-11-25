package com.example.mobile_android.ui.site;

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
    private EditText siteNameEditText;

    private Button registerButton;
    private Button cancelButton;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_site);

        siteUrlEditText = findViewById(R.id.et_url);
        siteNameEditText = findViewById(R.id.et_site_name);

        registerButton = findViewById(R.id.btn_register);
        cancelButton = findViewById(R.id.btn_cancel);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        registerButton.setOnClickListener(v -> {
            String siteUrl = siteUrlEditText.getText().toString().trim();
            String siteNameInput = siteNameEditText.getText().toString().trim();

            // 🔹 URL은 필수
            if (siteUrl.isEmpty()) {
                siteUrlEditText.setError("URL을 입력해주세요.");
                return;
            }
            String finalUrl;
            if (siteUrl.startsWith("http://") || siteUrl.startsWith("https://")) {
                finalUrl = siteUrl;
            } else {
                finalUrl = "https://" + siteUrl;
            }
            // 🔹 사이트 이름은 선택
            // 비어 있으면 URL을 이름으로 대신 사용
            String finalSiteName = siteNameInput.isEmpty() ? siteUrl : siteNameInput;

            registerSite(finalUrl, finalSiteName);
        });

        cancelButton.setOnClickListener(v -> finish());
    }

    private void registerSite(String url, String siteName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            Toast.makeText(this, "로그인 상태를 확인할 수 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);

        String userId = currentUser.getUid();

        Log.d("AddSiteActivity", "Registering site with URL: " + url +
                ", name: " + siteName);

        // 🔹 카테고리는 이제 안 보냄 → 백엔드 + AI가 자동 분류
        SiteRegisterRequest request =
                new SiteRegisterRequest(url, siteName, userId);

        Call<SiteRegisterResponse> call =
                ApiClient.getApiService().registerSite(request);

        call.enqueue(new Callback<SiteRegisterResponse>() {
            @Override
            public void onResponse(Call<SiteRegisterResponse> call,
                                   Response<SiteRegisterResponse> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {

                    SiteRegisterResponse body = response.body();
                    Toast.makeText(AddSiteActivity.this,
                            "사이트 등록 완료!\nAI가 사이트 내용을 분석해 카테고리를 자동으로 분류합니다.",
                            Toast.LENGTH_LONG).show();

                    android.content.Intent resultIntent = new android.content.Intent();
                    resultIntent.putExtra("SITE_ID", body.getSiteId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(AddSiteActivity.this,
                            "등록 실패: " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SiteRegisterResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(AddSiteActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingProgressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            cancelButton.setEnabled(false);
            siteUrlEditText.setEnabled(false);
            siteNameEditText.setEnabled(false);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            cancelButton.setEnabled(true);
            siteUrlEditText.setEnabled(true);
            siteNameEditText.setEnabled(true);
        }
    }
}
