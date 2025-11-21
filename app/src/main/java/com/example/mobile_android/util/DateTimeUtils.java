package com.example.mobile_android.util;

import android.text.TextUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 공통으로 사용하는 KST 전용 날짜/시간 유틸.
 * 서버에서 내려오는 ISO 문자열이 표준화되어 있지 않아도
 * 앱에서는 항상 한국 시간대를 강제한다.
 */
public final class DateTimeUtils {

    // 서버에서 내려올 수 있는 날짜 형식들을 순서대로 정의
    private static final String[] SERVER_DATE_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
    };

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final TimeZone KST_TIME_ZONE = TimeZone.getTimeZone("Asia/Seoul");
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Locale KOREA = Locale.KOREA;

    private DateTimeUtils() {}

    private static SimpleDateFormat formatter(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, KOREA);
        // 'Z'(Zulu)로 끝나는 패턴은 UTC 타임존으로 명시적으로 지정합니다.
        if (pattern.endsWith("'Z'")) {
            sdf.setTimeZone(UTC_TIME_ZONE);
        } else {
            // 그 외의 경우는 KST를 기준으로 해석합니다.
            sdf.setTimeZone(KST_TIME_ZONE);
        }
        return sdf;
    }

    public static Date parseServerDate(String dateTimeStr) {
        if (TextUtils.isEmpty(dateTimeStr)) {
            return null;
        }
        // 정의된 모든 패턴을 순서대로 시도
        for (String pattern : SERVER_DATE_PATTERNS) {
            try {
                return formatter(pattern).parse(dateTimeStr);
            } catch (ParseException e) {
                // 현재 패턴 실패 시, 다음 패턴으로 계속 진행
            }
        }
        // 모든 패턴 실패 시 null 반환
        return null;
    }

    public static String formatServerDate(String dateTimeStr, String pattern) {
        Date date = parseServerDate(dateTimeStr);
        if (date == null) {
            return "";
        }
        // 출력은 항상 KST 기준으로 포맷
        SimpleDateFormat outputFormatter = new SimpleDateFormat(pattern, KOREA);
        outputFormatter.setTimeZone(KST_TIME_ZONE);
        return outputFormatter.format(date);
    }

    public static LocalDate parseServerDateToLocalDate(String dateTimeStr) {
        if (TextUtils.isEmpty(dateTimeStr)) {
            return null;
        }
        Date date = parseServerDate(dateTimeStr);
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(KST_ZONE_ID).toLocalDate();
    }

    public static TimeZone getKstTimeZone() {
        return KST_TIME_ZONE;
    }

    public static ZoneId getKstZoneId() {
        return KST_ZONE_ID;
    }
}
