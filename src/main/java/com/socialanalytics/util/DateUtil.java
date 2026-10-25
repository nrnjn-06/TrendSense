package com.socialanalytics.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date formatting and manipulation utilities.
 */
public final class DateUtil {
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FRIENDLY_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("h a");

    private DateUtil() {
    }

    public static String formatIso(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(ISO_FORMAT);
    }

    public static String formatFriendly(LocalDateTime ldt) {
        if (ldt == null) return "N/A";
        return ldt.format(FRIENDLY_FORMAT);
    }

    public static String formatHour(int hourOfDay) {
        int hour12 = hourOfDay % 12;
        if (hour12 == 0) hour12 = 12;
        String amPm = (hourOfDay < 12) ? "AM" : "PM";
        return hour12 + " " + amPm;
    }

    public static String formatHourWindow(int startHour) {
        int endHour = (startHour + 2) % 24;
        return formatHour(startHour) + " – " + formatHour(endHour);
    }
}
