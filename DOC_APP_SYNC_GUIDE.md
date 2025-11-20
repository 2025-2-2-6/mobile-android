## 서버 연동 필수 요구사항 정리

### 1. Google 로그인 후 UID 동기화
- `/api/v1/auth/google-login` 호출을 완료해야만 서버 `users` 테이블에 Firebase UID가 upsert됩니다.
- **AddSiteActivity**:
  - `FirebaseAuth.getInstance().getCurrentUser()`에서 UID를 가져와 `SiteRegisterRequest.user_id`에 넣습니다.
  - 로그인 정보가 없으면 사이트 등록을 중단하고 사용자에게 다시 로그인을 요청합니다.
- **Login 화면**:
  - 구글 로그인 성공 직후 `FirebaseMessaging.getInstance().getToken()`을 호출해 최신 토큰을 가져옵니다.
  - 토큰 획득에 실패해도 `FcmTokenManager.registerTokenIfPossible()`로 캐시된 토큰+UID 조합을 재시도합니다.

### 2. FCM 토큰 등록 정책
- FCM 토큰 등록/재등록도 **같은 Firebase UID**를 사용해야 서버가 내부 UUID로 매핑할 수 있습니다.
- 앱 구조:
  - `MyFirebaseMessagingService.onNewToken` → `FcmTokenManager.handleNewToken(context, token)`
  - `FcmTokenManager`는 토큰을 `AppConfig.PREF_FCM`에 캐시하고, UID와 함께 `/api/v1/fcm/register`로 전송합니다.
  - 로그인 완료 및 앱 재시작 시점에서도 `FcmTokenManager.registerTokenIfPossible()`로 누락된 토큰을 보정합니다.
- 아직 로그아웃 시 `/fcm/unregister` 호출은 구현되지 않았으므로 추후 확장 시 `FcmTokenManager`에 제거 API를 연결하세요.

### 3. 새 `event_date` 필드 활용
- 서버가 게시물 `event_date`를 채워주기 시작했습니다.
- `PostAdapter`와 `PostDetailActivity`는 이미 ISO 문자열을 파싱하여 한국 시간(KST)으로 표시하므로 별도 수정 없이 바로 사용 가능합니다.

### 4. 정리
| 항목 | 적용 클래스 | 비고 |
| --- | --- | --- |
| 사이트 등록 UID | `AddSiteActivity` | 로그인 필수, UID 없으면 중단 |
| FCM 토큰 등록 | `FcmTokenManager`, `MyFirebaseMessagingService`, `Login` | UID + 토큰 묶음 유지 |
| 이벤트 일정 표시 | `PostAdapter`, `PostDetailActivity` | `DateTimeUtils`로 KST 변환 |
