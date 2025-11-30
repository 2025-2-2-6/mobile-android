# 사이트 상세 페이지(SiteDetailActivity) 디자인 개선 가이드

> **작성일**: 2025-01-XX
> **Context 제한으로 인한 상세 가이드 문서**

---

## 📋 요구사항 요약

### 1. 레이아웃 개선
- [ ] **iv_site_icon 제거** (사이트 아이콘 ImageView)
- [ ] **"수집중" 카드 제거**
- [ ] **헤더 수정/삭제 버튼을 item_site.xml과 통일**
- [ ] **카테고리를 Chip으로 변경** (InstagramFilterChip 스타일)

### 2. 통계 표시 개선
- [ ] **"전체" → "게시물"로 문구 변경**
- [ ] **해당 사이트의 총 게시물 수 표시**
- [ ] **is_new인 게시물 수 표시**

### 3. 로딩 및 데이터 갱신
- [ ] **스켈레톤 UI 구현** (로딩 중 회색 placeholder)
- [ ] **페이지 진입 시 게시물 수 즉시 업데이트**
  - 현재: 캘린더 알림 토글해야 반영됨
  - 개선: onCreate/onResume에서 바로 로드

---

## 🎨 1. activity_site_detail.xml 수정

### 1.1 iv_site_icon 제거 (라인 94-101)

**제거할 코드:**
```xml
<ImageView
    android:id="@+id/iv_site_icon"
    android:layout_width="50dp"
    android:layout_height="50dp"
    android:src="@drawable/ic_site_placeholder"
    android:layout_marginEnd="12dp"
    android:contentDescription="사이트 아이콘" />
```

**이유**: 사용자가 제거 요청

---

### 1.2 헤더 수정/삭제 버튼 통일 (라인 45-67)

**기존 코드:**
```xml
<LinearLayout
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="end"
    android:layout_marginEnd="8dp"
    android:orientation="horizontal">

    <ImageButton
        android:id="@+id/btn_edit"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:src="@android:drawable/ic_menu_edit"
        android:contentDescription="수정" />

    <ImageButton
        android:id="@+id/btn_delete"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:src="@drawable/ic_delete"
        android:contentDescription="삭제" />
</LinearLayout>
```

**수정 후 (item_site.xml 스타일):**
```xml
<LinearLayout
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="end"
    android:layout_marginEnd="8dp"
    android:orientation="horizontal">

    <ImageView
        android:id="@+id/btn_edit"
        android:layout_width="36dp"
        android:layout_height="36dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:clickable="true"
        android:focusable="true"
        android:padding="6dp"
        android:src="@drawable/ic_edit_24"
        android:tint="#888888"
        android:contentDescription="수정" />

    <ImageView
        android:id="@+id/btn_delete"
        android:layout_width="36dp"
        android:layout_height="36dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:clickable="true"
        android:focusable="true"
        android:padding="6dp"
        android:src="@drawable/ic_delete_24"
        android:tint="#FF5252"
        android:contentDescription="삭제" />
</LinearLayout>
```

