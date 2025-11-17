# 백엔드 FCM 구현 가이드

## 📋 개요

안드로이드 앱에서 FCM 기능이 구현되었으므로, 백엔드 서버에서 다음 작업을 수행해야 합니다:

1. ✅ FCM 토큰 등록 API 구현
2. ✅ 크롤링 완료 시 푸시 발송
3. ✅ 새 게시글 감지 시 푸시 발송
4. ✅ 일정 알림 스케줄러 및 푸시 발송

---

## 🔧 필수 환경 설정

### 1. Firebase Admin SDK 설치

```bash
pip install firebase-admin
```

### 2. Firebase 서비스 계정 키 발급

1. Firebase Console 접속
2. 프로젝트 설정 → 서비스 계정
3. "새 비공개 키 생성" 클릭
4. JSON 파일 다운로드
5. 서버에 저장 (예: `config/firebase-service-account.json`)

### 3. Firebase Admin SDK 초기화

**파일**: `app/firebase_config.py` (새로 생성)

```python
import firebase_admin
from firebase_admin import credentials, messaging
import os

# Firebase Admin SDK 초기화 (앱 시작 시 한 번만 실행)
cred = credentials.Certificate("config/firebase-service-account.json")
firebase_admin.initialize_app(cred)

def send_fcm_message(fcm_token: str, data: dict) -> bool:
    """
    FCM 푸시 메시지 발송

    Args:
        fcm_token: 수신자의 FCM 토큰
        data: 전송할 데이터 (딕셔너리)

    Returns:
        bool: 발송 성공 여부
    """
    try:
        # 데이터 페이로드의 모든 값을 문자열로 변환
        string_data = {k: str(v) for k, v in data.items()}

        message = messaging.Message(
            data=string_data,  # 데이터 페이로드 (앱에서 처리)
            token=fcm_token,
            android=messaging.AndroidConfig(
                priority='high',  # 높은 우선순위 (즉시 전달)
            ),
        )

        response = messaging.send(message)
        print(f"[FCM] 발송 성공: {response}")
        return True

    except messaging.UnregisteredError:
        print(f"[FCM] 유효하지 않은 토큰: {fcm_token}")
        # TODO: DB에서 해당 토큰 삭제
        return False

    except Exception as e:
        print(f"[FCM] 발송 실패: {e}")
        return False


def send_fcm_to_multiple(fcm_tokens: list[str], data: dict) -> dict:
    """
    여러 사용자에게 FCM 푸시 메시지 발송

    Args:
        fcm_tokens: 수신자들의 FCM 토큰 리스트
        data: 전송할 데이터

    Returns:
        dict: {"success": 성공 수, "failure": 실패 수}
    """
    success_count = 0
    failure_count = 0

    for token in fcm_tokens:
        if send_fcm_message(token, data):
            success_count += 1
        else:
            failure_count += 1

    return {
        "success": success_count,
        "failure": failure_count
    }
```

---

## 📡 API 구현

### 1. FCM 토큰 등록 API

**엔드포인트**: `POST /api/v1/fcm/register`

**파일**: `app/routers/fcm.py` (새로 생성)

