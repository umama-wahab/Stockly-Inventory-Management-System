package com.stockly.controller;

import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import com.stockly.ui.ToastType;
import javafx.beans.property.StringProperty;

/**
 * The services the main window offers to the page controllers (dashboard, inventory, reports).
 * <p>
 * Page controllers depend only on this small interface, not on {@link MainController}, which keeps them
 * independent of each other and easy to reason about.
 * </p>
 *
 * @author Umama Wahab
 */
public interface AppShell {

    /**
     * Shows a short notification in the corner of the window.
     *
     * @param message the text to display
     * @param type    the visual style (success, error, info)
     */
    void showToast(String message, ToastType type);

    /**
     * Switches to another page.
     *
     * @param page the page to show
     */
    void navigateTo(Page page);

    /**
     * Opens the add/edit dialog.
     *
     * @param product the product to edit, or {@code null} to add a new product
     */
    void openProductDialog(Product product);

    /**
     * Shows the inventory page with a stock-status filter already applied.
     *
     * @param filter the status to filter by, or {@code null} to show all products
     */
    void openInventory(StockStatus filter);

    /**
     * Returns the text typed into the search field in the top bar. The inventory table filters by it.
     *
     * @return the shared search text property
     */
    StringProperty searchQueryProperty();
}
