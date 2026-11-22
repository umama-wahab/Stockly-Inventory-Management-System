package com.stockly.controller;

import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import com.stockly.service.InventoryException;
import com.stockly.service.InventoryManager;
import com.stockly.service.PersistenceException;
import com.stockly.ui.ConfirmDialog;
import com.stockly.ui.EmptyState;
import com.stockly.ui.Icon;
import com.stockly.ui.StatusBadge;
import com.stockly.ui.ToastType;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Controller of the inventory page: the product table with search, status filter, edit, delete and refresh.
 * <p>
 * The table is fed by a chain of observable lists:
 * {@code manager.getProducts()} &rarr; {@link FilteredList} (search + status filter) &rarr; {@link SortedList}
 * (column sorting) &rarr; {@link TableView}. Because every link is observable, the table updates itself.
 * </p>
 *
 * @author Umama Wahab
 */
public class InventoryController {

    private final InventoryManager manager;
    private final AppShell shell;
    private final ObjectProperty<StockStatus> statusFilter = new SimpleObjectProperty<>();
    private FilteredList<Product> filteredProducts;
    private EmptyState placeholder;

    @FXML private VBox tableCard;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, Number> quantityColumn;
    @FXML private TableColumn<Product, StockStatus> statusColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;
    @FXML private ToggleGroup filterGroup;
    @FXML private ToggleButton filterAll;
    @FXML private ToggleButton filterInStock;
    @FXML private ToggleButton filterLowStock;
    @FXML private ToggleButton filterOutOfStock;
    @FXML private Label resultLabel;

    /**
     * Creates the controller.
     *
     * @param manager the shared inventory logic
     * @param shell   the main window services (dialogs, toasts, search text)
     */
    public InventoryController(InventoryManager manager, AppShell shell) {
        this.manager = manager;
        this.shell = shell;
    }

    /** Called by the FXML loader: configures the table columns, filtering, sorting and empty states. */
    @FXML
    private void initialize() {
        // Round the corners of the card around the table.
        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(tableCard.widthProperty());
        clip.heightProperty().bind(tableCard.heightProperty());
        tableCard.setClip(clip);

        configureColumns();
        configureFilterChips();

        filteredProducts = new FilteredList<>(manager.getProducts(), product -> true);
        filteredProducts.predicateProperty().bind(Bindings.createObjectBinding(
                () -> product -> InventoryManager.matches(product, shell.searchQueryProperty().get(), statusFilter.get()),
                shell.searchQueryProperty(), statusFilter));

        SortedList<Product> sortedProducts = new SortedList<>(filteredProducts);
        sortedProducts.comparatorProperty().bind(productTable.comparatorProperty());
        productTable.setItems(sortedProducts);

        placeholder = new EmptyState("inventory", "", "", null, null);
        productTable.setPlaceholder(placeholder);

        manager.getProducts().addListener((ListChangeListener<Product>) change -> updateSummary());
        filteredProducts.addListener((ListChangeListener<Product>) change -> updateSummary());
        statusFilter.addListener((observable, oldValue, newValue) -> updateSummary());
        shell.searchQueryProperty().addListener((observable, oldValue, newValue) -> updateSummary());
        updateSummary();
    }

    // ------------------------------------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------------------------------------

