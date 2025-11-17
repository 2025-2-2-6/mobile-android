# FCM 구현 완료 보고서

## 📋 개요

안드로이드 앱에서 Firebase Cloud Messaging(FCM)을 사용한 푸시 알림 기능을 구현했습니다.

### 구현된 기능
1. ✅ 크롤링 완료 알림
2. ✅ 새 게시글 알림
3. ✅ 캘린더 일정 알림

---

## 🔧 수정된 파일 목록

### 1. `app/build.gradle.kts`
**위치**: `D:\Develop\GitHub\mobile-android\app\build.gradle.kts`

**수정 내용**: FCM 라이브러리 의존성 추가

```kotlin
dependencies {
    // ... 기존 의존성
    implementation("com.google.firebase:firebase-messaging:24.0.0")  // 추가됨
}
```

**변경 라인**: 47번째 줄

---

### 2. `AndroidManifest.xml`
**위치**: `D:\Develop\GitHub\mobile-android\app\src\main\AndroidManifest.xml`

**수정 내용**:
1. 알림 권한 추가 (5번째 줄)
2. FCM 서비스 등록 (39-46번째 줄)

```xml
<!-- 추가된 권한 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- 추가된 서비스 -->
<service
    android:name=".fcm.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

### 3. `network/ApiClient.java`
**위치**: `D:\Develop\GitHub\mobile-android\app\src\main\java\com\example\mobile_android\network\ApiClient.java`

**수정 내용**: BASE_URL을 public으로 변경 (13번째 줄)

```java
// 변경 전
private static final String BASE_URL = "http://10.0.2.2:8000";

