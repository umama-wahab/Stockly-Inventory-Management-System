package com.stockly.model;

import java.time.LocalDateTime;

/**
 * One entry of the recent-activity feed shown on the dashboard.
 *
 * @param type      what kind of change happened
 * @param message   a human readable description, for example {@code Added "Desk Lamp" with 12 units}
 * @param timestamp when the change happened
 * @author Umama Wahab
 */
public record Activity(ActivityType type, String message, LocalDateTime timestamp) {
}
