package com.stockly.controller;

import com.stockly.model.Activity;
import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import com.stockly.service.InventoryManager;
import com.stockly.ui.EmptyState;
import com.stockly.ui.Icon;
import com.stockly.ui.TimeFormat;
import java.time.LocalDateTime;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller of the dashboard page. Every number, the chart, the low-stock list and the activity feed are
 * recalculated from the live inventory whenever it changes - nothing on this page is hard-coded.
 *
 * @author Umama Wahab
 */
public class DashboardController {

    private static final int MAX_LOW_STOCK_ROWS = 5;
    private static final int MAX_ACTIVITY_ROWS = 6;

    private final InventoryManager manager;
    private final AppShell shell;

    @FXML private Label totalProductsValue;
    @FXML private Label totalProductsHint;
    @FXML private Label totalUnitsValue;
    @FXML private Label lowStockValue;
    @FXML private Label lowStockHint;
    @FXML private Label outOfStockValue;
    @FXML private Label outOfStockHint;
    @FXML private VBox emptyHolder;
    @FXML private VBox contentSection;
    @FXML private PieChart stockChart;
    @FXML private Label legendInStock;
    @FXML private Label legendLowStock;
    @FXML private Label legendOutOfStock;
    @FXML private Label lowStockCount;
    @FXML private VBox lowStockList;
    @FXML private VBox activityList;

    private final PieChart.Data inStockSlice = new PieChart.Data("In Stock", 0);
    private final PieChart.Data lowStockSlice = new PieChart.Data("Low Stock", 0);
    private final PieChart.Data outOfStockSlice = new PieChart.Data("Out of Stock", 0);

    /**
     * Creates the controller.
     *
     * @param manager the shared inventory logic
     * @param shell   the main window services (navigation, dialogs)
     */
    public DashboardController(InventoryManager manager, AppShell shell) {
        this.manager = manager;
        this.shell = shell;
    }

    /** Called by the FXML loader; builds the empty state, the chart data and registers the change listeners. */
    @FXML
    private void initialize() {
        emptyHolder.getChildren().add(new EmptyState("inventory", "Your inventory is empty",
                "Add your first product and Stockly will start tracking stock levels, low-stock alerts and activity for you.",
                "Add your first product", () -> shell.openProductDialog(null)));

        // The slices are always added in the same order, so the CSS colours (default-color0..2) stay fixed.
        stockChart.getData().addAll(inStockSlice, lowStockSlice, outOfStockSlice);

        manager.getProducts().addListener((ListChangeListener<Product>) change -> refresh());
        manager.getActivities().addListener((ListChangeListener<Activity>) change -> refreshActivity());
        refresh();
    }

    /** Recalculates all statistics, the chart and the lists from the current inventory. */
    private void refresh() {
        int total = manager.getTotalProducts();
        int inStock = manager.countByStatus(StockStatus.IN_STOCK);
        int lowStock = manager.countByStatus(StockStatus.LOW_STOCK);
        int outOfStock = manager.countByStatus(StockStatus.OUT_OF_STOCK);

        totalProductsValue.setText(format(total));
        totalProductsHint.setText(total == 0 ? "No products yet" : inStock + " fully stocked");
        totalUnitsValue.setText(format(manager.getTotalUnits()));
        lowStockValue.setText(format(lowStock));
        lowStockHint.setText(lowStock == 0 ? "Nothing running low" : "Reorder soon");
        outOfStockValue.setText(format(outOfStock));
        outOfStockHint.setText(outOfStock == 0 ? "Nothing sold out" : "Unavailable right now");

        boolean empty = total == 0;
        emptyHolder.setVisible(empty);
        emptyHolder.setManaged(empty);
        contentSection.setVisible(!empty);
        contentSection.setManaged(!empty);

        inStockSlice.setPieValue(inStock);
        lowStockSlice.setPieValue(lowStock);
        outOfStockSlice.setPieValue(outOfStock);
        legendInStock.setText(String.valueOf(inStock));
        legendLowStock.setText(String.valueOf(lowStock));
        legendOutOfStock.setText(String.valueOf(outOfStock));

        refreshLowStock(lowStock);
        refreshActivity();
    }

    private void refreshLowStock(int lowStock) {
        lowStockCount.setText(lowStock + (lowStock == 1 ? " item" : " items"));
        lowStockList.getChildren().clear();

        List<Product> lowProducts = manager.getLowStockProducts();
        if (lowProducts.isEmpty()) {
            lowStockList.getChildren().add(mutedMessage("All good - no products are running low."));
            return;
        }
        for (Product product : lowProducts.stream().limit(MAX_LOW_STOCK_ROWS).toList()) {
            Label name = new Label(product.getName());
            name.getStyleClass().add("list-primary");
            name.setMaxWidth(260);
            Label id = new Label(product.getDisplayId());
            id.getStyleClass().add("list-secondary");
            VBox text = new VBox(2, name, id);

            Label units = new Label(product.getQuantity() + (product.getQuantity() == 1 ? " unit left" : " units left"));
            units.getStyleClass().add("badge");
            units.getStyleClass().add("badge-low-stock");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(12, text, spacer, units);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");
            lowStockList.getChildren().add(row);
        }
        if (lowProducts.size() > MAX_LOW_STOCK_ROWS) {
            lowStockList.getChildren().add(mutedMessage("+ " + (lowProducts.size() - MAX_LOW_STOCK_ROWS) + " more"));
        }
    }

    private void refreshActivity() {
        activityList.getChildren().clear();
        List<Activity> recent = manager.getActivities().stream().limit(MAX_ACTIVITY_ROWS).toList();
        if (recent.isEmpty()) {
            activityList.getChildren().add(mutedMessage("No activity yet. Changes you make will appear here."));
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Activity activity : recent) {
            String iconName = switch (activity.type()) {
                case ADDED -> "plus";
                case UPDATED -> "edit";
                case DELETED -> "delete";
            };
            String tone = switch (activity.type()) {
                case ADDED -> "emerald";
                case UPDATED -> "navy";
                case DELETED -> "red";
            };
            Icon icon = new Icon(iconName, 16);
            icon.getStyleClass().add("icon-" + tone);
            StackPane bubble = new StackPane(icon);
            bubble.getStyleClass().addAll("icon-bubble-small", "bubble-" + tone);

            Label message = new Label(activity.message());
            message.getStyleClass().add("list-primary");
            message.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(message, Priority.ALWAYS);

            Label time = new Label(TimeFormat.relative(activity.timestamp(), now));
            time.getStyleClass().add("list-secondary");

            HBox row = new HBox(14, bubble, message, time);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("list-row");
            activityList.getChildren().add(row);
        }
    }

    private static Label mutedMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted-message");
        label.setWrapText(true);
        return label;
    }

    private static String format(long number) {
        return String.format("%,d", number);
    }

    /** Handles "View low stock": opens the inventory table already filtered to low-stock products. */
    @FXML
    private void handleViewLowStock() {
        shell.openInventory(StockStatus.LOW_STOCK);
    }

    /** Handles "View inventory" in the activity card. */
    @FXML
    private void handleViewInventory() {
        shell.openInventory(null);
    }
}