**변경 사항:**
- ImageButton → ImageView
- 크기: 40dp → 36dp
- padding: 6dp 추가
- 아이콘: ic_edit_24, ic_delete_24 사용
- tint 적용: 수정(#888888), 삭제(#FF5252)

---

### 1.3 카테고리를 Chip으로 변경 (라인 130-141)

**기존 코드:**
```xml
<TextView
    android:id="@+id/tv_category"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="창업"
    android:textColor="#FFFFFF"
    android:background="@drawable/bg_category_badge"
    android:paddingHorizontal="14dp"
    android:paddingVertical="7dp"
    android:textSize="13sp"
    android:textStyle="bold"
    android:elevation="0dp" />
```

**수정 후:**
```xml
<com.google.android.material.chip.Chip
    android:id="@+id/chip_category"
    style="@style/InstagramFilterChip"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="창업"
    android:checkable="false"
    android:clickable="false"
    android:focusable="false" />
```

**InstagramFilterChip 스타일 (이미 존재):**
- 파일: `res/values/styles.xml`
- 배경: chip_background_state (선택 시 #4C84FF, 기본 #F5F6FA)
- 테두리: chip_stroke_state
- 텍스트: chip_text_state

---

### 1.4 "수집중" 카드 제거 (라인 240-273)

**제거할 전체 LinearLayout:**
```xml
<!-- 수집중 -->
<LinearLayout
    android:layout_width="0dp"
    android:layout_weight="1"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="16dp"
    android:background="@drawable/bg_stat_box">

    <ImageView
        android:id="@+id/iv_crawling_status"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@android:drawable/ic_popup_sync"
        app:tint="#FF9800"
        android:contentDescription="수집중" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text=""
        android:textColor="@android:color/black"
        android:textSize="20sp"
        android:textStyle="bold"
        android:layout_marginTop="4dp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="수집중"
        android:textColor="#666666"
        android:textSize="12sp" />
</LinearLayout>
```

**부모 LinearLayout 조정:**
- 기존: 3칸 (전체, 신규, 수집중)
- 수정: 2칸 (게시물, 신규)
- `android:weightSum="3"` → `android:weightSum="2"` (있다면)

---

### 1.5 통계 문구 변경

**"전체" → "게시물" (라인 약 220)**

```xml
<!-- 기존 -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="전체"
    android:textColor="#666666"
    android:textSize="12sp" />

<!-- 수정 후 -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="게시물"
    android:textColor="#666666"
    android:textSize="12sp" />
```

---

### 1.6 스켈레톤 UI 추가

**ConstraintLayout 최상위에 추가:**

```xml
<!-- 스켈레톤 로딩 레이아웃 -->
<LinearLayout
    android:id="@+id/skeleton_loading"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="20dp"
    android:background="#FFFFFF"
    android:visibility="gone"
    app:layout_constraintTop_toTopOf="parent">

    <!-- 헤더 스켈레톤 -->
    <View
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="@drawable/bg_skeleton_item"
        android:layout_marginBottom="16dp" />

    <!-- 사이트 정보 스켈레톤 -->
    <View
        android:layout_width="200dp"
        android:layout_height="28dp"
        android:background="@drawable/bg_skeleton_item"
        android:layout_marginBottom="8dp" />

    <View
        android:layout_width="150dp"
        android:layout_height="20dp"
        android:background="@drawable/bg_skeleton_item"
        android:layout_marginBottom="16dp" />

    <!-- 통계 카드 스켈레톤 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="100dp"
        android:orientation="horizontal"
        android:layout_marginTop="16dp">

        <View
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_skeleton_item"
            android:layout_marginEnd="8dp" />

        <View
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/bg_skeleton_item"
            android:layout_marginStart="8dp" />
    </LinearLayout>

    <!-- 게시물 리스트 스켈레톤 -->
    <View
        android:layout_width="match_parent"
        android:layout_height="80dp"
        android:background="@drawable/bg_skeleton_item"
        android:layout_marginTop="16dp" />
</LinearLayout>
```

**bg_skeleton_item.xml 생성:**
```xml
<!-- res/drawable/bg_skeleton_item.xml -->
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#F0F0F0" />
    <corners android:radius="8dp" />
</shape>
```

---

## 💻 2. SiteDetailActivity.java 수정

### 2.1 멤버 변수 추가

```java
private View skeletonLoading;
private View contentLayout;  // 메인 콘텐츠 레이아웃
```

### 2.2 initViews() 수정

```java
private void initViews() {
    btnBack = findViewById(R.id.btn_back);
    btnEdit = findViewById(R.id.btn_edit);
    btnDelete = findViewById(R.id.btn_delete);
    // ivSiteIcon 제거
    tvSiteName = findViewById(R.id.tv_site_name);
    // tvCategory 제거, chipCategory 추가
    Chip chipCategory = findViewById(R.id.chip_category);
    tvUrl = findViewById(R.id.tv_url);
    tvLastCrawl = findViewById(R.id.tv_last_crawl);
    tvTotalCount = findViewById(R.id.tv_total_count);
    tvNewCount = findViewById(R.id.tv_new_count);
    // ivCrawlingStatus 제거
    rvRecentPosts = findViewById(R.id.rv_recent_posts);
    tvViewAll = findViewById(R.id.tv_view_all);

    // 스켈레톤 UI
    skeletonLoading = findViewById(R.id.skeleton_loading);
    contentLayout = findViewById(R.id.content_layout);  // 메인 콘텐츠를 감싸는 레이아웃 ID

    rvRecentPosts.setLayoutManager(new LinearLayoutManager(this));
    recentPostsAdapter = new PostAdapter(this, recentPostsList);
    rvRecentPosts.setAdapter(recentPostsAdapter);
}
```

### 2.3 loadSiteDetails() 수정

**기존 문제:**
- 실패 시 더미 데이터 표시
- 로딩 인디케이터 없음

**개선 후:**

```java
private void loadSiteDetails() {
    // 스켈레톤 표시
    showSkeleton(true);

    String token = TokenManager.getBearerToken(this);
    ApiClient.getApiService().getSiteById(token, siteId).enqueue(new Callback<Site>() {
        @Override
        public void onResponse(Call<Site> call, Response<Site> response) {
            // 스켈레톤 숨김
            showSkeleton(false);

            if (response.isSuccessful() && response.body() != null) {
                site = response.body();
                updateUI();
            } else {
                Toast.makeText(SiteDetailActivity.this,
                    "사이트 정보를 불러올 수 없습니다",
                    Toast.LENGTH_SHORT).show();
                // 더미 데이터 대신 그냥 종료
                finish();
            }
        }

        @Override
        public void onFailure(Call<Site> call, Throwable t) {
            showSkeleton(false);
            Toast.makeText(SiteDetailActivity.this,
                "네트워크 오류: " + t.getMessage(),
                Toast.LENGTH_SHORT).show();
            finish();
        }
    });
}

private void showSkeleton(boolean show) {
    if (show) {
        skeletonLoading.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
    } else {
        skeletonLoading.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }
}
```

### 2.4 createDummySite() 제거

```java
// 이 메서드 전체 삭제
private Site createDummySite() {
    // ...
}
```

### 2.5 updateUI() 수정

**카테고리 TextView → Chip 변경:**

```java
private void updateUI() {
    if (site == null) return;

    tvSiteName.setText(site.getName() != null ? site.getName() : "이름 없음");

    // TextView 대신 Chip 사용
    Chip chipCategory = findViewById(R.id.chip_category);
    String category = site.getCategory() != null ? site.getCategory() : "기타";
    chipCategory.setText(category);

    tvUrl.setText(site.getUrl() != null ?
        site.getUrl().replace("https://", "").replace("http://", "") : "");

    // 마지막 수집 시간
    tvLastCrawl.setText("마지막 수집: " +
        (site.getCreatedAt() != null ? site.getCreatedAt().substring(0, 10) : "알 수 없음"));

    // 통계는 loadRecentPosts()에서 업데이트됨
}
```

### 2.6 loadRecentPosts() - 카운트 즉시 반영

**현재 문제:** 캘린더 알림 토글해야 카운트 업데이트됨

**원인 분석:**
- onCreate()에서 `loadRecentPosts()` 호출은 정상
- 하지만 LiveData observe가 비동기라서 초기 로드가 느릴 수 있음
- PostAdapter의 알림 상태 변경 시 DB 업데이트 → LiveData 재발행 → 카운트 업데이트

**해결 방법:**
```java
private void loadRecentPosts() {
    // PostDao를 사용해 로컬 DB에서 해당 사이트의 최신 4개 게시물 조회
    postDao.getPostsBySite(siteId).observe(this, posts -> {
        if (posts != null && !posts.isEmpty()) {
            // 최대 4개만 표시
            List<Post> recentPosts = posts.stream()
                    .limit(4)
                    .collect(Collectors.toList());

            recentPostsList.clear();
            recentPostsList.addAll(recentPosts);
            recentPostsAdapter.notifyDataSetChanged();

            // ✅ 통계 업데이트 (전체 게시물 수)
            tvTotalCount.setText(String.valueOf(posts.size()));

            // ✅ 새 게시물 수 계산 (is_new)
            long newCount = posts.stream()
                    .filter(Post::isActuallyNew)
                    .count();
            tvNewCount.setText(String.valueOf(newCount));

            Log.d("SiteDetailActivity", "게시물 수 업데이트 - 전체: " + posts.size() + ", 신규: " + newCount);
        } else {
            recentPostsList.clear();
            recentPostsAdapter.notifyDataSetChanged();
            tvTotalCount.setText("0");
            tvNewCount.setText("0");
        }
    });
}
```

**추가 개선 (선택사항):**

onResume()에서 강제 새로고침:
```java
@Override
protected void onResume() {
    super.onResume();
    // 다른 화면에서 돌아왔을 때 게시물 목록 새로고침
    loadRecentPosts();
}
```

---

## 📝 3. strings.xml 업데이트 (선택사항)

```xml
<!-- res/values/strings.xml -->
<string name="site_detail_stat_posts">게시물</string>
<string name="site_detail_stat_new">신규</string>
```

XML에서 사용:
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/site_detail_stat_posts"
    android:textColor="#666666"
    android:textSize="12sp" />
```

---

## 🔍 4. 단계별 구현 순서

### Phase 1: 레이아웃 수정 (activity_site_detail.xml)
1. ✅ iv_site_icon 제거
2. ✅ 헤더 버튼 통일 (ImageButton → ImageView)
3. ✅ "수집중" 카드 제거
4. ✅ 카테고리 TextView → Chip 변경
5. ✅ "전체" → "게시물" 문구 변경
6. ✅ 스켈레톤 UI 레이아웃 추가
7. ✅ bg_skeleton_item.xml drawable 생성

### Phase 2: Java 코드 수정 (SiteDetailActivity.java)
1. ✅ 멤버 변수 추가 (skeletonLoading, contentLayout)
2. ✅ initViews() 수정 (제거된 뷰 정리)
3. ✅ showSkeleton() 메서드 추가
4. ✅ loadSiteDetails() 수정 (스켈레톤 표시/숨김)
5. ✅ createDummySite() 메서드 제거
6. ✅ updateUI() 수정 (Chip 사용)
7. ✅ loadRecentPosts() 로그 추가 (디버깅용)
8. ✅ onResume() 추가 (선택사항)

### Phase 3: 테스트
1. ✅ 사이트 상세 페이지 진입 시 스켈레톤 표시 확인
2. ✅ 사이트 정보 로드 후 정상 표시 확인
3. ✅ 게시물 수/신규 수 즉시 표시 확인
4. ✅ 수정/삭제 버튼 item_site와 동일한 디자인 확인
5. ✅ 카테고리 Chip 스타일 확인
6. ✅ API 실패 시 graceful 종료 확인

---

## ⚠️ 예상 이슈 및 해결

### 이슈 1: 게시물 수가 0으로 표시됨
**원인:** DB에 게시물이 없음
**해결:** PostListActivity에서 서버 데이터 동기화 후 확인

### 이슈 2: 스켈레톤이 너무 오래 표시됨
**원인:** API 응답이 느림
**해결:** Timeout 설정 추가 (OkHttp)

### 이슈 3: Chip 스타일이 안 먹힘
**원인:** InstagramFilterChip 스타일 누락
**해결:** styles.xml 확인, Material Components 의존성 확인

### 이슈 4: iv_site_icon 제거 후 레이아웃 깨짐
**원인:** Constraint 연결 문제
**해결:** constraint를 parent로 직접 연결

---

## 📦 필요한 리소스 파일 체크리스트

- [x] `@drawable/ic_edit_24` (존재 확인됨)
- [x] `@drawable/ic_delete_24` (존재 확인됨)
- [ ] `@drawable/bg_skeleton_item` (새로 생성)
- [x] `@style/InstagramFilterChip` (존재 확인됨)
- [x] `@color/chip_background_state` (존재 확인됨)
- [x] `@color/chip_stroke_state` (존재 확인됨)
- [x] `@color/chip_text_state` (존재 확인됨)

---

## 🎯 최종 목표

1. **사용자 경험 개선**
   - 로딩 중 스켈레톤 UI로 부드러운 전환
   - 더미 데이터 대신 실제 데이터만 표시
   - 페이지 진입 시 즉시 게시물 수 표시

2. **디자인 일관성**
   - item_site와 동일한 버튼 스타일
   - 전체 앱에서 사용하는 Chip 디자인 통일
   - 불필요한 요소 제거 (사이트 아이콘, 수집중 카드)

3. **성능 최적화**
   - LiveData 활용한 반응형 UI
   - 불필요한 API 호출 제거

---

**다음 세션에서 이 가이드를 참고하여 구현하세요!**
