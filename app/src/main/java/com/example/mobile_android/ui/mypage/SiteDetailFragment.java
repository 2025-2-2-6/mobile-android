package com.example.mobile_android.ui.mypage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mobile_android.databinding.FragmentSiteDetailBinding;
import com.example.mobile_android.model.SiteDetailResponse;
import com.example.mobile_android.model.UpdateSiteRequest;
import com.example.mobile_android.network.ApiService;
import com.example.mobile_android.retrofit.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SiteDetailFragment extends Fragment {

    private FragmentSiteDetailBinding binding;
    private String siteId;
    private String siteUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentSiteDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        siteId = requireArguments().getString("siteId");

        loadDetail();   // 사이트 상세 + 요약(description)

        // URL 클릭 → 외부 브라우저 이동
        binding.tvUrl.setOnClickListener(v -> {
            if (siteUrl != null && !siteUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(siteUrl));
                startActivity(intent);
            }
        });

        // 저장 버튼 -> 이름 변경
        binding.btnSave.setOnClickListener(v -> save());
    }

    /** 사이트 상세 정보 로드 */
    private void loadDetail() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getSiteDetail(siteId).enqueue(new Callback<SiteDetailResponse>() {
            @Override
            public void onResponse(Call<SiteDetailResponse> call,
                                   Response<SiteDetailResponse> response) {

                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    SiteDetailResponse d = response.body();

                    // 이름
                    binding.etDisplayName.setText(d.name);

                    // URL
                    binding.tvUrl.setText(d.url);
                    siteUrl = d.url;

                    // ⭐ 요약 영역 = 사이트 description
                    if (d.description != null && !d.description.isEmpty()) {
                        binding.tvSummary.setText(d.description);
                    } else {
                        binding.tvSummary.setText("요약 정보가 없습니다.");
                    }

                } else {
                    Toast.makeText(requireContext(),
                            "상세 정보 불러오기 실패", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(SiteDetailFragment.this).popBackStack();
                }
            }

            @Override
            public void onFailure(Call<SiteDetailResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(SiteDetailFragment.this).popBackStack();
            }
        });
    }

    /** PATCH /api/v1/sites/{siteId} : 이름 변경 */
    private void save() {

        String newName = binding.etDisplayName.getText().toString();

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        UpdateSiteRequest body = new UpdateSiteRequest(newName, null);

        api.updateSite(siteId, body).enqueue(new Callback<SiteDetailResponse>() {
            @Override
            public void onResponse(Call<SiteDetailResponse> call,
                                   Response<SiteDetailResponse> response) {

                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "저장 완료", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(SiteDetailFragment.this).popBackStack();
                } else {
                    Toast.makeText(requireContext(), "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SiteDetailResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "서버 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
