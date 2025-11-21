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

    /**
     * API Base URL (BuildConfig에서 자동 설정)
     * Debug: http://localhost:8000
     * Release: https://api.yourserver.com
     */
    public static final String BASE_URL = BuildConfig.BASE_URL;

    /**
     * 에뮬레이터용 Base URL (필요시 사용)
     * 에뮬레이터에서는 10.0.2.2가 호스트 머신의 localhost를 가리킵니다
     */
    public static final String EMULATOR_BASE_URL = "http://10.0.2.2:8000";

    /**
     * 실제 디바이스용 Base URL (필요시 사용)
     * 예: http://localhost:8000
     */
    public static final String DEVICE_BASE_URL = "http://localhost:8000";

    /**
     * 운영 서버 URL
     */
    public static final String PRODUCTION_URL = "https://api.yourserver.com";

    /**
     * API 타임아웃 설정 (초 단위)
     */
    public static final int CONNECT_TIMEOUT = 10;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    /**
     * FCM 관련 설정
     */
    public static final String FCM_NOTIFICATION_CHANNEL_ID = "notification_channel";
    public static final String FCM_NOTIFICATION_CHANNEL_NAME = "알림";

    /**
     * SharedPreferences 키
     */
    public static final String PREF_CALENDAR = "CalendarPrefs";
    public static final String PREF_USER = "UserPrefs";
    public static final String PREF_FCM = "FCMPrefs";

    /**
     * 에뮬레이터 여부 감지
     */
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator");
    }

    /**
     * 현재 사용 중인 Base URL 반환
     * 에뮬레이터/실제 기기 자동 감지
     */
    public static String getBaseUrl() {
        if (!BuildConfig.DEBUG) {
            return PRODUCTION_URL;
        }
        // 디버그 모드: 에뮬레이터면 10.0.2.2, 실제 기기면 localhost (adb reverse 필요)
        return isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
    }

    /**
     * 디버그 모드 여부
     */
    public static boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }

    /**
     * 앱 버전 정보
     */
    public static String getVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * 로그 출력 여부 (디버그 모드에서만)
     */
    public static boolean enableLogging() {
        return BuildConfig.DEBUG;
    }
}
