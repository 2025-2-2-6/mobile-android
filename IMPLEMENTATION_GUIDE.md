# 알림 및 게시물 기능 구현 가이드

## 📋 목차
1. [개요](#개요)
2. [구현된 기능](#구현된-기능)
3. [파일 구조](#파일-구조)
4. [상세 구현 내용](#상세-구현-내용)
5. [API 연동](#api-연동)
6. [FCM 연동](#fcm-연동)
7. [캘린더 연동](#캘린더-연동)
8. [사용 방법](#사용-방법)
9. [추가 작업 필요 사항](#추가-작업-필요-사항)

---

## 개요

이 문서는 모바일 앱의 **알림 시스템**, **게시물 목록**, **게시물 상세** 페이지 구현 내용을 정리한 문서입니다.

### 구현 목표
- 백엔드(FastAPI + Celery + Redis)에서 전송하는 FCM 알림 수신
- 사이트 등록 시 비동기 처리 완료 알림
- 게시물 정보 표시 및 캘린더 연동
- 사용자에게 직관적인 UI/UX 제공

### 디자인 참고
프로젝트 루트에 있는 3개의 스크린샷 이미지를 참고하여 구현:
- `notification_page.jpeg` - 알림 페이지
- `post_list_page.jpeg` - 게시물 목록
- `post_detail_page.jpeg` - 게시물 상세

---

## 구현된 기능

### 1. 알림 시스템
- ✅ 탭 기반 필터링 (전체, 게시물 알림, 마감, 새 항목)
- ✅ 알림 타입별 아이콘 및 색상 구분
- ✅ 읽음/안읽음 상태 표시
- ✅ 상대 시간 표시 ("5분 전", "1시간 전" 등)
- ✅ 알림 클릭 시 해당 컨텐츠로 이동
- ✅ 개별/전체 알림 삭제 기능

### 2. 게시물 목록
- ✅ 탭 기반 필터링 (전체, 새 게시물, 저장됨)
- ✅ NEW 배지 (24시간 이내 게시물)
- ✅ D-day 배지 (7일 이내 마감/일정)
- ✅ 카테고리 태그 표시
- ✅ 캘린더 토글 스위치
- ✅ 게시물 클릭 시 상세 페이지 이동

### 3. 게시물 상세
- ✅ 제목, 카테고리, 날짜, 장소 표시
- ✅ 본문 내용 전체 표시
- ✅ 원문 보기 버튼 (외부 브라우저)
- ✅ 일정 추가/삭제 기능
- ✅ 캘린더 자동 알림 설정 (1일 전, 1시간 전)

### 4. FCM 푸시 알림
- ✅ 새 게시물 알림
- ✅ 사이트 등록 완료 알림
- ✅ 일정/마감 리마인더
- ✅ 알림 클릭 시 적절한 페이지로 이동

### 5. 캘린더 연동
- ✅ Android Calendar Provider API 사용
- ✅ 일정 추가/삭제
- ✅ 일정 ID SharedPreferences 저장
- ✅ 목록과 상세 페이지 동기화

---

## 파일 구조

```
mobile-android/
├── app/src/main/
│   ├── java/com/example/mobile_android/
│   │   ├── config/
│   │   │   └── AppConfig.java                    # 전역 설정 관리
│   │   │
│   │   ├── model/
│   │   │   ├── Notification.java                 # 알림 데이터 모델
│   │   │   └── Post.java                         # 게시물 데이터 모델 (기존)
│   │   │
│   │   ├── network/
│   │   │   ├── ApiClient.java                    # Retrofit 클라이언트 (수정)
│   │   │   └── ApiService.java                   # API 인터페이스 (수정)
│   │   │
│   │   ├── ui/
│   │   │   ├── notification/
│   │   │   │   ├── NotificationFragment.java     # 알림 페이지
│   │   │   │   └── NotificationAdapter.java      # 알림 어댑터
│   │   │   │
│   │   │   └── post/
│   │   │       ├── PostAdapter.java              # 게시물 어댑터 (개선)
│   │   │       ├── PostListActivity.java         # 게시물 목록 (기존)
│   │   │       └── PostDetailActivity.java       # 게시물 상세 (신규)
│   │   │
│   │   ├── util/
│   │   │   └── CalendarManager.java              # 캘린더 연동 유틸리티
│   │   │
│   │   └── fcm/
│   │       └── MyFirebaseMessagingService.java   # FCM 서비스
│   │
│   ├── res/
│   │   ├── layout/
│   │   │   ├── fragment_notification.xml         # 알림 페이지 레이아웃
│   │   │   ├── item_notification.xml             # 알림 아이템
│   │   │   ├── activity_post_list.xml            # 게시물 목록 레이아웃
│   │   │   ├── item_post.xml                     # 게시물 아이템
│   │   │   └── activity_post_detail.xml          # 게시물 상세 레이아웃
│   │   │
│   │   ├── drawable/
│   │   │   ├── ic_notification_post.xml          # 게시물 알림 아이콘
│   │   │   ├── ic_notification_deadline.xml      # 마감 알림 아이콘
│   │   │   ├── ic_notification_new.xml           # 새 항목 알림 아이콘
│   │   │   ├── ic_notification_calendar.xml      # 일정 알림 아이콘
│   │   │   ├── bg_notification_icon_blue.xml     # 파란색 배경
│   │   │   ├── bg_notification_icon_red.xml      # 빨간색 배경
│   │   │   ├── bg_notification_icon_green.xml    # 초록색 배경
│   │   │   ├── bg_notification_icon_purple.xml   # 보라색 배경
│   │   │   └── bg_notification_unread_dot.xml    # 읽지 않음 표시
│   │   │
│   │   └── values/
│   │       └── themes.xml                        # 테마 설정 (다크 모드 비활성화)
│   │
│   └── AndroidManifest.xml                       # 권한 및 서비스 등록
│
├── BASE_URL_GUIDE.md                             # Base URL 설정 가이드
├── IMPLEMENTATION_GUIDE.md                       # 이 문서
└── mobile_app_fcm_guide.md                       # FCM 통합 가이드 (기존)
```

---

## 상세 구현 내용

### 1. 데이터 모델

#### Notification.java
```java
public class Notification {
    private String id;              // 알림 ID
    private String userId;          // 사용자 ID
    private String type;            // 알림 타입 (new_post, site_registered, event_reminder, deadline)
    private String title;           // 알림 제목
    private String message;         // 알림 내용
    private String postId;          // 게시물 ID (nullable)
    private String siteId;          // 사이트 ID (nullable)
    private boolean isRead;         // 읽음 여부
    private String createdAt;       // 생성 시간
}
```

**알림 타입**:
- `new_post` - 새 게시물 알림 (파란색 별 아이콘)
- `site_registered` - 사이트 등록 완료 (초록색 벨 아이콘)
- `event_reminder` - 일정 리마인더 (보라색 캘린더 아이콘)
- `deadline` - 마감 임박 알림 (빨간색 경고 아이콘)

#### Post.java (기존 모델 활용)
```java
public class Post {
    private String id;              // UUID (36자)
    private String siteId;          // 사이트 ID (36자)
    private String title;           // 제목 (최대 512자)
    private String content;         // 본문
    private String sourceUrl;       // 원문 URL (nullable)
    private String eventDate;       // 일정/마감일 (nullable)
    private String location;        // 장소 (nullable)
    private String createdAt;       // 생성 시간
    private String updatedAt;       // 수정 시간
}
```

---

### 2. API 엔드포인트

#### ApiService.java 추가된 메서드

```java
// 알림 목록 조회
@GET("/api/v1/notifications")
Call<List<Notification>> getNotifications(
    @Query("user_id") String userId,
    @Query("type") String type,
    @Query("is_read") Boolean isRead
);

// 알림 읽음 처리
@POST("/api/v1/notifications/{notification_id}/read")
Call<Void> markNotificationAsRead(@Path("notification_id") String notificationId);

// 게시물 상세 조회
@GET("/api/v1/posts/{post_id}")
Call<Post> getPostDetail(@Path("post_id") String postId);

// 게시물 목록 조회 (기존)
@GET("/api/v1/posts/list")
Call<PostListResponse> getPosts(...);
```

**백엔드 API 스펙**: `mobile_app_fcm_guide.md` 참조

---

### 3. UI 레이아웃

#### 알림 페이지 (fragment_notification.xml)

**구조**:
```
┌─────────────────────────────────┐
│ [←] 알림    [필터] [모두읽음]    │  ← 헤더
├─────────────────────────────────┤
│ 읽지 않은 알림 3개               │
├─────────────────────────────────┤
│ [전체] [게시물] [마감] [새항목]  │  ← 탭
├─────────────────────────────────┤
│ 5개의 알림     읽은 알림 모두 삭제│
├─────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ 🔵 AI 요약 완료        • │ │
│ │    재학생 장학금...         │ │
│ │    5분 전              🗑  │ │
│ └────────────────────────────┘ │
│ ┌────────────────────────────┐ │
│ │ 🔴 마감 임박            • │ │
│ │    네이버 클라우드...       │ │
│ │    1시간 전            🗑  │ │
│ └────────────────────────────┘ │
│ ...                             │
└─────────────────────────────────┘
```

**주요 요소**:
- `TabLayout` - 필터 탭
- `RecyclerView` - 알림 리스트
- 읽지 않은 알림 카운트
- 읽은 알림 모두 삭제 버튼

#### 게시물 목록 (activity_post_list.xml)

**구조**:
```
┌─────────────────────────────────┐
│ [≡] 게시물              [🔔]    │  ← 헤더
├─────────────────────────────────┤
│ [      게시물 검색...      ]    │  ← 검색창
├─────────────────────────────────┤
│ [전체] [새 게시물] [저장됨]      │  ← 탭
├─────────────────────────────────┤
│ 📅 캘린더 등록된 게시물 3개  [>] │  ← 캘린더 정보 (선택)
├─────────────────────────────────┤
│ ┌────────────────────────────┐ │
│ │ 학과·컴공  [NEW] [D-3]     │ │
│ │                             │ │
│ │ 2025-1학기 수강신청 안내    │ │
│ │                             │ │
│ │ 11월 18일 오후 02:00        │ │
│ │ 📍 캘린더 등록       [토글] │ │
│ └────────────────────────────┘ │
│ ...                             │
└─────────────────────────────────┘
```

**주요 요소**:
- 카테고리 태그 (배경색)
- NEW 배지 (24시간 이내)
- D-day 배지 (7일 이내)
- 캘린더 토글 스위치
- 위치 정보 (선택)

#### 게시물 상세 (activity_post_detail.xml)

**구조**:
```
┌─────────────────────────────────┐
│ [←] 게시물 상세                 │  ← 헤더
├─────────────────────────────────┤
│ 2025-1학기 수강신청 안내         │  ← 제목
│                                 │
│ 학과·컴퓨터공학과 공지사항       │  ← 카테고리
│                                 │
│ 🕐 2025년 11월 18일 오후 02:00  │  ← 게시일
│ 📅 마감일: 2025년 11월 25일...  │  ← 마감일 (선택)
│ 📍 컴퓨터공학과 사무실           │  ← 장소 (선택)
├─────────────────────────────────┤
│ [본문 내용]                     │
│ 2025학년도 1학기 수강신청...    │
│ ...                             │
│                                 │
├─────────────────────────────────┤
│ 원문 확인하기                   │
│ 브라우저에서 게시물 원문을...    │
│ ┌──────────────────────────┐   │
│ │  🔗 게시글 직접 보기      │   │  ← source_url 있을 때만
│ └──────────────────────────┘   │
├─────────────────────────────────┤
│ 캘린더에 추가하기               │
│ 마감일을 캘린더에 추가하여...    │
│ ┌──────────────────────────┐   │
│ │  📅 일정 추가하기         │   │  ← event_date 있을 때만
│ └──────────────────────────┘   │
└─────────────────────────────────┘
```

---

### 4. Adapter 구현

#### NotificationAdapter.java

**주요 기능**:
```java
public class NotificationAdapter {
    // 알림 타입별 아이콘 설정
    private void setNotificationIcon(ImageView imageView, String type) {
        switch (type) {
            case "new_post":      // 파란색 별
            case "deadline":      // 빨간색 경고
            case "site_registered": // 초록색 벨
            case "event_reminder": // 보라색 캘린더
        }
    }

    // 상대 시간 표시
    private String getRelativeTimeString(String createdAt) {
        // "방금 전", "5분 전", "1시간 전", "3일 전", "11월 15일"
    }
}
```

**클릭 이벤트**:
- 알림 클릭 → 해당 컨텐츠로 이동
- 삭제 버튼 → 알림 삭제

#### PostAdapter.java

**주요 기능**:
```java
public class PostAdapter {
    // NEW 배지 (24시간 이내)
    private boolean isNew(String createdAt) {
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        return hours <= 24;
    }

    // D-day 배지 (7일 이내)
    private int getDaysUntil(String eventDateStr) {
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return days >= 0 && days <= 7;
    }

    // 캘린더 토글
    switchCalendar.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked) {
            calendarManager.addEventToCalendar(...);
        } else {
            calendarManager.removeEventFromCalendar(...);
        }
    });
}
```

---

### 5. Activity/Fragment 구현

#### NotificationFragment.java

**주요 메서드**:
```java
// 알림 목록 불러오기
private void loadNotifications() {
    apiService.getNotifications(userId, null, null)
        .enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(...) {
                notificationList.addAll(response.body());
                filterNotifications();
            }
        });
}

// 탭별 필터링
private void filterNotifications() {
    if ("all".equals(currentFilter)) {
        filteredList.addAll(notificationList);
    } else if ("unread".equals(currentFilter)) {
        // 읽지 않은 알림만
    } else {
        // 타입별 필터링
    }
}

// 알림 클릭 처리
private void handleNotificationClick(Notification notification) {
    if (notification.getPostId() != null) {
        // 게시물 상세로 이동
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("POST_ID", notification.getPostId());
        startActivity(intent);
    }
}
```

#### PostDetailActivity.java

**주요 메서드**:
```java
// 게시물 상세 불러오기
private void loadPostDetail(String postId) {
    apiService.getPostDetail(postId)
        .enqueue(new Callback<Post>() {
            @Override
            public void onResponse(...) {
                displayPostDetail(response.body());
            }
        });
}

// 원문 보기
btnViewSource.setOnClickListener(v -> {
    Intent browserIntent = new Intent(
        Intent.ACTION_VIEW,
        Uri.parse(post.getSourceUrl())
    );
    startActivity(browserIntent);
});

// 캘린더 토글
private void handleCalendarToggle() {
    if (calendarManager.isEventRegistered(postId)) {
        calendarManager.removeEventFromCalendar(postId);
    } else {
        calendarManager.addEventToCalendar(
            postId, title, eventDate, location
        );
    }
}
```

---

## API 연동

### 백엔드 엔드포인트 (FastAPI)

#### 1. 알림 API

**GET /api/v1/notifications**
```json
// Request
GET /api/v1/notifications?user_id=<user_id>&type=new_post&is_read=false

// Response
[
  {
    "id": "uuid",
    "user_id": "user123",
    "type": "new_post",
    "title": "새 게시물 알림",
    "message": "컴퓨터공학과에 새로운 공지사항이 등록되었습니다",
    "post_id": "post-uuid",
    "site_id": "site-uuid",
    "is_read": false,
    "created_at": "2025-01-19T10:30:00"
  }
]
```

**POST /api/v1/notifications/{notification_id}/read**
```json
// Request
POST /api/v1/notifications/uuid-123/read

// Response
{ "status": "success" }
```

#### 2. 게시물 API

**GET /api/v1/posts/{post_id}**
```json
// Request
GET /api/v1/posts/post-uuid-123

// Response
{
  "id": "post-uuid-123",
  "site_id": "site-uuid-456",
  "title": "2025-1학기 수강신청 안내",
  "content": "2025학년도 1학기 수강신청 일정을 안내드립니다...",
  "source_url": "https://cs.inha.ac.kr/notice/123",
  "event_date": "2025-11-25T12:00:00",
  "location": "컴퓨터공학과 사무실",
  "created_at": "2025-01-19T14:00:00",
  "updated_at": "2025-01-19T14:00:00"
}
```

**GET /api/v1/posts/list**
```json
// Request
GET /api/v1/posts/list?page=1&page_size=20&site_id=site-uuid

// Response
{
  "total": 50,
  "page": 1,
  "page_size": 20,
  "items": [ /* Post 배열 */ ]
}
```

---

## FCM 연동

### 1. 백엔드 → 앱 푸시 알림

#### FCM 메시지 포맷

**새 게시물 알림**:
```json
{
  "data": {
    "type": "new_post",
    "post_id": "post-uuid-123",
    "site_id": "site-uuid-456",
    "site_name": "컴퓨터공학과 공지사항",
    "post_title": "2025-1학기 수강신청 안내"
  },
  "notification": {
    "title": "새 게시물 알림",
    "body": "컴퓨터공학과에 새로운 공지사항이 등록되었습니다"
  }
}
```

**사이트 등록 완료 알림**:
```json
{
  "data": {
    "type": "site_registered",
    "site_id": "site-uuid-456",
    "site_name": "컴퓨터공학과 공지사항"
  },
  "notification": {
    "title": "사이트 등록 완료",
    "body": "컴퓨터공학과 공지사항 크롤링이 완료되었습니다"
  }
}
```

### 2. MyFirebaseMessagingService.java

```java
@Override
public void onMessageReceived(RemoteMessage remoteMessage) {
    Map<String, String> data = remoteMessage.getData();
    String type = data.get("type");

    switch (type) {
        case "new_post":
            // 게시물 상세 페이지로 이동하는 알림 생성
            Intent intent = new Intent(this, PostDetailActivity.class);
            intent.putExtra("POST_ID", data.get("post_id"));
            break;

        case "site_registered":
            // 메인 또는 사이트 상세 페이지로 이동
            break;

        case "event_reminder":
        case "deadline":
            // 일정 알림
            break;
    }

    showNotification(title, message, data);
}
```

### 3. 알림 권한 (Android 13+)

**AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**런타임 권한 요청** (필요시):
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityCompat.requestPermissions(this,
        new String[]{Manifest.permission.POST_NOTIFICATIONS},
        REQUEST_NOTIFICATION_PERMISSION);
}
```

---

## 캘린더 연동

### 1. CalendarManager.java

#### 주요 메서드

**일정 추가**:
```java
public long addEventToCalendar(
    String postId,      // 게시물 ID
    String title,       // 일정 제목
    String eventDateStr,// 일정 날짜
    String location     // 장소 (nullable)
) {
    // 1. 캘린더 권한 확인
    if (!hasCalendarPermission()) return -1;

    // 2. 날짜 파싱
    Date eventDate = parseDate(eventDateStr);

    // 3. 일정 생성
    ContentValues values = new ContentValues();
    values.put(CalendarContract.Events.TITLE, title);
    values.put(CalendarContract.Events.DTSTART, eventDate.getTime());
    values.put(CalendarContract.Events.DTEND, eventDate.getTime() + 1시간);
    values.put(CalendarContract.Events.EVENT_LOCATION, location);

    Uri eventUri = contentResolver.insert(
        CalendarContract.Events.CONTENT_URI,
        values
    );

    // 4. 알림 추가
    addReminder(eventId, 1440); // 1일 전
    addReminder(eventId, 60);   // 1시간 전

    // 5. SharedPreferences에 저장
    saveEventId(postId, eventId);

    return eventId;
}
```

**일정 삭제**:
```java
public boolean removeEventFromCalendar(String postId) {
    long eventId = getEventId(postId);

    Uri deleteUri = ContentUris.withAppendedId(
        CalendarContract.Events.CONTENT_URI,
        eventId
    );

    int rows = contentResolver.delete(deleteUri, null, null);

    if (rows > 0) {
        removeEventId(postId);
        return true;
    }
    return false;
}
```

**등록 여부 확인**:
```java
public boolean isEventRegistered(String postId) {
    SharedPreferences prefs = context.getSharedPreferences(
        AppConfig.PREF_CALENDAR,
        Context.MODE_PRIVATE
    );
    long eventId = prefs.getLong("event_" + postId, -1);
    return eventId != -1;
}
```

### 2. 권한 설정

**AndroidManifest.xml**:
```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

**런타임 권한 요청**:
```java
private static final int REQUEST_CALENDAR_PERMISSION = 1001;

private void requestCalendarPermission() {
    ActivityCompat.requestPermissions(this,
        new String[]{
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        },
        REQUEST_CALENDAR_PERMISSION
    );
}
```

### 3. 동기화

**목록과 상세 페이지 동기화**:
- 목록의 토글 ON → 상세 페이지의 버튼 "일정 삭제하기"로 변경
- 상세 페이지에서 일정 추가 → 목록의 토글 ON으로 변경
- SharedPreferences를 통해 상태 공유

---

## 사용 방법

### 1. 알림 페이지

**진입 방법**:
- 하단 네비게이션의 "알림" 탭 클릭
- FCM 푸시 알림 클릭

**기능**:
1. 탭 선택으로 알림 필터링
2. 알림 클릭 → 해당 게시물/사이트로 이동
3. 🗑 아이콘 클릭 → 개별 알림 삭제
4. "읽은 알림 모두 삭제" → 읽은 알림 일괄 삭제

### 2. 게시물 목록

**진입 방법**:
- 사이트 카드 클릭
- 알림에서 게시물 클릭

**기능**:
1. 탭으로 필터링 (전체, 새 게시물, 저장됨)
2. NEW 배지: 24시간 이내 게시물
3. D-day 배지: 7일 이내 마감/일정
4. 캘린더 토글:
   - event_date가 있는 게시물만 활성화
   - ON: 캘린더에 일정 추가 + 알림 설정
   - OFF: 일정 삭제
5. 게시물 클릭 → 상세 페이지

### 3. 게시물 상세

**진입 방법**:
- 게시물 목록에서 아이템 클릭
- 알림에서 게시물 클릭

**기능**:
1. 전체 본문 내용 확인
2. "게시글 직접 보기" 버튼:
   - source_url이 있을 때만 표시
   - 외부 브라우저로 원문 열기
3. "일정 추가하기" 버튼:
   - event_date가 있을 때만 표시
   - 첫 클릭: 일정 추가
   - 두 번째 클릭: 일정 삭제 ("일정 삭제하기"로 변경)

### 4. FCM 푸시 알림

**수신 시나리오**:
1. 사이트 등록 → 백엔드 Celery가 크롤링 완료 → FCM 알림
2. 새 게시물 감지 → FCM 알림
3. 일정/마감 임박 → FCM 알림

**알림 클릭**:
- 자동으로 해당 컨텐츠 페이지로 이동
- 알림 DB에도 저장되어 알림 페이지에서 확인 가능

---

## 추가 작업 필요 사항

### 1. 백엔드 연동
- [ ] 실제 사용자 ID 세션/SharedPreferences에서 가져오기
- [ ] FCM 토큰 등록 API 구현 (`/api/v1/fcm/register`)
- [ ] 알림 DB 저장 로직 구현 (FCM 수신 시)

### 2. Site 정보 연동
- [ ] 카테고리 태그에 실제 site 정보 표시
- [ ] Site API에서 이름, 카테고리 가져오기

### 3. 날짜 형식
- [ ] 백엔드 응답 날짜 형식 확인
- [ ] SimpleDateFormat 패턴 조정

### 4. UI/UX 개선
- [ ] 빈 화면 처리 (알림/게시물 없을 때)
- [ ] 로딩 스피너 추가
- [ ] 에러 메시지 개선
- [ ] 스와이프 새로고침

### 5. 성능 최적화
- [ ] 페이지네이션 구현
- [ ] 이미지 캐싱 (Glide)
- [ ] 네트워크 재시도 로직

### 6. 테스트
- [ ] FCM 푸시 알림 테스트
- [ ] 캘린더 연동 테스트
- [ ] 다양한 디바이스 테스트

---

## 참고 문서

1. **mobile_app_fcm_guide.md**: FCM 통합 가이드
2. **BASE_URL_GUIDE.md**: Base URL 설정 가이드
3. **docs/FCM_구현_완료_보고서.md**: FCM 구현 세부 사항
4. **Android Developer**:
   - [Calendar Provider](https://developer.android.com/guide/topics/providers/calendar-provider)
   - [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging/android/client)
   - [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview)

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|-----------|
| 2025-01-19 | 1.0 | 초기 문서 작성 |

---

**작성자**: Claude Code
**마지막 업데이트**: 2025-01-19
