package com.example.mobile_android.fcm;

import com.example.mobile_android.fcm.handler.BaseNotificationHandler;
import com.example.mobile_android.fcm.handler.CalendarReminderHandler;
import com.example.mobile_android.fcm.handler.CrawlNewPostsHandler;
import com.example.mobile_android.fcm.handler.NotificationHandler;

import java.util.EnumMap;
import java.util.Map;

public class NotificationHandlerFactory {

    private static final Map<NotificationType, NotificationHandler> handlers = new EnumMap<>(NotificationType.class);
    private static final NotificationHandler defaultHandler = new BaseNotificationHandler() {};

    static {
        handlers.put(NotificationType.CALENDAR_REMINDER, new CalendarReminderHandler());
        handlers.put(NotificationType.CRAWL_NEW_POSTS, new CrawlNewPostsHandler());
    }

    public static NotificationHandler getHandler(NotificationType type) {
        return handlers.getOrDefault(type, defaultHandler);
    }

    public static NotificationHandler getHandler(String typeString) {
        NotificationType type = NotificationType.fromString(typeString);
        return getHandler(type);
    }
}
