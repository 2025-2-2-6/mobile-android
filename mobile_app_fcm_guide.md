# Mobile App ↔ Backend FCM 통합 가이드

모바일 개발팀이 우리 FastAPI 백엔드와 FCM 토큰/비콘 통신을 안정적으로 연결하기 위한 흐름, 엔드포인트, 로그 및 점검 항목을 한 장에 정리했습니다.

## 1. 서비스 접점

- **Base URL**: `https://<backend-host>/api/v1`
- **인증**: Google ID Token → `Authorization: Bearer <token>` 헤더 (https://www.googleapis.com/ OAuth2 인증)
- **요청 바디**: `application/json`
- **FCM 토큰 관련 API**: `/fcm/register`, `/fcm/unregister` (FastAPI의 `app/api/v1/endpoints/fcm.py`가 처리)
- **스키마 폴더명**: `app/schemas`는 Pydantic 모델을 모아둔 고정 위치로, rename하지 말고 그대로 import합니다.

## 2. 로그인 & 유저 세션

1. Firebase/Google 로그인으로 ID Token 확보.
2. `POST /api/v1/auth/google-login` 호출 시 `Authorization: Bearer <token>` 헤더 포함.
3. 반환 JSON에는 내부 사용자 ID(`id`, `firebase_uid`, `email`, `name`)가 들어 있으므로 로컬 세션에 저장 후 이후 요청에 활용합니다.
4. 개발 모드에서는 `POST /api/v1/auth/dev-upsert`에 `X-Debug-Uid` 또는 `uid`를 보내서 테스트 유저를 즉시 만들 수 있지만, 운영에서는 403이므로 주의.

## 3. FCM 토큰 등록/해제

### 3.1 등록

```http
POST /api/v1/fcm/register
Content-Type: application/json

{
  "user_id": "<firebase_uid>",    # 반드시 백엔드에 존재하는 사용자 ID (JWT/AWS 로그인 이후)
  "fcm_token": "<device token>", # Firebase가 발급한 최신 FCM 토큰
  "platform": "android",         # 또는 "ios"
  "device_info": "Pixel 8 Pro / Android 15"
}
```

- 같은 토큰이 이미 존재하면 `last_used`와 `updated_at`만 갱신, 그렇지 않으면 새 행을 추가합니다.
- 실패 시 404 또는 500을 응답하므로 token이 없으면 `ResourceNotFoundError` 로그(`app/core/exceptions.py`) 확인.

### 3.2 해제

```http
DELETE /api/v1/fcm/unregister?fcm_token=<device token>
```

- 토큰이 등록되지 않았으면 404, 요청이 성공하면 `{status:"success"}`. 앱 종료/로그아웃시 호출.

## 4. FCM 메시지 흐름 요약

1. 백엔드 크롤링 작업(`app/tasks/crawling_tasks.py`)이 새 게시글을 감지하면, `send_fcm_message` 혹은 `send_fcm_multicast`를 통해 토큰에게 푸시.
2. payload 예시(새 게시글):

```json
{
  "data": {
    "type": "new_post",
    "site_id": "uuid",
    "site_name": "인하대학교 공지사항",
    "post_id": "uuid",
    "post_title": "2025년 1학기 장학..."
  }
}
```

3. 알림 표시/처리 로직은 `type`에 따라 분기. 새 게시글/공지/일정에 대해 적절히 UI 연결.

4. 푸시 수신 확인은 Android `Logcat`의 `FCMService` 또는 `FcmTokenManager` 로그(`docs/FCM_구현_완료_보고서.md:466-500`)에서 확인 가능.

## 5. 점검 & 테스트 체크리스트

| 항목 | 설명 |
| --- | --- |
| Firebase 프로젝트 | `google-services.json`이 `app/`에 존재하고, Firebase Console에서 Cloud Messaging 키 확인 |
| 앱 권한 | Android 13 이상은 `POST_NOTIFICATIONS` 요청 코드 추가 (`MainActivity` 등) |
| 서버 URL | 로컬 테스트: `http://10.0.2.2:8000`, 실제 디바이스/QA 환경: 운영 호스트 IP/도메인 |
| 토큰 로그 | `FcmTokenManager` 로그에 `FCM 토큰:` 찍히는지 확인 |
| 콘솔 테스트 | Firebase Console → Cloud Messaging → Custom data로 `type:new_post` 등 넣어서 테스트 |

## 6. 코드 참고 포인트

- **FCM 토큰 초기화**: `Login.java`에서 로그인 후 `FcmTokenManager.initializeFcmToken(this)` 호출 (이미 구현된 앱 전제).  
- **토큰 매니저**: `FcmTokenManager.java` 내 `sendTokenToServer`가 위 API를 호출 (`docs/FCM_구현_완료_보고서.md:267-362`)  
- **서비스 등록**: `AndroidManifest` 내 `MyFirebaseMessagingService` 등록, `POST_NOTIFICATIONS` 권한 필수 (`docs/FCM_구현_완료_보고서.md:32-91`)

## 7. 운영 가이드

1. 새로운 토큰이 나올 때마다 반드시 `/fcm/register` 호출 (onNewToken 호출 시).  
2. 로그아웃 시 `deleteToken()` 호출 → `/fcm/unregister`.
3. Firebase Console에서 test message 보내기 전 `Custom data`로 `type`/`site_id`/`post_title` 등 전달해 앱 수신 로직 확인.
4. 서버 에러(500, 404)는 FastAPI 로그 혹은 `celery beat` 로그에서 확인하며, 알림 실패 시 `send_fcm_message`에서 예외 내용 로깅.

## 8. 기타 전달 사항

- schemas 폴더명은 `app/schemas` 그대로 유지하고, FastAPI 코드에서 `from app.schemas.site import ...`처럼 참조함.  
- 필요 시 추가적인 엔드포인트 (`/sites`, `/posts`)도 해당 문서(`docs/mobile_app_integration.md`) 참조.  
- 이 가이드는 앱 배포 시 테스트용 로컬 URL → QA → 운영 URL 순으로 바꾸고, Firebase 콘솔에서 푸시를 확인하는 흐름을 담고 있습니다. 필요하면 `docs/FCM_구현_완료_보고서.md`도 함께 전달하세요.
