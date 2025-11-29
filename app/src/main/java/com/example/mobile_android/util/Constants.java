package com.example.mobile_android.util;

import com.example.mobile_android.BuildConfig;

/**
 * 앱 전체에서 사용되는 상수 정의
 */
public class Constants {
    /**
     * 게시물을 "새 게시물"로 간주하는 시간 기준 (밀리초)
     * - DEBUG 빌드: 1분 (60 * 1000)
     * - RELEASE 빌드: 24시간 (24 * 60 * 60 * 1000)
     */
    public static final long NEW_POST_THRESHOLD_MS =
            BuildConfig.DEBUG
                    ? 60 * 1000L // 1분 (디버그용)
                    : 24 * 60 * 60 * 1000L; // 24시간 (릴리즈용)
}
