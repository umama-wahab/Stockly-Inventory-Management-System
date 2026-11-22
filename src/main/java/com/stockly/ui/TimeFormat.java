package com.stockly.ui;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Turns timestamps into short, friendly text such as "5 min ago" or "Yesterday, 14:30".
 *
 * @author Umama Wahab
 */
public final class TimeFormat {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH);

    /** Utility class - not meant to be instantiated. */
    private TimeFormat() {
    }

    /**
     * Describes how long ago a moment was.
     *
     * @param moment the moment to describe
     * @param now    the current time (passed in so the method is easy to test)
     * @return "Just now", "N min ago", "Today, HH:mm", "Yesterday, HH:mm" or "d MMM, HH:mm"
     */
    public static String relative(LocalDateTime moment, LocalDateTime now) {
        Duration age = Duration.between(moment, now);
        if (age.isNegative() || age.toMinutes() < 1) {
            return "Just now";
        }
        if (age.toMinutes() < 60) {
            return age.toMinutes() + " min ago";
        }
        LocalDate today = now.toLocalDate();
        if (moment.toLocalDate().equals(today)) {
            return "Today, " + moment.format(TIME);
        }
        if (moment.toLocalDate().equals(today.minusDays(1))) {
            return "Yesterday, " + moment.format(TIME);
        }
        return moment.format(DATE_TIME);
    }
}
