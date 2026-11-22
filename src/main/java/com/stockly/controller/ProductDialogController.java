package com.stockly.controller;

import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import com.stockly.service.InventoryException;
import com.stockly.service.InventoryManager;
import com.stockly.service.ProductNotFoundException;
import com.stockly.service.ValidationException;
import com.stockly.ui.Resources;
import com.stockly.ui.StatusBadge;
import com.stockly.ui.ToastType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Controller of the modal "Add Product" / "Edit Product" dialog.
 * <p>
 * The dialog validates the input, shows a clear message next to the field that is wrong, previews the stock
 * status that the quantity will produce, and performs the save itself so that problems reported by the
 * {@link InventoryManager} (such as a duplicate name) can be displayed inline without closing the dialog.
 * </p>
 *
 * @author Umama Wahab
 */
public class ProductDialogController {

    /**
     * The outcome of a successful save.
     *
     * @param message the text for the confirmation toast
     * @param type    the style of the toast
     */
    public record Result(String message, ToastType type) {
    }

    private final InventoryManager manager;
    private final Product editing;
    private Stage stage;
    private Result result;
    private final StatusBadge statusPreview = new StatusBadge();

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField nameField;
    @FXML private TextField quantityField;
    @FXML private Label nameError;
    @FXML private Label quantityError;
    @FXML private Label formError;
    @FXML private HBox previewHolder;
    @FXML private Button saveButton;

    private ProductDialogController(InventoryManager manager, Product editing) {
        this.manager = manager;
        this.editing = editing;
    }

    /**
     * Opens the dialog and waits until the user closes it.
     *
     * @param owner   the window the dialog is centred over
     * @param manager the inventory logic used to save
     * @param product the product to edit, or {@code null} to add a new product
     * @return the result if the product was saved, or an empty Optional if the user cancelled
     */
    public static Optional<Result> open(Window owner, InventoryManager manager, Product product) {
        try {
            ProductDialogController controller = new ProductDialogController(manager, product);
            FXMLLoader loader = new FXMLLoader(Resources.fxml("ProductDialog.fxml"));
            loader.setControllerFactory(type -> controller);
            Parent root = loader.load();

            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.getStylesheets().add(Resources.stylesheet());
            stage.setScene(scene);
            controller.stage = stage;
            stage.setOnShown(event -> {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 3);
                controller.nameField.requestFocus();
            });
            stage.showAndWait();
            return Optional.ofNullable(controller.result);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open the product dialog", e);
        }
    }

    /** Called by the FXML loader: fills the fields when editing and wires the live validation. */
    @FXML
    private void initialize() {
        previewHolder.getChildren().add(statusPreview);
        if (editing == null) {
            titleLabel.setText("Add Product");
            subtitleLabel.setText("Enter the product name and quantity.");
        } else {
            titleLabel.setText("Edit Product");
            subtitleLabel.setText("Update the details of " + editing.getDisplayId() + ".");
            nameField.setText(editing.getName());
            quantityField.setText(String.valueOf(editing.getQuantity()));
        }

        nameField.textProperty().addListener((observable, oldText, newText) -> clearError(nameError, nameField));
        quantityField.textProperty().addListener((observable, oldText, newText) -> {
            clearError(quantityError, quantityField);
            updateStatusPreview();
        });
        updateStatusPreview();
    }

    /** Shows the status the typed quantity would produce, or a dash while the quantity is not valid. */
    private void updateStatusPreview() {
        try {
            statusPreview.setStatus(StockStatus.fromQuantity(InventoryManager.parseQuantity(quantityField.getText())));
        } catch (ValidationException e) {
            statusPreview.setStatus(null);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Button handlers
    // ------------------------------------------------------------------------------------------------

    /** Handles "Save Product": validates, saves and closes the dialog, or shows what is wrong. */
    @FXML
    private void handleSave() {
        clearError(nameError, nameField);
        clearError(quantityError, quantityField);
        formError.setVisible(false);
        formError.setManaged(false);

        String name = null;
        int quantity = 0;
        boolean valid = true;
        try {
            name = InventoryManager.validateName(nameField.getText());
        } catch (ValidationException e) {
            showError(e);
            valid = false;
        }
        try {
            quantity = InventoryManager.parseQuantity(quantityField.getText());
        } catch (ValidationException e) {
            showError(e);
            valid = false;
        }
        if (!valid) {
            (nameError.isVisible() ? nameField : quantityField).requestFocus();
            return;
        }

        try {
            if (editing == null) {
                Product added = manager.addProduct(name, quantity);
                result = new Result("\"" + added.getName() + "\" was added to your inventory.", ToastType.SUCCESS);
            } else if (manager.updateProduct(editing.getId(), name, quantity)) {
                result = new Result("\"" + name + "\" was updated.", ToastType.SUCCESS);
            } else {
                result = new Result("No changes to save.", ToastType.INFO);
            }
            stage.close();
        } catch (ValidationException e) {
            showError(e);
            (nameError.isVisible() ? nameField : quantityField).requestFocus();
        } catch (ProductNotFoundException e) {
            showFormError(e.getMessage() + " Close this window and refresh the inventory.");
            saveButton.setDisable(true);
        } catch (InventoryException e) {
            showFormError(e.getMessage());
        }
    }

    /** Handles "Cancel" and the close button: closes without saving. */
    @FXML
    private void handleCancel() {
        stage.close();
    }

    // ------------------------------------------------------------------------------------------------
    // Error display
    // ------------------------------------------------------------------------------------------------

    private void showError(ValidationException e) {
        switch (e.getField()) {
            case NAME -> showFieldError(nameError, nameField, e.getMessage());
            case QUANTITY -> showFieldError(quantityError, quantityField, e.getMessage());
            default -> showFormError(e.getMessage());
        }
    }

    private void showFieldError(Label label, TextField field, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
        if (!field.getStyleClass().contains("error")) {
            field.getStyleClass().add("error");
        }
    }

    private void clearError(Label label, TextField field) {
        label.setVisible(false);
        label.setManaged(false);
        field.getStyleClass().remove("error");
    }

    private void showFormError(String message) {
        formError.setText(message);
        formError.setVisible(true);
        formError.setManaged(true);
    }
}
