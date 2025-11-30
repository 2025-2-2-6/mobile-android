package com.example.mobile_android.config;

import android.os.Build;

import com.example.mobile_android.BuildConfig;

/**
 * 앱 전역 설정 관리 클래스
 *
 * Base URL은 build.gradle.kts에서 환경별로 자동 설정됩니다:
 * - Debug 빌드: http://localhost:8000 (개발용)
 * - Release 빌드: https://api.yourserver.com (운영용)
 *
 * 사용법:
 * 1. 에뮬레이터에서 테스트: BASE_URL 그대로 사용 (10.0.2.2:8000)
 * 2. 실제 디바이스에서 테스트: ADB reverse 설정 후 localhost:8000 사용
 *    명령어: adb reverse tcp:8000 tcp:8000
 * 3. 운영 서버 연결: build.gradle.kts의 release BASE_URL 수정
 */
public class AppConfig {

    // Base URL은 BuildConfig에만 존재해야 함!
    public static String getBaseUrl() {
        return BuildConfig.BASE_URL;
    }

    // Timeout 유지 OK
    public static final int CONNECT_TIMEOUT = 10;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    public static final String FCM_NOTIFICATION_CHANNEL_ID = "notification_channel";
    public static final String FCM_NOTIFICATION_CHANNEL_NAME = "알림";

    public static final String PREF_CALENDAR = "CalendarPrefs";
    public static final String PREF_USER = "UserPrefs";
    public static final String PREF_FCM = "FCMPrefs";

    public static boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }

    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public static boolean enableLogging() {
        return BuildConfig.DEBUG;
    }
}