// 변경 후
public static final String BASE_URL = "http://10.0.2.2:8000";
```

**이유**: FcmTokenManager에서 BASE_URL에 접근하기 위함

---

### 4. `ui/login/Login.java`
**위치**: `D:\Develop\GitHub\mobile-android\app\src\main\java\com\example\mobile_android\ui\login\Login.java`

**수정 내용**:

1. **Import 추가** (19번째 줄):
```java
import com.example.mobile_android.fcm.FcmTokenManager;
```

2. **로그인 성공 시 FCM 토큰 초기화** (50-51번째 줄):
```java
@Override
public void onComplete(@NonNull Task<AuthResult> task) {
    if (task.isSuccessful()) {
        // FCM 토큰 초기화 및 서버 전송
        FcmTokenManager.initializeFcmToken(Login.this);

        // 로그인 성공 시 MainActivity로 이동
        Intent intent = new Intent(Login.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
```

3. **이미 로그인된 경우에도 FCM 토큰 초기화** (76-77번째 줄):
```java
if (auth.getCurrentUser() != null) {
    // FCM 토큰 초기화 (이미 로그인된 경우에도)
    FcmTokenManager.initializeFcmToken(this);

    Intent intent = new Intent(Login.this, MainActivity.class);
    startActivity(intent);
    finish();
    return;
}
```

---

## 📁 새로 추가된 파일

### 1. `fcm/MyFirebaseMessagingService.java` ⭐
**위치**: `D:\Develop\GitHub\mobile-android\app\src\main\java\com\example\mobile_android\fcm\MyFirebaseMessagingService.java`

**역할**: FCM 메시지를 수신하고 알림을 표시하는 핵심 서비스

**주요 기능**:

#### 1) FCM 토큰 갱신 처리 (46-53번째 줄)
```java
@Override
public void onNewToken(@NonNull String token) {
    super.onNewToken(token);
    Log.d(TAG, "새로운 FCM 토큰: " + token);

    // 토큰을 서버로 전송
    FcmTokenManager.sendTokenToServer(this, token);
}
```

#### 2) FCM 메시지 수신 처리 (59-91번째 줄)
```java
@Override
public void onMessageReceived(@NonNull RemoteMessage message) {
    Map<String, String> data = message.getData();
    String notificationType = data.get("type");

    // 타입별로 처리
    switch (notificationType) {
        case "crawling_complete":
            handleCrawlingComplete(data);
            break;
        case "new_post":
            handleNewPost(data);
            break;
        case "schedule_reminder":
            handleScheduleReminder(data);
            break;
    }
}
```

#### 3) 알림 타입별 처리 메서드

**크롤링 완료 알림** (96-103번째 줄):
```java
private void handleCrawlingComplete(Map<String, String> data) {
    String siteName = data.getOrDefault("site_name", "사이트");
    String title = siteName + " 크롤링 완료";
    String body = "새로운 정보가 업데이트되었습니다.";

    showNotification(CHANNEL_CRAWLING, title, body, data);
}
```

**새 게시글 알림** (108-118번째 줄):
```java
private void handleNewPost(Map<String, String> data) {
    String siteName = data.getOrDefault("site_name", "사이트");
    String postTitle = data.getOrDefault("post_title", "새 게시글");

    String title = siteName + " 새 게시글";
    String body = postTitle;

    showNotification(CHANNEL_NEW_POST, title, body, data);
}
```

**일정 알림** (123-133번째 줄):
```java
private void handleScheduleReminder(Map<String, String> data) {
    String eventTitle = data.getOrDefault("event_title", "일정");
    String eventTime = data.getOrDefault("event_time", "");

    String title = "일정 알림";
    String body = eventTitle;
    if (!eventTime.isEmpty()) {
        body += " (" + eventTime + ")";
    }

    showNotification(CHANNEL_SCHEDULE, title, body, data);
}
```

#### 4) 알림 표시 (138-175번째 줄)
```java
private void showNotification(String channelId, String title, String body, Map<String, String> data) {
    NotificationManager notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    // MainActivity로 이동하는 Intent
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    // 데이터 전달
    for (Map.Entry<String, String> entry : data.entrySet()) {
        intent.putExtra(entry.getKey(), entry.getValue());
    }

    PendingIntent pendingIntent = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    // 알림 생성
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

    int notificationId = (int) System.currentTimeMillis();
    notificationManager.notify(notificationId, builder.build());
}
```

#### 5) 알림 채널 생성 (181-215번째 줄)
```java
private void createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationManager manager = getSystemService(NotificationManager.class);

        // 크롤링 완료 채널
        NotificationChannel crawlingChannel = new NotificationChannel(
            CHANNEL_CRAWLING,
            "크롤링 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        manager.createNotificationChannel(crawlingChannel);

        // 새 게시글 채널
        NotificationChannel newPostChannel = new NotificationChannel(
            CHANNEL_NEW_POST,
            "새 게시글 알림",
            NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(newPostChannel);

        // 일정 알림 채널
        NotificationChannel scheduleChannel = new NotificationChannel(
            CHANNEL_SCHEDULE,
            "일정 알림",
            NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(scheduleChannel);
    }
}
```

---

### 2. `fcm/FcmTokenManager.java` ⭐
**위치**: `D:\Develop\GitHub\mobile-android\app\src\main\java\com\example\mobile_android\fcm\FcmTokenManager.java`

**역할**: FCM 토큰 생성, 저장, 서버 전송 관리

**주요 기능**:

#### 1) FCM 토큰 초기화 (32-49번째 줄)
```java
public static void initializeFcmToken(Context context) {
    FirebaseMessaging.getInstance().getToken()
        .addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "FCM 토큰 가져오기 실패", task.getException());
                return;
            }

            // 토큰 획득
            String token = task.getResult();
            Log.d(TAG, "FCM 토큰: " + token);

            // SharedPreferences에 저장
            saveTokenLocally(context, token);

            // 서버로 전송
            sendTokenToServer(context, token);
        });
}
```

#### 2) 로컬 저장 (54-59번째 줄)
```java
private static void saveTokenLocally(Context context, String token) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
    Log.d(TAG, "FCM 토큰 로컬 저장 완료");
}
```

#### 3) 서버로 전송 (71-119번째 줄)
```java
public static void sendTokenToServer(Context context, String token) {
    // Firebase 인증된 사용자 확인
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Log.w(TAG, "사용자가 로그인되지 않음. 토큰 전송 보류");
        return;
    }

    String userId = user.getUid();

    // 백그라운드에서 서버로 전송
    executor.execute(() -> {
        try {
            // API 엔드포인트
            String url = ApiClient.BASE_URL + "/api/v1/fcm/register";

            // JSON 데이터 생성
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_id", userId);
            jsonBody.put("fcm_token", token);
            jsonBody.put("platform", "android");
            jsonBody.put("device_info", Build.MODEL);

            RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
            );

            // HTTP 요청 생성
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 요청 실행
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                Log.d(TAG, "FCM 토큰 서버 전송 성공");
            } else {
                Log.e(TAG, "FCM 토큰 서버 전송 실패: " + response.code());
            }

            response.close();

        } catch (Exception e) {
            Log.e(TAG, "FCM 토큰 서버 전송 중 오류", e);
        }
    });
}
```

#### 4) 토큰 삭제 (로그아웃용) (124-136번째 줄)
```java
public static void deleteToken(Context context) {
    FirebaseMessaging.getInstance().deleteToken()
        .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "FCM 토큰 삭제 완료");
                // 로컬 저장소에서도 제거
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().remove(KEY_FCM_TOKEN).apply();
            } else {
                Log.w(TAG, "FCM 토큰 삭제 실패", task.getException());
            }
        });
}
```

---

## 🔄 동작 흐름

### 1. 앱 시작 및 로그인
```
1. 사용자가 Google 로그인
   ↓
2. Login.java:51 - FcmTokenManager.initializeFcmToken() 호출
   ↓
3. FcmTokenManager.java:34 - Firebase에서 FCM 토큰 발급
   ↓
4. FcmTokenManager.java:43 - SharedPreferences에 토큰 저장
   ↓
