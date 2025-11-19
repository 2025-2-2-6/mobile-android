package com.example.mobile_android.ui.mypage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile_android.R;
import com.example.mobile_android.databinding.FragmentSearchBinding;
import com.example.mobile_android.model.SiteSummary;
import com.example.mobile_android.network.ApiService;
import com.example.mobile_android.retrofit.RetrofitClient;
import com.example.mobile_android.ui.search.SiteManageAdapter;
import com.example.mobile_android.ui.search.SpaceItemDecoration;
import com.example.mobile_android.ui.search.TagChip;
import com.example.mobile_android.ui.search.TagChipAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SiteManageFragment extends Fragment {

    private FragmentSearchBinding binding; // 검색 탭이랑 같은 레이아웃 사용
    private SiteManageAdapter adapter;
    private final List<SiteSummary> data = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔹 1) 상단 필터 칩 UI (검색탭이랑 동일하게 세팅)
        setupChips();

        // 🔹 2) 사이트 리스트 RecyclerView
        binding.rvSites.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SiteManageAdapter(data, site -> {
            // 카드 눌렀을 때 상세 화면으로 이동
            Bundle args = new Bundle();
            args.putString("siteId", site.site_id);   // ✅ site.id 넘기기
            Navigation.findNavController(view)
                    .navigate(R.id.nav_site_detail, args);
        });
        binding.rvSites.setAdapter(adapter);

        // 🔹 3) 서버에서 사이트 목록 불러오기
        loadSites();
    }

    private void setupChips() {
        binding.chips.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.chips.addItemDecoration(new SpaceItemDecoration(dp(6)));

        List<TagChip> tagData = new ArrayList<>();
        tagData.add(new TagChip("전체", 0, true));
        tagData.add(new TagChip("학교", 0, false));
        tagData.add(new TagChip("장학금", 0, false));
        tagData.add(new TagChip("공모전", 0, false));

        TagChipAdapter tagAdapter = new TagChipAdapter(tagData, (pos, item) -> {
            // TODO: 카테고리별 필터 필요하면 여기에서 구현
        });
        binding.chips.setAdapter(tagAdapter);
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    private void loadSites() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        // only_public=false (전체 사이트), 공용만 보고 싶으면 true
        api.getSites(false).enqueue(new Callback<List<SiteSummary>>() {
            @Override
            public void onResponse(Call<List<SiteSummary>> call,
                                   Response<List<SiteSummary>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    data.clear();
                    data.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(),
                            "사이트 목록 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<SiteSummary>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        "서버 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
