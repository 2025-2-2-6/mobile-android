package com.example.mobile_android.ui.site;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_android.R;
import com.example.mobile_android.model.SiteRegisterRequest;
import com.example.mobile_android.model.SiteRegisterResponse;
import com.example.mobile_android.network.ApiClient;
import com.example.mobile_android.util.CategoryUtils;
import com.example.mobile_android.util.TokenManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddSiteActivity extends AppCompatActivity {

    private static final int MAX_URL_LENGTH = 255;
    private static final int REQUIRED_MARK_COLOR = Color.parseColor("#FF3B30");

    private EditText siteUrlEditText;
    private EditText siteNameEditText;
    private EditText categoryEditText;
    private EditText memoEditText;
    private Button registerButton;
    private Button cancelButton;
    private ProgressBar loadingProgressBar;
    private TextView urlCounterTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_site);

        siteUrlEditText = findViewById(R.id.et_url);
        siteNameEditText = findViewById(R.id.et_site_name);
        categoryEditText = findViewById(R.id.et_category);
        memoEditText = findViewById(R.id.et_memo);
        registerButton = findViewById(R.id.btn_register);
        cancelButton = findViewById(R.id.btn_cancel);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        urlCounterTextView = findViewById(R.id.tv_url_counter);

        setupRequiredLabels();
        setupUrlLengthWatcher();

        registerButton.setOnClickListener(v -> {
            String siteUrl = siteUrlEditText.getText().toString().trim();
            String siteName = siteNameEditText.getText().toString().trim();

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

            if (siteName.isEmpty()) {
                siteNameEditText.setError("사이트 제목을 입력해주세요.");
                return;
            }

            // CategoryUtils로 카테고리 정규화 (빈 값이면 "기타"로 자동 설정)
            String category = CategoryUtils.normalizeCategory(
                categoryEditText.getText().toString()
            );

            // 메모 가져오기
            String memo = memoEditText.getText().toString().trim();

            registerSite(finalUrl, siteName, category, memo);
        });

        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupRequiredLabels() {
        applyRequiredMark((TextView) findViewById(R.id.tv_site_url_label));
        applyRequiredMark((TextView) findViewById(R.id.tv_site_name_label));
        applyRequiredMark((TextView) findViewById(R.id.tv_category_label));
    }

    private void applyRequiredMark(TextView label) {
        if (label == null) return;
        String original = label.getText() != null ? label.getText().toString().trim() : "";
        if (!original.contains("*")) {
            original = original + " *";
        }
        SpannableString spannableString = new SpannableString(original);
        int asteriskIndex = original.indexOf("*");
        if (asteriskIndex != -1) {
            spannableString.setSpan(
                    new ForegroundColorSpan(REQUIRED_MARK_COLOR),
                    asteriskIndex,
                    asteriskIndex + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        label.setText(spannableString);
    }

    private void setupUrlLengthWatcher() {
        siteUrlEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_URL_LENGTH)});
        updateUrlCounter(siteUrlEditText.getText().length());
        siteUrlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateUrlCounter(s.length());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateUrlCounter(int currentLength) {
        if (urlCounterTextView != null) {
            urlCounterTextView.setText(currentLength + "/" + MAX_URL_LENGTH);
        }
    }

    private void registerSite(String url, String siteName, String category, String memo) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            Toast.makeText(this, "로그인 상태를 확인할 수 없습니다. 다시 로그인해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        String userId = currentUser.getUid();

        Log.d("AddSiteActivity", "Registering site - URL: " + url + ", Name: " + siteName + ", Category: " + category + ", Memo: " + memo);

        String token = TokenManager.getBearerToken(this);
        SiteRegisterRequest request = new SiteRegisterRequest(url, siteName, userId, category, memo);
        Call<SiteRegisterResponse> call = ApiClient.getApiService().registerSite(token, request);

        call.enqueue(new Callback<SiteRegisterResponse>() {
            @Override
            public void onResponse(Call<SiteRegisterResponse> call, Response<SiteRegisterResponse> response) {
                showLoading(false);
                Log.d("AddSiteActivity", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    SiteRegisterResponse body = response.body();

                    Log.d("AddSiteActivity", "Registration successful! siteId: " + body.getSiteId());
                    Log.d("AddSiteActivity", "Message: " + body.getMessage());

                    Toast.makeText(AddSiteActivity.this, "사이트 등록 완료!\n크롤링이 끝나면 알림으로 알려드릴게요.", Toast.LENGTH_LONG).show();

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
            siteNameEditText.setEnabled(false);
            categoryEditText.setEnabled(false);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            cancelButton.setEnabled(true);
            siteUrlEditText.setEnabled(true);
            siteNameEditText.setEnabled(true);
            categoryEditText.setEnabled(true);
        }
    }
}
