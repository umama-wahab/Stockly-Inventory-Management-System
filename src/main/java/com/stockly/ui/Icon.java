package com.stockly.ui;

import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

/**
 * A small vector icon. It is a plain {@link Region} whose shape is an SVG path, so its colour is controlled
 * from CSS ({@code -fx-background-color}) and it stays sharp at any size. It can be used directly in FXML:
 * <pre>{@code <Icon name="plus" size="18"/>}</pre>
 *
 * @author Umama Wahab
 */
public final class Icon extends Region {

    /** SVG path data (24x24 grid) for every available icon name. */
    private static final Map<String, String> PATHS = Map.ofEntries(
            Map.entry("dashboard", "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"),
            Map.entry("inventory", "M20 2H4c-1 0-2 .9-2 2v3.01c0 .72.43 1.34 1 1.69V20c0 1.1 1.1 2 2 2h14c.9 0 2-.9 2-2V8.7c.57-.35 1-.97 1-1.69V4c0-1.1-1-2-2-2zm-5 12H9v-2h6v2zm5-7H4V4h16v3z"),
            Map.entry("reports", "M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z"),
            Map.entry("units", "M12 2l-5.5 9h11L12 2zm5.5 11c-2.49 0-4.5 2.01-4.5 4.5s2.01 4.5 4.5 4.5 4.5-2.01 4.5-4.5-2.01-4.5-4.5-4.5zM3 21.5h8v-8H3v8z"),
            Map.entry("plus", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
            Map.entry("search", "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"),
            Map.entry("edit", "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.9959.9959 0 0 0 0-1.41l-2.34-2.34a.9959.9959 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"),
            Map.entry("delete", "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"),
            Map.entry("refresh", "M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"),
            Map.entry("warning", "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"),
            Map.entry("check", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"),
            Map.entry("block", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"),
            Map.entry("close", "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"),
            Map.entry("box", "M21 16.5c0 .38-.21.71-.53.88l-7.9 4.44c-.16.12-.36.18-.57.18s-.41-.06-.57-.18l-7.9-4.44A.991.991 0 0 1 3 16.5v-9c0-.38.21-.71.53-.88l7.9-4.44c.16-.12.36-.18.57-.18s.41.06.57.18l7.9 4.44c.32.17.53.5.53.88v9zM12 4.15L6.04 7.5 12 10.85l5.96-3.35L12 4.15zM5 15.91l6 3.38v-6.71L5 9.21v6.7zm14 0v-6.7l-6 3.37v6.71l6-3.38z")
    );

    private final StringProperty name = new SimpleStringProperty();
    private double size;

    /** Creates an empty icon; set the {@link #setName(String) name} afterwards (this is what FXML does). */
    public Icon() {
        getStyleClass().add("icon");
        setSize(18);
        name.addListener((observable, oldName, newName) -> applyShape(newName));
    }

    /**
     * Creates an icon of the default size (18 px).
     *
     * @param name the icon name, for example {@code "plus"}
     */
    public Icon(String name) {
        this();
        setName(name);
    }

    /**
     * Creates an icon of a given size.
     *
     * @param name the icon name, for example {@code "plus"}
     * @param size the width and height in pixels
     */
    public Icon(String name, double size) {
        this(name);
        setSize(size);
    }

    /**
     * Chooses which drawing to show.
     *
     * @param iconName one of: dashboard, inventory, reports, units, plus, search, edit, delete, refresh,
     *                 warning, check, block, close, box
     * @throws IllegalArgumentException if the name is unknown
     */
    public final void setName(String iconName) {
        name.set(iconName);
    }

    /**
     * Returns the current icon name.
     *
     * @return the icon name, or {@code null} if none was set
     */
    public final String getName() {
        return name.get();
    }

    /**
     * Returns the icon name property.
     *
     * @return the name property
     */
    public final StringProperty nameProperty() {
        return name;
    }

    /**
     * Sets the width and height of the icon.
     *
     * @param pixels the size in pixels
     */
    public final void setSize(double pixels) {
        size = pixels;
        setMinSize(pixels, pixels);
        setPrefSize(pixels, pixels);
        setMaxSize(pixels, pixels);
    }

    /**
     * Returns the width and height of the icon.
     *
     * @return the size in pixels
     */
    public final double getSize() {
        return size;
    }

    private void applyShape(String iconName) {
        String data = PATHS.get(iconName);
        if (data == null) {
            throw new IllegalArgumentException("Unknown icon: " + iconName);
        }
        SVGPath path = new SVGPath();
        path.setContent(data);
        setShape(path);
        setScaleShape(true);
        setCenterShape(true);
    }
}
