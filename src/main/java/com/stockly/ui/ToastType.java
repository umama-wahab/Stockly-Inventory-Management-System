package com.stockly.ui;

/**
 * The visual style of a toast notification.
 *
 * @author Umama Wahab
 */
public enum ToastType {

    /** An operation succeeded (green). */
    SUCCESS("success", "check"),

    /** An operation failed (red). */
    ERROR("error", "warning"),

    /** Neutral information (navy). */
    INFO("info", "refresh");

    private final String cssClass;
    private final String iconName;

    ToastType(String cssClass, String iconName) {
        this.cssClass = cssClass;
        this.iconName = iconName;
    }

    /**
     * Returns the suffix for the CSS class {@code toast-<suffix>}.
     *
     * @return the CSS suffix
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * Returns the name of the {@link Icon} drawn inside the toast.
     *
     * @return the icon name
     */
    public String getIconName() {
        return iconName;
    }
}