```python
from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel
from datetime import datetime
from app.database import get_db
from sqlalchemy.orm import Session
import uuid

router = APIRouter()


class FcmTokenRequest(BaseModel):
    user_id: str          # Firebase UID
    fcm_token: str        # FCM 토큰
    platform: str         # "android" 또는 "ios"
    device_info: str      # 디바이스 모델명


@router.post("/api/v1/fcm/register")
async def register_fcm_token(
    request: FcmTokenRequest,
    db: Session = Depends(get_db)
):
    """
    FCM 토큰 등록/갱신

    - 기존 토큰이 있으면 last_used 갱신
    - 없으면 새로 등록
    """
    try:
        # 1. 사용자 확인 (users 테이블에서 firebase_uid로 조회)
        user = db.execute(
            "SELECT id FROM users WHERE firebase_uid = :uid",
            {"uid": request.user_id}
        ).fetchone()

        if not user:
            raise HTTPException(status_code=404, detail="사용자를 찾을 수 없습니다")

        user_id = user[0]

        # 2. 기존 토큰 확인
        existing_token = db.execute(
            """
            SELECT id FROM fcm_tokens
            WHERE user_id = :user_id AND fcm_token = :token
            """,
            {"user_id": user_id, "token": request.fcm_token}
        ).fetchone()

        now = datetime.utcnow()

        if existing_token:
            # 기존 토큰 갱신
            db.execute(
                """
                UPDATE fcm_tokens
                SET last_used = :now, updated_at = :now
                WHERE id = :id
                """,
                {"now": now, "id": existing_token[0]}
            )
            print(f"[FCM] 토큰 갱신: user_id={user_id}")
        else:
            # 새 토큰 등록
            db.execute(
                """
                INSERT INTO fcm_tokens (id, user_id, fcm_token, platform, device_info, last_used, created_at, updated_at)
                VALUES (:id, :user_id, :token, :platform, :device_info, :now, :now, :now)
                """,
                {
                    "id": str(uuid.uuid4()),
                    "user_id": user_id,
                    "token": request.fcm_token,
                    "platform": request.platform,
                    "device_info": request.device_info,
                    "now": now
                }
            )
            print(f"[FCM] 새 토큰 등록: user_id={user_id}")

        db.commit()

        return {
            "status": "success",
            "message": "FCM token registered successfully"
        }

    except HTTPException:
        raise
    except Exception as e:
        db.rollback()
        print(f"[FCM] 토큰 등록 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))
```

**FastAPI 메인 앱에 라우터 등록**:

```python
# main.py
from app.routers import fcm

app.include_router(fcm.router)
```

---

## 🔔 푸시 알림 발송 구현

### 2. 크롤링 완료 시 푸시 발송

**파일**: `app/services/crawler.py` (기존 파일 수정)

```python
from app.firebase_config import send_fcm_to_multiple
from app.database import get_db

def send_crawling_complete_notification(site_id: str, site_name: str):
    """
    크롤링 완료 시 구독자들에게 푸시 발송

    Args:
        site_id: 크롤링된 사이트 ID
        site_name: 사이트 이름
    """
    db = next(get_db())

    try:
        # 1. 해당 사이트를 구독한 사용자들의 FCM 토큰 조회
        result = db.execute(
            """
            SELECT DISTINCT f.fcm_token
            FROM fcm_tokens f
            JOIN users u ON f.user_id = u.id
            JOIN user_sites us ON u.id = us.user_id
            WHERE us.site_id = :site_id
              AND us.is_subscribed = true
            """,
            {"site_id": site_id}
        ).fetchall()

        fcm_tokens = [row[0] for row in result]

        if not fcm_tokens:
            print(f"[FCM] 크롤링 완료 알림: 구독자 없음 (site_id={site_id})")
            return

        # 2. FCM 메시지 데이터 구성
        data = {
            "type": "crawling_complete",
            "site_id": site_id,
            "site_name": site_name
        }

        # 3. 푸시 발송
        result = send_fcm_to_multiple(fcm_tokens, data)
        print(f"[FCM] 크롤링 완료 알림 발송: {result['success']}명 성공, {result['failure']}명 실패")

    except Exception as e:
        print(f"[FCM] 크롤링 완료 알림 발송 실패: {e}")
    finally:
        db.close()


# 크롤링 함수에서 호출 예시
def crawl_site(site_id: str):
    # ... 크롤링 로직 ...

    # 크롤링 완료 후
    site_name = get_site_name(site_id)  # 사이트 이름 조회
    send_crawling_complete_notification(site_id, site_name)
```

---

### 3. 새 게시글 감지 시 푸시 발송

**파일**: `app/services/post_detector.py` (기존 파일 수정)

