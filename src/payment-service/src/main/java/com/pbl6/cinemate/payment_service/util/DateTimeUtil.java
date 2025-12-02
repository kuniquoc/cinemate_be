package com.pbl6.cinemate.payment_service.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : null;
    }
    
    public static String formatDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
    }
    
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return dateTimeStr != null ? LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER) : null;
    }
    
    public static long getDaysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }
    
    public static boolean isExpired(LocalDateTime endDate) {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }
    
    public static boolean isActive(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        return startDate != null && endDate != null && 
               !now.isBefore(startDate) && !now.isAfter(endDate);
    }
    
    public static LocalDateTime addDays(LocalDateTime dateTime, int days) {
        return dateTime != null ? dateTime.plusDays(days) : null;
    }
    
    public static LocalDateTime addMonths(LocalDateTime dateTime, int months) {
        return dateTime != null ? dateTime.plusMonths(months) : null;
    }
}
