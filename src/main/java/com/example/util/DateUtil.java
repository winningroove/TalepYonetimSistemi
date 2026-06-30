package com.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Uygulama genelinde tarih gösterimi: gün.ay.yıl (dd.MM.yyyy). */
public final class DateUtil {

    private static final DateTimeFormatter GUN_AY_YIL = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DateUtil() {}

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(GUN_AY_YIL);
    }

    public static String format(LocalDate date) {
        return date == null ? "-" : date.format(GUN_AY_YIL);
    }
}
