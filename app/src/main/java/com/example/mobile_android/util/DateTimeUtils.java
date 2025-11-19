package com.example.mobile_android.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final TimeZone KST_TIME_ZONE = TimeZone.getTimeZone("Asia/Seoul");
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Locale KOREA = Locale.KOREA;

    private DateTimeUtils() {}

    private static SimpleDateFormat formatter(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, KOREA);
        sdf.setTimeZone(KST_TIME_ZONE);
        return sdf;
    }

    public static Date parseServerDate(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return formatter(DEFAULT_PATTERN).parse(dateTimeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String formatServerDate(String dateTimeStr, String pattern) {
        Date date = parseServerDate(dateTimeStr);
        if (date == null) {
            return "";
        }
        return formatter(pattern).format(date);
    }

    public static TimeZone getKstTimeZone() {
        return KST_TIME_ZONE;
    }

    public static ZoneId getKstZoneId() {
        return KST_ZONE_ID;
    }
}