    private void configureColumns() {
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        idColumn.setCellFactory(column -> textCell(Product::formatId, "id-cell"));

        nameColumn.setCellValueFactory(cell -> cell.getValue().nameProperty());
        nameColumn.setCellFactory(column -> textCell(Function.identity(), "name-cell"));

        quantityColumn.setCellValueFactory(cell -> cell.getValue().quantityProperty());
        quantityColumn.setCellFactory(column -> textCell(number -> String.format("%,d", number.intValue()), "quantity-cell"));

        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new TableCell<>() {
            private final StatusBadge badge = new StatusBadge();

            @Override
            protected void updateItem(StockStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    badge.setStatus(status);
                    setGraphic(badge);
                }
            }
        });

        actionsColumn.setSortable(false);
        actionsColumn.setCellFactory(column -> new ActionsCell());
    }

    /** Builds a table cell that shows plain text produced from the cell value. */
    private static <T> TableCell<Product, T> textCell(Function<T, String> formatter, String styleClass) {
        return new TableCell<>() {
            {
                getStyleClass().add(styleClass);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.apply(item));
            }
        };
    }

    private void configureFilterChips() {
        filterAll.setUserData(null);
        filterInStock.setUserData(StockStatus.IN_STOCK);
        filterLowStock.setUserData(StockStatus.LOW_STOCK);
        filterOutOfStock.setUserData(StockStatus.OUT_OF_STOCK);
        filterGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true); // one chip must always stay selected
            } else {
                statusFilter.set((StockStatus) newToggle.getUserData());
            }
        });
    }

    // ------------------------------------------------------------------------------------------------
    // Public API used by the shell
    // ------------------------------------------------------------------------------------------------

    /**
     * Selects a status filter chip, for example when the dashboard links to "low stock".
     *
     * @param status the status to filter by, or {@code null} for all products
     */
    public void setStatusFilter(StockStatus status) {
        ToggleButton chip = status == null ? filterAll
                : status == StockStatus.IN_STOCK ? filterInStock
                : status == StockStatus.LOW_STOCK ? filterLowStock : filterOutOfStock;
        chip.setSelected(true);
    }

    // ------------------------------------------------------------------------------------------------
    // Reacting to changes
    // ------------------------------------------------------------------------------------------------

    /** Updates the chip counters, the "showing x of y" label and the text shown when the table is empty. */
    private void updateSummary() {
        filterAll.setText("All  " + manager.getTotalProducts());
        filterInStock.setText("In Stock  " + manager.countByStatus(StockStatus.IN_STOCK));
        filterLowStock.setText("Low Stock  " + manager.countByStatus(StockStatus.LOW_STOCK));
        filterOutOfStock.setText("Out of Stock  " + manager.countByStatus(StockStatus.OUT_OF_STOCK));

        int total = manager.getTotalProducts();
        int shown = filteredProducts.size();
        resultLabel.setText(total == 0 ? "" : "Showing " + shown + " of " + total + (total == 1 ? " product" : " products"));

        if (total == 0) {
            placeholder.update("Your inventory is empty",
                    "Add your first product to start tracking stock levels.", "Add Product",
                    () -> shell.openProductDialog(null));
        } else {
            placeholder.update("No matching products",
                    "Nothing matches your search or filter. Try something different or clear the filters.",
                    "Clear filters", this::clearFilters);
        }
    }

    private void clearFilters() {
        shell.searchQueryProperty().set("");
        setStatusFilter(null);
    }

    // ------------------------------------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------------------------------------

    /** Handles the Refresh button: reloads the inventory from the data file. */
    @FXML
    private void handleRefresh() {
        try {
            manager.load();
            int count = manager.getTotalProducts();
            shell.showToast("Inventory refreshed - " + count + (count == 1 ? " product" : " products") + " loaded.",
                    ToastType.INFO);
        } catch (PersistenceException e) {
            shell.showToast(e.getMessage(), ToastType.ERROR);
        }
    }

    /**
     * Asks for confirmation and then deletes a product, reporting the result with a toast.
     *
     * @param product the product to delete
     */
    private void confirmAndDelete(Product product) {
        if (!ConfirmDialog.confirmDelete(productTable.getScene().getWindow(), product.getName())) {
            return;
        }
        try {
            manager.deleteProduct(product.getId());
            shell.showToast("\"" + product.getName() + "\" was deleted.", ToastType.SUCCESS);
        } catch (InventoryException e) {
            // Includes ProductNotFoundException: the product vanished (for example after a refresh).
            shell.showToast(e.getMessage(), ToastType.ERROR);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Actions column
    // ------------------------------------------------------------------------------------------------

    /** The cell with the Edit and Delete icon buttons of a table row. */
    private final class ActionsCell extends TableCell<Product, Void> {

        private final HBox buttons = new HBox(4);

        ActionsCell() {
            Button edit = iconButton("edit", "Edit product", "icon-btn-neutral");
            edit.setOnAction(event -> shell.openProductDialog(getTableRow().getItem()));
            Button delete = iconButton("delete", "Delete product", "icon-btn-danger");
            delete.setOnAction(event -> confirmAndDelete(getTableRow().getItem()));
            buttons.getChildren().addAll(edit, delete);
            buttons.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            boolean hasRow = !empty && getTableRow() != null && getTableRow().getItem() != null;
            setGraphic(hasRow ? buttons : null);
        }

        private static Button iconButton(String iconName, String tooltip, String styleClass) {
            Button button = new Button();
            button.setGraphic(new Icon(iconName, 16));
            button.getStyleClass().addAll("icon-btn", styleClass);
            button.setTooltip(new Tooltip(tooltip));
            button.setFocusTraversable(false);
            return button;
        }
    }
}
