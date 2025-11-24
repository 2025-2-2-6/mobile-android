# 백엔드 API 명세서 (안드로이드 앱 연동)

## 📋 목차
1. [캘린더 일정 API](#캘린더-일정-api)
2. [사이트 관리 API](#사이트-관리-api)
3. [게시물 API](#게시물-api)
4. [알림 설정 API](#알림-설정-api)
5. [통계 API](#통계-api)

---

## 🗓️ 캘린더 일정 API

### 1. 일정 생성
```
POST /api/calendar-events
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "user_id": "string",
  "title": "정부 지원금 신청 마감",
  "category": "창업",
  "event_date": "2024-01-15",
  "event_time": "15:00",
  "memo": "서류 준비: 사업자등록증, 사업계획서",
  "alarm_enabled": true,
  "alarm_time": "1일 전"
}
```

**Response (201 Created)**:
```json
{
  "id": 123,
  "user_id": "string",
  "title": "정부 지원금 신청 마감",
  "category": "창업",
  "event_date": "2024-01-15",
  "event_time": "15:00",
  "memo": "서류 준비: 사업자등록증, 사업계획서",
  "alarm_enabled": true,
  "alarm_time": "1일 전",
  "created_at": "2024-01-01T10:00:00Z",
  "updated_at": "2024-01-01T10:00:00Z"
}
```

### 2. 일정 목록 조회
```
GET /api/calendar-events?user_id={userId}&start_date={startDate}&end_date={endDate}
Authorization: Bearer {token}
```

**Query Parameters**:
- `user_id` (required): 사용자 ID
- `start_date` (optional): 조회 시작 날짜 (YYYY-MM-DD)
- `end_date` (optional): 조회 종료 날짜 (YYYY-MM-DD)

**Response (200 OK)**:
```json
{
  "events": [
    {
      "id": 123,
      "user_id": "string",
      "title": "정부 지원금 신청 마감",
      "category": "창업",
      "event_date": "2024-01-15",
      "event_time": "15:00",
      "memo": "서류 준비: 사업자등록증, 사업계획서",
      "alarm_enabled": true,
      "alarm_time": "1일 전",
      "created_at": "2024-01-01T10:00:00Z",
      "updated_at": "2024-01-01T10:00:00Z"
    }
  ],
  "total": 1
}
```

### 3. 일정 수정
```
PUT /api/calendar-events/{id}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body** (수정할 필드만 포함):
```json
{
  "title": "정부 지원금 신청 마감 (수정됨)",
  "event_date": "2024-01-16",
  "alarm_enabled": false
}
```

**Response (200 OK)**:
```json
{
  "id": 123,
  "user_id": "string",
  "title": "정부 지원금 신청 마감 (수정됨)",
  "category": "창업",
  "event_date": "2024-01-16",
  "event_time": "15:00",
  "memo": "서류 준비: 사업자등록증, 사업계획서",
  "alarm_enabled": false,
  "alarm_time": null,
  "created_at": "2024-01-01T10:00:00Z",
  "updated_at": "2024-01-02T10:00:00Z"
}
```

### 4. 일정 삭제
```
DELETE /api/calendar-events/{id}
Authorization: Bearer {token}
```

**Response (204 No Content)**:
```
(빈 응답)
```

### 📌 CalendarEvent 필드 상세

| 필드명 | 타입 | 필수 | 설명 | 예시 |
|--------|------|------|------|------|
| `id` | integer | - | 자동 생성 | 123 |
| `user_id` | string | ✅ | 사용자 ID | "user123" |
| `title` | string | ✅ | 일정 제목 | "정부 지원금 신청" |
| `category` | string | ✅ | 카테고리 | "창업", "공모전", "교육", "기타" |
| `event_date` | string | ✅ | 일정 날짜 (YYYY-MM-DD) | "2024-01-15" |
| `event_time` | string | ❌ | 일정 시간 (HH:mm) | "15:00" |
| `memo` | string | ❌ | 메모 | "서류 준비..." |
| `alarm_enabled` | boolean | ✅ | 알림 활성화 | true |
| `alarm_time` | string | ❌ | 알림 시간 옵션 | "일정 시작시간", "10분 전", "1시간 전", "1일 전", "3일 전", "일주일 전" |
| `created_at` | string | - | 생성 시간 (ISO 8601) | "2024-01-01T10:00:00Z" |
| `updated_at` | string | - | 수정 시간 (ISO 8601) | "2024-01-01T10:00:00Z" |

---

## 🌐 사이트 관리 API

### 1. 사이트 상세 조회
```
GET /api/sites/{siteId}
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "id": "site123",
  "name": "창업진흥원",
  "url": "https://k-startup.go.kr",
  "category": "창업",
  "last_crawl_date": "2024-01-15",
  "crawl_status": "success",
  "total_posts_count": 23,
  "new_posts_count": 12,
  "is_crawling": false,
  "favicon_url": "https://k-startup.go.kr/favicon.ico",
  "created_at": "2024-01-01T10:00:00Z"
}
```

### 2. 사이트 수정
```
PUT /api/sites/{siteId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "창업진흥원 (수정됨)",
  "category": "공모전",
  "url": "https://new-url.go.kr"
}
```

**Response (200 OK)**:
```json
{
  "id": "site123",
  "name": "창업진흥원 (수정됨)",
  "url": "https://new-url.go.kr",
  "category": "공모전",
  "last_crawl_date": "2024-01-15",
  "updated_at": "2024-01-16T10:00:00Z"
}
```

### 3. 사이트 최근 게시물 조회 (2개)
```
GET /api/sites/{siteId}/posts?limit=2&sort=created_at&order=desc
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "posts": [
    {
      "id": "post123",
      "site_id": "site123",
      "title": "2024년 AI 기술 트렌드 분석",
      "author": "이준호",
      "created_at": "2024-01-15",
      "deadline": "2024-01-20",
      "is_new": true,
      "url": "https://k-startup.go.kr/posts/123"
    },
    {
      "id": "post122",
      "site_id": "site123",
      "title": "창업 지원금 신청 안내",
      "author": "관리자",
      "created_at": "2024-01-14",
      "deadline": null,
      "is_new": false,
      "url": "https://k-startup.go.kr/posts/122"
    }
  ]
}
```

### 📌 Site 필드 상세

| 필드명 | 타입 | 필수 | 설명 | 예시 |
|--------|------|------|------|------|
| `id` | string | - | 사이트 ID | "site123" |
| `name` | string | ✅ | 사이트 이름 | "창업진흥원" |
| `url` | string | ✅ | 사이트 URL | "https://k-startup.go.kr" |
| `category` | string | ✅ | 카테고리 | "창업", "공모전", "교육" 등 |
| `last_crawl_date` | string | - | 마지막 수집 날짜 | "2024-01-15" |
| `crawl_status` | string | - | 수집 상태 | "success", "failed", "in_progress" |
| `total_posts_count` | integer | - | 전체 게시물 수 | 23 |
| `new_posts_count` | integer | - | 신규 게시물 수 | 12 |
| `is_crawling` | boolean | - | 현재 수집 중 여부 | false |
| `favicon_url` | string | ❌ | 파비콘 URL | "https://..." |

---

## 📝 게시물 API

### 1. 게시물 목록 조회 (필터링)
```
GET /api/posts?site_id={siteId}&status={status}&page=1&limit=20
Authorization: Bearer {token}
```

**Query Parameters**:
- `site_id` (optional): 사이트 ID로 필터링
- `status` (optional): 상태 필터
  - `all`: 전체
  - `recruiting`: 모집중 (마감일이 현재보다 미래)
  - `new`: 최근 게시물 (is_new = true)
  - `priority`: 우선순위 높음
- `page` (optional): 페이지 번호 (default: 1)
- `limit` (optional): 페이지당 개수 (default: 20)
- `search` (optional): 검색어 (제목, 내용 검색)

**Response (200 OK)**:
```json
{
  "posts": [
    {
      "id": "post123",
      "site_id": "site123",
      "site_name": "창업진흥원",
      "title": "2024년 AI 기술 트렌드 분석",
      "content": "올해 가장 주목할 만한 AI 기술을...",
      "author": "이준호",
      "created_at": "2024-01-15",
      "event_start_date": "2024-01-20",
      "event_end_date": "2024-01-25",
      "deadline": "D-7",
      "is_new": true,
      "is_saved": false,
      "url": "https://k-startup.go.kr/posts/123"
    }
  ],
  "total": 45,
  "page": 1,
  "limit": 20
}
```

---

## 🔔 알림 설정 API

### 현재 알림 설정 조회
```
GET /api/users/{userId}/notification-settings
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "device_notification_enabled": true,
  "new_post_notification": true,
  "calendar_notification": true
}
```

### 알림 설정 업데이트
```
PUT /api/users/{userId}/notification-settings
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body**:
```json
{
  "new_post_notification": false,
  "calendar_notification": true
}
```

**Response (200 OK)**:
```json
{
  "device_notification_enabled": true,
  "new_post_notification": false,
  "calendar_notification": true,
  "updated_at": "2024-01-16T10:00:00Z"
}
```

---

## 📊 통계 API

### 사용자 활동 통계 조회
```
GET /api/users/{userId}/statistics
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "registered_sites_count": 5,
  "new_posts_count": 12,
  "saved_events_count": 8,
  "total_notifications_count": 25,
  "unread_notifications_count": 3
}
```

**설명**:
- `registered_sites_count`: 등록된 사이트 수 (홈화면과 MyPage 동기화)
- `new_posts_count`: 새 게시물 수 (is_new = true)
- `saved_events_count`: 저장된 캘린더 일정 수
- `total_notifications_count`: 전체 알림 수
- `unread_notifications_count`: 읽지 않은 알림 수

---

## 🔄 데이터 동기화 흐름

### 앱 → 서버 (업로드)
```
1. 사용자가 일정 추가
2. Room DB에 먼저 저장 (오프라인 지원)
3. 백그라운드에서 POST /api/calendar-events 호출
4. 성공 시: 서버에서 받은 id로 Room DB 업데이트
5. 실패 시: 재시도 큐에 추가 (나중에 재시도)
```

### 서버 → 앱 (다운로드)
```
1. 앱 실행 시 GET /api/calendar-events 호출
2. 서버에서 받은 데이터를 Room DB에 저장 (upsert)
3. LiveData가 자동으로 UI 업데이트
```

---

## 🚨 에러 응답

### 공통 에러 형식
```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "일정 제목은 필수입니다",
    "details": {
      "field": "title",
      "reason": "required"
    }
  }
}
```

### 에러 코드

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| 400 | INVALID_REQUEST | 잘못된 요청 (필수 필드 누락 등) |
| 401 | UNAUTHORIZED | 인증 실패 (토큰 만료/없음) |
| 403 | FORBIDDEN | 권한 없음 (다른 사용자의 데이터 접근) |
| 404 | NOT_FOUND | 리소스 없음 (존재하지 않는 일정/사이트) |
| 409 | CONFLICT | 중복 (이미 존재하는 리소스) |
| 500 | INTERNAL_ERROR | 서버 내부 오류 |

---

## 📝 구현 우선순위

### 🔴 높음 (즉시 필요)
1. ✅ **캘린더 일정 API** (POST, GET, PUT, DELETE)
   - 앱에서 완전히 구현됨, API만 연동하면 바로 동작
2. ✅ **사이트 상세 조회 API** (GET /api/sites/{id})
   - SiteDetailActivity에서 필요
3. ✅ **통계 API** (GET /api/users/{userId}/statistics)
   - MyPage 동기화에 필요

### 🟡 중간 (1-2주 내)
4. **사이트 수정 API** (PUT /api/sites/{id})
5. **게시물 필터링 API** (status 파라미터 지원)

### 🟢 낮음 (추후)
6. 알림 설정 API (현재 SharedPreferences로 로컬 관리)

---

## 🔐 인증 헤더

모든 API 요청에는 Bearer 토큰이 필요합니다:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**토큰 관리**:
- 앱에서 `TokenManager.getBearerToken(context)` 사용
- SharedPreferences에 저장됨
- 만료 시 자동으로 재로그인 유도

---

## 📞 문의

API 구현 중 불명확한 부분이나 추가 필드가 필요한 경우 안드로이드 팀에 문의 바랍니다.

**작성일**: 2025-11-24
**작성자**: Claude Code Assistant (Android Team)