```python
from app.firebase_config import send_fcm_to_multiple
from app.database import get_db

def send_new_post_notification(site_id: str, site_name: str, post_id: str, post_title: str):
    """
    새 게시글 감지 시 구독자들에게 푸시 발송

    Args:
        site_id: 사이트 ID
        site_name: 사이트 이름
        post_id: 새 게시글 ID
        post_title: 게시글 제목
    """
    db = next(get_db())

    try:
        # 1. 해당 사이트를 구독한 사용자들의 FCM 토큰 조회
        result = db.execute(
            """
            SELECT DISTINCT f.fcm_token
            FROM fcm_tokens f
            JOIN users u ON f.user_id = u.id
            JOIN user_sites us ON u.id = us.user_id
            WHERE us.site_id = :site_id
              AND us.is_subscribed = true
            """,
            {"site_id": site_id}
        ).fetchall()

        fcm_tokens = [row[0] for row in result]

        if not fcm_tokens:
            print(f"[FCM] 새 게시글 알림: 구독자 없음 (site_id={site_id})")
            return

        # 2. FCM 메시지 데이터 구성
        data = {
            "type": "new_post",
            "site_id": site_id,
            "site_name": site_name,
            "post_id": post_id,
            "post_title": post_title
        }

        # 3. 푸시 발송
        result = send_fcm_to_multiple(fcm_tokens, data)
        print(f"[FCM] 새 게시글 알림 발송: {result['success']}명 성공, {result['failure']}명 실패")

    except Exception as e:
        print(f"[FCM] 새 게시글 알림 발송 실패: {e}")
    finally:
        db.close()


# SimHash 비교 후 새 게시글 추가 시 호출 예시
def add_new_post(site_id: str, post_data: dict):
    db = next(get_db())

    try:
        # posts 테이블에 게시글 추가
        post_id = str(uuid.uuid4())
        db.execute(
            """
            INSERT INTO posts (id, site_id, title, content, source_url, created_at, updated_at)
            VALUES (:id, :site_id, :title, :content, :url, :now, :now)
            """,
            {
                "id": post_id,
                "site_id": site_id,
                "title": post_data["title"],
                "content": post_data["content"],
                "url": post_data.get("source_url"),
                "now": datetime.utcnow()
            }
        )
        db.commit()

        # 푸시 알림 발송
        site_name = get_site_name(site_id)
        send_new_post_notification(site_id, site_name, post_id, post_data["title"])

    except Exception as e:
        db.rollback()
        print(f"[ERROR] 게시글 추가 실패: {e}")
    finally:
        db.close()
```

---

### 4. 일정 알림 스케줄러 및 푸시 발송

**파일**: `app/services/scheduler.py` (새로 생성)

```python
from apscheduler.schedulers.background import BackgroundScheduler
from datetime import datetime, timedelta
from app.firebase_config import send_fcm_message
from app.database import get_db

def check_and_send_schedule_reminders():
    """
    매 분마다 실행하여 알림 시각이 도달한 일정을 찾아 푸시 발송
    """
    db = next(get_db())

    try:
        # 현재 시각 기준으로 알림 시각이 도달한 일정 조회
        now = datetime.utcnow()
        five_minutes_ago = now - timedelta(minutes=5)

        result = db.execute(
            """
            SELECT
                ce.id as event_id,
                ce.user_id,
                ce.title,
                ce.start_time,
                ce.notify_time,
                u.firebase_uid,
                f.fcm_token
            FROM calendar_events ce
            JOIN users u ON ce.user_id = u.id
            JOIN fcm_tokens f ON u.id = f.user_id
            WHERE ce.notify_enabled = true
              AND ce.notify_time <= :now
              AND ce.notify_time > :five_min_ago
            """,
            {"now": now, "five_min_ago": five_minutes_ago}
        ).fetchall()

        sent_count = 0

        for row in result:
            event_id = row[0]
            title = row[2]
            start_time = row[3]
            fcm_token = row[6]

            # FCM 메시지 데이터 구성
            data = {
                "type": "schedule_reminder",
                "event_id": event_id,
                "event_title": title,
                "event_time": start_time.strftime("%Y-%m-%d %H:%M")
            }

            # 푸시 발송
            if send_fcm_message(fcm_token, data):
                sent_count += 1

                # 알림 발송 완료 표시 (중복 발송 방지)
                db.execute(
                    """
                    UPDATE calendar_events
                    SET notify_enabled = false
                    WHERE id = :id
                    """,
                    {"id": event_id}
                )

        if sent_count > 0:
            db.commit()
            print(f"[FCM] 일정 알림 발송: {sent_count}건")

    except Exception as e:
        db.rollback()
        print(f"[FCM] 일정 알림 발송 실패: {e}")
    finally:
        db.close()


# 스케줄러 시작
scheduler = BackgroundScheduler()
scheduler.add_job(
    check_and_send_schedule_reminders,
    'interval',
    minutes=1,  # 매 1분마다 실행
    id='schedule_reminder_job'
)
scheduler.start()

print("[Scheduler] 일정 알림 스케줄러 시작됨 (1분 간격)")
```

