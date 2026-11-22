package com.stockly.ui;

import com.stockly.model.StockStatus;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * A small coloured "pill" showing a {@link StockStatus}: green for In Stock, amber for Low Stock and soft red
 * for Out of Stock. The colours come from the CSS classes {@code badge-in-stock}, {@code badge-low-stock} and
 * {@code badge-out-of-stock}.
 *
 * @author Umama Wahab
 */
public final class StatusBadge extends Label {

    /** Creates an empty (neutral) badge. */
    public StatusBadge() {
        getStyleClass().add("badge");
        setStatus(null);
    }

    /**
     * Creates a badge for a status.
     *
     * @param status the status to display
     */
    public StatusBadge(StockStatus status) {
        this();
        setStatus(status);
    }

    /**
     * Changes the status shown by the badge.
     *
     * @param status the new status, or {@code null} to show a neutral dash
     */
    public final void setStatus(StockStatus status) {
        getStyleClass().removeIf(styleClass -> styleClass.startsWith("badge-"));
        Region dot = new Region();
        dot.getStyleClass().add("badge-dot");
        setGraphic(dot);
        if (status == null) {
            setText("\u2014");
            getStyleClass().add("badge-neutral");
        } else {
            setText(status.getLabel());
            getStyleClass().add("badge-" + status.getCssClass());
        }
    }
}
