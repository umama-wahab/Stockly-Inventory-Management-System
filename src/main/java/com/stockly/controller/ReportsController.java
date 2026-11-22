package com.stockly.controller;

import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import com.stockly.service.InventoryManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Controller of the reports page: a simple inventory summary with the share of products per stock status.
 * Like the dashboard, it recalculates itself whenever the inventory changes.
 *
 * @author Umama Wahab
 */
public class ReportsController {

    private static final DateTimeFormatter UPDATED_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private final InventoryManager manager;

    @FXML private Label totalProductsValue;
    @FXML private Label totalUnitsValue;
    @FXML private Label inStockValue;
    @FXML private Label inStockShare;
    @FXML private Label lowStockValue;
    @FXML private Label lowStockShare;
    @FXML private Label outOfStockValue;
    @FXML private Label outOfStockShare;
    @FXML private HBox stockBar;
    @FXML private Region barInStock;
    @FXML private Region barLowStock;
    @FXML private Region barOutOfStock;
    @FXML private Label updatedLabel;

    /**
     * Creates the controller.
     *
     * @param manager the shared inventory logic
     */
    public ReportsController(InventoryManager manager) {
        this.manager = manager;
    }

    /** Called by the FXML loader: shows the first summary and keeps it up to date. */
    @FXML
    private void initialize() {
        manager.getProducts().addListener((ListChangeListener<Product>) change -> refresh());
        refresh();
    }

    /** Recalculates every number and the stacked bar. */
    private void refresh() {
        int total = manager.getTotalProducts();
        int inStock = manager.countByStatus(StockStatus.IN_STOCK);
        int lowStock = manager.countByStatus(StockStatus.LOW_STOCK);
        int outOfStock = manager.countByStatus(StockStatus.OUT_OF_STOCK);

        totalProductsValue.setText(String.format("%,d", total));
        totalUnitsValue.setText(String.format("%,d", manager.getTotalUnits()));
        inStockValue.setText(String.valueOf(inStock));
        lowStockValue.setText(String.valueOf(lowStock));
        outOfStockValue.setText(String.valueOf(outOfStock));
        inStockShare.setText(percent(inStock, total));
        lowStockShare.setText(percent(lowStock, total));
        outOfStockShare.setText(percent(outOfStock, total));

        bindSegment(barInStock, inStock, total);
        bindSegment(barLowStock, lowStock, total);
        bindSegment(barOutOfStock, outOfStock, total);

        updatedLabel.setText("Last updated " + LocalDateTime.now().format(UPDATED_FORMAT));
    }

    /** Makes a bar segment as wide as its share of the whole bar, and keeps it correct when the window is resized. */
    private void bindSegment(Region segment, int count, int total) {
        segment.prefWidthProperty().unbind();
        if (total == 0) {
            segment.setPrefWidth(0);
        } else {
            segment.prefWidthProperty().bind(stockBar.widthProperty().multiply((double) count / total));
        }
    }

    private static String percent(int part, int total) {
        return total == 0 ? "0%" : String.format("%.0f%%", 100.0 * part / total);
    }
}
