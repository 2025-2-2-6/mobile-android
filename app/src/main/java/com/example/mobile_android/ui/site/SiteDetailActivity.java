package com.example.mobile_android.ui.site;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobile_android.R;
import com.example.mobile_android.model.Site;

public class SiteDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SITE_ID = "extra_site_id";
    public static final String EXTRA_SITE_NAME = "extra_site_name";
    public static final String EXTRA_SITE_URL = "extra_site_url";
    public static final String EXTRA_SITE_DESCRIPTION = "extra_site_description";

    private EditText etDisplayName;
    private TextView tvUrl;
    private TextView tvSummary;
    private Button btnSave;

    private String siteId;
    private String originalName;
    private String siteUrl;
    private String siteDescription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_detail);

        etDisplayName = findViewById(R.id.et_display_name);
        tvUrl        = findViewById(R.id.tv_url);
        tvSummary    = findViewById(R.id.tv_summary);
        btnSave      = findViewById(R.id.btn_save);

        // 👉 인텐트에서 값 꺼내기
        Intent intent = getIntent();
        siteId          = intent.getStringExtra(EXTRA_SITE_ID);
        originalName    = intent.getStringExtra(EXTRA_SITE_NAME);       // ★ 사용자가 입력한 사이트 이름
        siteUrl         = intent.getStringExtra(EXTRA_SITE_URL);        // 실제 링크
        siteDescription = intent.getStringExtra(EXTRA_SITE_DESCRIPTION); // AI 요약 (description)

        // 🔹 "사이트 이름" 칸에는 **name** 을 넣는다 (지금 URL 들어가 있던 부분 수정)
        if (originalName != null) {
            etDisplayName.setText(originalName);
        }

        // 🔹 "링크" 텍스트뷰에는 url 표시
        if (siteUrl != null) {
            tvUrl.setText(siteUrl);
        }

        // 🔹 "AI 요약" 텍스트뷰에는 description 표시
        if (siteDescription != null && !siteDescription.isEmpty()) {
            tvSummary.setText(siteDescription);
        } else {
            tvSummary.setText("이 사이트에서 최근에 올라온 게시글 요약이 여기 표시됩니다.");
        }

        // 🔹 URL 클릭 시 브라우저로 이동 (수정은 불가)
        tvUrl.setOnClickListener(v -> {
            if (siteUrl != null && !siteUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "이동할 링크가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔹 저장 버튼: 이름만 바꿔서 되돌려주기 (백엔드 수정은 아직 안 함)
        btnSave.setOnClickListener(v -> {
            String newName = etDisplayName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "사이트 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 결과를 호출한 액티비티( SiteManageActivity )로 돌려보내고 닫기
            Intent result = new Intent();
            result.putExtra(EXTRA_SITE_ID, siteId);
            result.putExtra(EXTRA_SITE_NAME, newName);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
