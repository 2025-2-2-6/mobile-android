package com.example.mobile_android.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_android.R;
import com.example.mobile_android.databinding.FragmentSearchBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Tag Chips
        binding.chips.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        binding.chips.addItemDecoration(new SpaceItemDecoration(dp(6)));

        List<TagChip> tagData = new ArrayList<>();
        tagData.add(new TagChip("전체", 24, true));
        tagData.add(new TagChip("학교", 8, false));
        tagData.add(new TagChip("장학금", 5, false));
        tagData.add(new TagChip("공모전", 12, false));

        TagChipAdapter tagAdapter = new TagChipAdapter(tagData, (pos, item) -> {
            // Handle tag selection
        });
        binding.chips.setAdapter(tagAdapter);

        // Site List
        binding.rvSites.setLayoutManager(new LinearLayoutManager(getContext()));
        List<SearchFragment.SiteItem> siteData = Arrays.asList(
                new SearchFragment.SiteItem("서울대학교 공지사항","전체 공지사항 및 소식","학교","1,250명 구독 중","등록됨"),
                new SearchFragment.SiteItem("한국장학재단","국가장학금, 학자금대출 안내","장학금","8,920명 구독 중","+ 추가"),
                new SearchFragment.SiteItem("씽굿 공모전","대학생 공모전/아이디어/디자인","공모전","2,340명 구독 중","등록됨")
        );
        binding.rvSites.setAdapter(new SearchFragment.Adapter(siteData));
    }

    static class SiteItem {
        String title, desc, badge, stats, btnText;
        SiteItem(String t, String d, String b, String s, String bt) {
            title=t; desc=d; badge=b; stats=s; btnText=bt;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView t, d, b, s;
        Button btn;
        VH(@NonNull View v) {
            super(v);
            t=v.findViewById(R.id.tv_title);
            d=v.findViewById(R.id.tv_desc);
            b=v.findViewById(R.id.tv_badge);
            s=v.findViewById(R.id.tv_stats);
            btn=v.findViewById(R.id.btn_primary);
        }
    }

    static class Adapter extends RecyclerView.Adapter<VH> {
        private final List<SiteItem> data;
        Adapter(List<SiteItem> d){ data=d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vType){
            View v=LayoutInflater.from(p.getContext()).inflate(R.layout.item_site_card, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int i){
            SiteItem x=data.get(i);
            h.t.setText(x.title);
            h.d.setText(x.desc);
            h.b.setText(x.badge);
            h.s.setText(x.stats);
            h.btn.setText(x.btnText);
        }
        @Override public int getItemCount(){ return data.size(); }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
