package com.stockly.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

/**
 * Styled confirmation dialogs for destructive actions.
 *
 * @author Umama Wahab
 */
public final class ConfirmDialog {

    /** Utility class - not meant to be instantiated. */
    private ConfirmDialog() {
    }

    /**
     * Asks the user to confirm deleting a product. The dialog blocks until the user answers.
     *
     * @param owner       the window the dialog belongs to
     * @param productName the name of the product that would be deleted
     * @return {@code true} only if the user pressed "Delete"; closing the dialog or pressing Cancel returns {@code false}
     */
    public static boolean confirmDelete(Window owner, String productName) {
        ButtonType deleteType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.NONE,
                "\"" + productName + "\" will be permanently removed from your inventory. This cannot be undone.",
                deleteType, cancelType);
        alert.initOwner(owner);
        alert.setTitle("Delete product");
        alert.setHeaderText("Delete this product?");

        StackPane bubble = new StackPane(new Icon("delete", 22));
        bubble.getStyleClass().addAll("icon-bubble", "bubble-red");
        bubble.getChildren().get(0).getStyleClass().add("icon-red");

        DialogPane pane = alert.getDialogPane();
        pane.setGraphic(bubble);
        pane.getStylesheets().add(Resources.stylesheet());
        pane.getStyleClass().add("confirm-dialog");
        ((Button) pane.lookupButton(deleteType)).getStyleClass().addAll("btn", "btn-danger");
        ((Button) pane.lookupButton(cancelType)).getStyleClass().addAll("btn", "btn-secondary");

        return alert.showAndWait().filter(button -> button == deleteType).isPresent();
    }
}