**FastAPI 앱 시작 시 스케줄러 실행**:

```python
# main.py
from app.services.scheduler import scheduler

@app.on_event("startup")
def startup_event():
    print("[App] 서버 시작")
    # 스케줄러는 이미 scheduler.py에서 시작됨

@app.on_event("shutdown")
def shutdown_event():
    scheduler.shutdown()
    print("[Scheduler] 일정 알림 스케줄러 종료됨")
```

**필요한 패키지 설치**:

```bash
pip install apscheduler
```

---

## 📊 "새 게시글" 표시 기능 구현

### 5. `/posts/list` API에 `is_new` 필드 추가

**파일**: `app/routers/posts.py` (기존 파일 수정)

```python
from datetime import datetime, timedelta

@router.get("/api/v1/posts/list")
async def get_posts(
    page: int = 1,
    page_size: int = 20,
    q: str = None,
    site_id: str = None,
    since: str = None,
    until: str = None,
    order_by: str = "created_at",
    order: str = "desc",
    db: Session = Depends(get_db)
):
    """
    게시글 목록 조회 (is_new 필드 포함)

    is_new: 24시간 이내에 생성된 게시글은 true
    """
    try:
        # ... 기존 쿼리 로직 ...

        # 24시간 전 시각 계산
        twenty_four_hours_ago = datetime.utcnow() - timedelta(hours=24)

        # 쿼리에 is_new 필드 추가
        query = """
            SELECT
                p.id,
                p.site_id,
                p.title,
                p.content,
                p.source_url,
                p.event_date,
                p.location,
                p.created_at,
                p.updated_at,
                s.name as site_name,
                CASE
                    WHEN p.created_at >= :cutoff_time THEN true
                    ELSE false
                END as is_new
            FROM posts p
            JOIN sites s ON p.site_id = s.id
            WHERE 1=1
        """

        params = {"cutoff_time": twenty_four_hours_ago}

        # ... 필터링 조건 추가 (site_id, since, until 등) ...

        # ... ORDER BY, LIMIT, OFFSET 추가 ...

        result = db.execute(query, params).fetchall()

        posts = []
        for row in result:
            posts.append({
                "id": row[0],
                "site_id": row[1],
                "title": row[2],
                "content": row[3],
                "source_url": row[4],
                "event_date": row[5],
                "location": row[6],
                "created_at": row[7].isoformat() if row[7] else None,
                "updated_at": row[8].isoformat() if row[8] else None,
                "site_name": row[9],
                "is_new": row[10]  # 추가됨
            })

        return {
            "status": "success",
            "data": posts,
            "page": page,
            "page_size": page_size
        }

    except Exception as e:
        print(f"[ERROR] 게시글 목록 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))
```

---

## 🧪 테스트 방법

### 1. FCM 토큰 등록 API 테스트

```bash
curl -X POST http://localhost:8000/api/v1/fcm/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "firebase_uid_123",
    "fcm_token": "fAbCdEfG...",
    "platform": "android",
    "device_info": "Samsung SM-G991N"
  }'
```

**기대 응답**:
```json
{
  "status": "success",
  "message": "FCM token registered successfully"
}
```

### 2. 크롤링 완료 푸시 테스트

```python
# Python 콘솔에서 직접 테스트
from app.services.crawler import send_crawling_complete_notification

site_id = "your-site-uuid"
site_name = "서울대학교 공지사항"

send_crawling_complete_notification(site_id, site_name)
```

**앱에서 확인**: 푸시 알림 수신 여부 확인

### 3. 새 게시글 푸시 테스트

```python
from app.services.post_detector import send_new_post_notification

site_id = "your-site-uuid"
site_name = "서울대학교 공지사항"
post_id = "new-post-uuid"
post_title = "2025학년도 1학기 수강신청 안내"

send_new_post_notification(site_id, site_name, post_id, post_title)
```

### 4. 일정 알림 테스트

