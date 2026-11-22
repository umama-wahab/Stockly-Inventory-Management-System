package com.stockly;

import com.stockly.controller.MainController;
import com.stockly.persistence.JsonInventoryRepository;
import com.stockly.service.InventoryManager;
import com.stockly.ui.Resources;
import com.stockly.ui.ToastType;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The JavaFX {@link Application} subclass. It wires the application together:
 * it creates the persistence layer, the business logic ({@link InventoryManager}), loads the main FXML
 * view and shows the primary window.
 *
 * @author Umama Wahab
 */
public class InventoryApplication extends Application {

    /** Default window width in pixels. */
    private static final double WINDOW_WIDTH = 1280;

    /** Default window height in pixels. */
    private static final double WINDOW_HEIGHT = 820;

    /** Creates the application object. The JavaFX runtime calls this; it does nothing by itself. */
    public InventoryApplication() {
        super();
    }

    /**
     * Builds the object graph and shows the main window.
     * <p>
     * If the saved inventory file cannot be read, the application still starts with an empty inventory and
     * shows the reason in an error notification instead of crashing.
     * </p>
     *
     * @param stage the primary stage supplied by the JavaFX runtime
     * @throws IOException if the main FXML view cannot be loaded (a packaging error)
     */
    @Override
    public void start(Stage stage) throws IOException {
        InventoryManager manager = new InventoryManager(new JsonInventoryRepository(JsonInventoryRepository.defaultFile()));

        String startupProblem = null;
        try {
            manager.load();
        } catch (com.stockly.service.PersistenceException e) {
            startupProblem = e.getMessage();
        }

        FXMLLoader loader = new FXMLLoader(Resources.fxml("MainView.fxml"));
        loader.setControllerFactory(type -> new MainController(manager));
        Parent root = loader.load();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(Resources.stylesheet());

        stage.setTitle(AppInfo.NAME + " - " + AppInfo.TAGLINE);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        if (startupProblem != null) {
            MainController controller = loader.getController();
            controller.showToast(startupProblem, ToastType.ERROR);
        }
    }
}
