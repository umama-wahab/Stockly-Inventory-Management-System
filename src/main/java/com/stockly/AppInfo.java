package com.stockly;

/**
 * Central place for application-wide constants such as the name, tagline and version.
 * Keeping them here avoids repeating the same text in several classes.
 *
 * @author Umama Wahab
 */
public final class AppInfo {

    /** The product name shown in the window title and sidebar. */
    public static final String NAME = "Stockly";

    /** The marketing tagline shown under the logo. */
    public static final String TAGLINE = "Smart Inventory. Simplified Management.";

    /** The current application version, shown at the bottom of the sidebar. */
    public static final String VERSION = "1.0.0";

    /** Utility class - not meant to be instantiated. */
    private AppInfo() {
    }
}