1. DB에 테스트 일정 추가:
```sql
INSERT INTO calendar_events (id, user_id, title, start_time, notify_enabled, notify_time, created_at, updated_at)
VALUES (
    UUID(),
    (SELECT id FROM users LIMIT 1),
    '테스트 일정',
    NOW() + INTERVAL 10 MINUTE,
    true,
    NOW() + INTERVAL 1 MINUTE,  -- 1분 후 알림
    NOW(),
    NOW()
);
```

2. 1분 후 앱에서 푸시 알림 수신 확인

---

## 📁 파일 구조

```
backend/
├── app/
│   ├── main.py                    # FastAPI 앱
│   ├── firebase_config.py         # 새로 생성 ⭐
│   ├── database.py
│   ├── routers/
│   │   ├── fcm.py                 # 새로 생성 ⭐
│   │   └── posts.py               # 수정 필요 ✏️
│   └── services/
│       ├── crawler.py             # 수정 필요 ✏️
│       ├── post_detector.py       # 수정 필요 ✏️
│       └── scheduler.py           # 새로 생성 ⭐
└── config/
    └── firebase-service-account.json  # Firebase 서비스 계정 키
```

---

## ⚠️ 주의사항

### 1. Firebase 서비스 계정 키 보안
- `firebase-service-account.json` 파일을 **절대 Git에 커밋하지 마세요**
- `.gitignore`에 추가:
  ```
  config/firebase-service-account.json
  ```

### 2. 토큰 만료 처리
- FCM 토큰이 유효하지 않으면 `UnregisteredError` 발생
- 에러 발생 시 해당 토큰을 DB에서 삭제하거나 비활성화 처리

### 3. 대량 발송 최적화
- Firebase Admin SDK의 `send_multicast()` 사용 고려 (최대 500개 토큰 동시 발송)
- 구독자가 많은 경우 배치 처리 권장

```python
def send_fcm_multicast(fcm_tokens: list[str], data: dict):
    """여러 사용자에게 한 번에 발송 (최대 500개)"""
    messages = []
    for token in fcm_tokens[:500]:  # 최대 500개
        messages.append(messaging.Message(
            data=data,
            token=token,
            android=messaging.AndroidConfig(priority='high')
        ))

    response = messaging.send_all(messages)
    return {
        "success": response.success_count,
        "failure": response.failure_count
    }
```

### 4. 일정 알림 중복 발송 방지
- 알림 발송 후 `notify_enabled = false`로 업데이트
- 또는 `last_notified_at` 컬럼 추가하여 중복 체크

---

## 📈 구현 체크리스트

### 필수 구현
- [ ] Firebase Admin SDK 설치 및 초기화
- [ ] `firebase_config.py` 생성
- [ ] `POST /api/v1/fcm/register` API 구현
- [ ] 크롤링 완료 시 푸시 발송 로직 추가
- [ ] 새 게시글 감지 시 푸시 발송 로직 추가
- [ ] 일정 알림 스케줄러 구현
- [ ] `/posts/list` API에 `is_new` 필드 추가

### 선택 구현
- [ ] 대량 발송 최적화 (`send_multicast`)
- [ ] 토큰 만료 자동 정리
- [ ] 푸시 발송 로그 DB 저장
- [ ] 사용자별 알림 설정 (ON/OFF)

---

## 🔍 디버깅 팁

### Firebase Admin SDK 에러
```python
# 에러 발생 시 상세 로그 출력
import logging
logging.getLogger('firebase_admin').setLevel(logging.DEBUG)
```

### DB 쿼리 확인
```python
# SQLAlchemy 쿼리 로그 출력
import logging
logging.basicConfig()
logging.getLogger('sqlalchemy.engine').setLevel(logging.INFO)
```

### 스케줄러 동작 확인
```python
# 스케줄러 작업 목록 출력
from app.services.scheduler import scheduler
print(scheduler.get_jobs())
```

---

## 📞 문의

구현 중 문제 발생 시:
1. Firebase Admin SDK 공식 문서 참고
2. 서버 로그에서 에러 메시지 확인
3. FCM 토큰이 올바르게 등록되었는지 DB 확인

---

**작성일**: 2025-11-18
**작성자**: Claude (AI Assistant)
