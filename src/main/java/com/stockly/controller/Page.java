package com.stockly.controller;

/**
 * The screens that can be shown in the main content area.
 *
 * @author Umama Wahab
 */
public enum Page {

    /** Statistics, chart, low-stock alerts and recent activity. */
    DASHBOARD("Dashboard", "Here is how your store's stock looks right now.", "DashboardView.fxml"),

    /** The product table with add, edit, delete, search and filter. */
    INVENTORY("Inventory", "Add, edit and keep track of every product.", "InventoryView.fxml"),

    /** The simple inventory summary. */
    REPORTS("Reports", "A simple summary of your inventory.", "ReportsView.fxml");

    private final String title;
    private final String subtitle;
    private final String fxmlFile;

    Page(String title, String subtitle, String fxmlFile) {
        this.title = title;
        this.subtitle = subtitle;
        this.fxmlFile = fxmlFile;
    }

    /**
     * Returns the page title shown in the top bar.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the short description shown under the title.
     *
     * @return the subtitle
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Returns the name of the FXML file that defines the page.
     *
     * @return the FXML file name
     */
    public String getFxmlFile() {
        return fxmlFile;
    }
}
