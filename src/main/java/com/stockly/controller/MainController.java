package com.stockly.controller;

import com.stockly.AppInfo;
import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import com.stockly.service.InventoryManager;
import com.stockly.ui.Icon;
import com.stockly.ui.Resources;
import com.stockly.ui.ToastType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller of the application shell: the sidebar, the top bar, page navigation and toast notifications.
 * It loads the three page views once, creates their controllers and swaps them into the content area when
 * the user clicks a sidebar item.
 *
 * @author Umama Wahab
 */
public class MainController implements AppShell {

    private static final int MAX_VISIBLE_TOASTS = 3;

    private final InventoryManager manager;
    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final Map<Page, Node> views = new EnumMap<>(Page.class);
    private InventoryController inventoryController;
    private Page currentPage;

    @FXML private StackPane root;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navDashboard;
    @FXML private ToggleButton navInventory;
    @FXML private ToggleButton navReports;
    @FXML private Label dateLabel;
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private Label versionLabel;
    @FXML private HBox searchBox;
    @FXML private TextField searchField;
    @FXML private StackPane contentHost;
    @FXML private VBox toastLayer;

    /**
     * Creates the controller. It is called by the FXML loader through a controller factory.
     *
     * @param manager the shared inventory logic that all pages use
     */
    public MainController(InventoryManager manager) {
        this.manager = manager;
    }

    /**
     * Called by the FXML loader after all {@code @FXML} fields are injected. Loads the pages, wires the
     * navigation and shows the dashboard.
     */
    @FXML
    private void initialize() {
        versionLabel.setText("Version " + AppInfo.VERSION);
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)).toUpperCase(Locale.ENGLISH));
        searchField.textProperty().bindBidirectional(searchQuery);
        searchField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (isFocused) {
                searchBox.getStyleClass().add("focused");
            } else {
                searchBox.getStyleClass().remove("focused");
            }
        });

        DashboardController dashboard = new DashboardController(manager, this);
        inventoryController = new InventoryController(manager, this);
        ReportsController reports = new ReportsController(manager);
        loadPage(Page.DASHBOARD, dashboard);
        loadPage(Page.INVENTORY, inventoryController);
        loadPage(Page.REPORTS, reports);

        navDashboard.setUserData(Page.DASHBOARD);
        navInventory.setUserData(Page.INVENTORY);
        navReports.setUserData(Page.REPORTS);
        navGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true); // one item must always stay selected
            } else {
                navigateTo((Page) newToggle.getUserData());
            }
        });

        // Typing in the search box from another page jumps to the inventory, where the results are shown.
        searchQuery.addListener((observable, oldText, newText) -> {
            if (currentPage != Page.INVENTORY && !newText.isEmpty()) {
                navigateTo(Page.INVENTORY);
            }
        });

        root.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene != null) {
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                        () -> openProductDialog(null));
                scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                        () -> searchField.requestFocus());
            }
        });

        navigateTo(Page.DASHBOARD);
    }

    private void loadPage(Page page, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Resources.fxml(page.getFxmlFile()));
            loader.setControllerFactory(type -> controller);
            views.put(page, loader.load());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + page.getFxmlFile(), e);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // AppShell
    // ------------------------------------------------------------------------------------------------

    /**
     * Switches to the given page by selecting its sidebar item; the selection listener shows the view.
     *
     * @param page the page to show
     */
    @Override
    public void navigateTo(Page page) {
        ToggleButton button = toggleFor(page);
        if (!button.isSelected()) {
            button.setSelected(true);
            return;
        }
        showPage(page);
    }

    /**
     * Opens the add/edit dialog and shows a toast with the outcome.
     *
     * @param product the product to edit, or {@code null} to add a new one
     */
    @Override
    public void openProductDialog(Product product) {
        ProductDialogController.open(root.getScene().getWindow(), manager, product)
                .ifPresent(result -> showToast(result.message(), result.type()));
    }

    /**
     * Opens the inventory page with a status filter.
     *
     * @param filter the status to show, or {@code null} for all
     */
    @Override
    public void openInventory(StockStatus filter) {
        inventoryController.setStatusFilter(filter);
        navigateTo(Page.INVENTORY);
    }

    /**
     * Returns the property behind the top-bar search field.
     *
     * @return the shared search text
     */
    @Override
    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    /**
     * Shows a toast notification that fades in, stays for a few seconds and fades out.
     *
     * @param message the text to display
     * @param type    the visual style
     */
    @Override
    public void showToast(String message, ToastType type) {
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(320);
        text.getStyleClass().add("toast-text");

        StackPane bubble = new StackPane(new Icon(type.getIconName(), 16));
        bubble.getStyleClass().add("toast-icon-bubble");

        HBox toast = new HBox(12, bubble, text);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("toast", "toast-" + type.getCssClass());
        toast.setOpacity(0);

        while (toastLayer.getChildren().size() >= MAX_VISIBLE_TOASTS) {
            toastLayer.getChildren().remove(0);
        }
        toastLayer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setToValue(1);
        PauseTransition stay = new PauseTransition(Duration.seconds(type == ToastType.ERROR ? 6 : 3.2));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(320), toast);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> toastLayer.getChildren().remove(toast));
        new SequentialTransition(fadeIn, stay, fadeOut).play();
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private void showPage(Page page) {
        currentPage = page;
        pageTitle.setText(page.getTitle());
        pageSubtitle.setText(page == Page.DASHBOARD ? greeting() + "! " + page.getSubtitle() : page.getSubtitle());

        boolean searchable = page != Page.REPORTS;
        searchBox.setVisible(searchable);
        searchBox.setManaged(searchable);

        Node view = views.get(page);
        contentHost.getChildren().setAll(view);
        view.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(160), view);
        fade.setToValue(1);
        fade.play();
    }

    private ToggleButton toggleFor(Page page) {
        return switch (page) {
            case DASHBOARD -> navDashboard;
            case INVENTORY -> navInventory;
            case REPORTS -> navReports;
        };
    }

    private static String greeting() {
        int hour = LocalDateTime.now().getHour();
        if (hour < 12) {
            return "Good morning";
        }
        return hour < 18 ? "Good afternoon" : "Good evening";
    }

    /**
     * Handles the "Add Product" button in the top bar.
     */
    @FXML
    private void handleAddProduct() {
        openProductDialog(null);
    }
}
