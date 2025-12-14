package com.pbl6.cinemate.payment_service.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatDateTime(Instant dateTime) {
        return dateTime != null ? ZonedDateTime.ofInstant(dateTime, ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
                : null;
    }

    public static String formatDate(Instant dateTime) {
        return dateTime != null ? ZonedDateTime.ofInstant(dateTime, ZoneId.systemDefault()).format(DATE_FORMATTER)
                : null;
    }

    public static Instant parseDateTime(String dateTimeStr) {
        return dateTimeStr != null
                ? ZonedDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER.withZone(ZoneId.systemDefault())).toInstant()
                : null;
    }

    public static long getDaysBetween(Instant start, Instant end) {
        return Duration.between(start, end).toDays();
    }

    public static boolean isExpired(Instant endDate) {
        return endDate != null && endDate.isBefore(Instant.now());
    }

    public static boolean isActive(Instant startDate, Instant endDate) {
        Instant now = Instant.now();
        return startDate != null && endDate != null &&
                !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    public static Instant addDays(Instant dateTime, int days) {
        return dateTime != null ? dateTime.plus(Duration.ofDays(days)) : null;
    }

    public static Instant addMonths(Instant dateTime, int months) {
        return dateTime != null
                ? ZonedDateTime.ofInstant(dateTime, ZoneId.systemDefault()).plusMonths(months).toInstant()
                : null;
    }
}