5. FcmTokenManager.java:46 - 서버로 토큰 전송
   ↓
6. 서버: fcm_tokens 테이블에 저장
```

### 2. FCM 메시지 수신
```
1. 서버에서 FCM 메시지 발송
   ↓
2. MyFirebaseMessagingService.java:59 - onMessageReceived() 호출
   ↓
3. MyFirebaseMessagingService.java:76 - 메시지 타입 확인 (type 필드)
   ↓
4. MyFirebaseMessagingService.java:78-86 - 타입별 핸들러 호출
   ↓
5. MyFirebaseMessagingService.java:138 - showNotification() 호출
   ↓
6. 사용자에게 푸시 알림 표시
```

### 3. 알림 클릭
```
1. 사용자가 알림 클릭
   ↓
2. MyFirebaseMessagingService.java:149 - PendingIntent 실행
   ↓
3. MainActivity로 이동 (데이터 전달됨)
```

---

## 📊 FCM 메시지 데이터 형식

### 1. 크롤링 완료 알림
```json
{
  "data": {
    "type": "crawling_complete",
    "site_id": "uuid",
    "site_name": "서울대학교 공지사항"
  }
}
```

### 2. 새 게시글 알림
```json
{
  "data": {
    "type": "new_post",
    "site_id": "uuid",
    "site_name": "서울대학교 공지사항",
    "post_id": "uuid",
    "post_title": "2025학년도 1학기 수강신청 안내"
  }
}
```

### 3. 일정 알림
```json
{
  "data": {
    "type": "schedule_reminder",
    "event_id": "uuid",
    "event_title": "팀 프로젝트 미팅",
    "event_time": "2025-11-18 14:00"
  }
}
```

---

## 🧪 테스트 방법

### 1. FCM 토큰 확인
**Logcat 필터**: `FcmTokenManager`

**기대 로그**:
```
D/FcmTokenManager: FCM 토큰: fAbCdEfG1h2IjK3lM4nO5pQ6rS7tU8vW9xY0zA...
D/FcmTokenManager: FCM 토큰 로컬 저장 완료
D/FcmTokenManager: FCM 토큰 서버 전송 성공
```

### 2. Firebase Console로 테스트 푸시 발송
1. Firebase Console 접속
2. Cloud Messaging 메뉴 선택
3. "Send test message" 클릭
4. Logcat에서 확인한 FCM 토큰 입력
5. **Advanced options → Custom data** 추가:
   ```
   type: new_post
   site_name: 테스트 사이트
   post_title: 테스트 게시글입니다
   ```
6. "Test" 버튼 클릭

**기대 결과**:
- 앱에 푸시 알림 표시
- 알림 제목: "테스트 사이트 새 게시글"
- 알림 내용: "테스트 게시글입니다"

### 3. Logcat으로 메시지 수신 확인
**Logcat 필터**: `FCMService`

**기대 로그**:
```
D/FCMService: FCM 메시지 수신: [발신자]
D/FCMService: 알림 표시 완료: 테스트 사이트 새 게시글
```

---

## ⚠️ 필수 설정 사항

### 1. Firebase 프로젝트 설정
1. Firebase Console에서 프로젝트 생성
2. Android 앱 등록 (패키지명: `com.example.mobile_android`)
3. `google-services.json` 다운로드
4. 파일 위치: `app/google-services.json`

### 2. 알림 권한 런타임 요청 (Android 13+)
`MainActivity.java`에 추가 권장:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
    }
}
```

### 3. 서버 URL 변경
실제 기기 테스트 시 `ApiClient.java:13` 수정:

```java
// 에뮬레이터
public static final String BASE_URL = "http://10.0.2.2:8000";

// 실제 기기 (서버 IP로 변경)
public static final String BASE_URL = "http://192.168.0.10:8000";
```

---

## 📈 다음 단계

### 안드로이드 개발자
- [ ] Firebase 프로젝트 설정 및 `google-services.json` 추가
- [ ] Firebase Console로 테스트 푸시 발송하여 수신 확인
- [ ] 알림 권한 런타임 요청 코드 추가
- [ ] 로그아웃 시 `FcmTokenManager.deleteToken()` 호출 추가

### 백엔드 개발자
- [ ] FCM 토큰 등록 API 구현 (`POST /api/v1/fcm/register`)
- [ ] 크롤링 완료 시 FCM 푸시 발송
- [ ] 새 게시글 감지 시 FCM 푸시 발송
- [ ] 일정 알림 스케줄러 및 FCM 푸시 발송
- [ ] `/posts/list` API에 `is_new` 필드 추가

---

## 📞 문의

구현 과정에서 문제가 발생하면:
1. Logcat에서 에러 로그 확인
2. Firebase Console에서 프로젝트 설정 확인
3. 서버 API 응답 상태 코드 확인

---

**작성일**: 2025-11-18
**작성자**: Claude (AI Assistant)
