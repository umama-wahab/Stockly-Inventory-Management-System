package com.stockly.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A friendly "nothing here yet" panel with an icon, a title, a short explanation and an optional button.
 * It is used when the inventory is empty or when a search finds nothing.
 *
 * @author Umama Wahab
 */
public final class EmptyState extends VBox {

    private final Label title = new Label();
    private final Label message = new Label();
    private final Button action = new Button();
    private Runnable actionHandler = () -> { };

    /**
     * Creates the panel.
     *
     * @param iconName   the {@link Icon} to show
     * @param title      the headline
     * @param message    the explanation under the headline
     * @param buttonText the label of the button, or {@code null} for no button
     * @param onAction   what happens when the button is pressed (ignored if there is no button)
     */
    public EmptyState(String iconName, String title, String message, String buttonText, Runnable onAction) {
        getStyleClass().add("empty-state");
        setAlignment(Pos.CENTER);
        setSpacing(8);

        StackPane bubble = new StackPane(new Icon(iconName, 30));
        bubble.getStyleClass().addAll("empty-icon-bubble");

        this.title.getStyleClass().add("empty-title");
        this.message.getStyleClass().add("empty-message");
        this.message.setWrapText(true);
        this.message.setMaxWidth(380);
        this.message.setAlignment(Pos.CENTER);
        this.message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        action.getStyleClass().addAll("btn", "btn-primary");
        action.setOnAction(event -> actionHandler.run());
        VBox.setMargin(action, new javafx.geometry.Insets(10, 0, 0, 0));

        getChildren().addAll(bubble, this.title, this.message, action);
        update(title, message, buttonText, onAction);
    }

    /**
     * Changes everything the panel says, so one instance can be reused for different situations.
     *
     * @param newTitle      the headline
     * @param newMessage    the explanation
     * @param newButtonText the button label, or {@code null} to hide the button
     * @param onAction      the button handler
     */
    public final void update(String newTitle, String newMessage, String newButtonText, Runnable onAction) {
        title.setText(newTitle);
        message.setText(newMessage);
        actionHandler = onAction == null ? () -> { } : onAction;
        boolean hasButton = newButtonText != null;
        action.setText(newButtonText == null ? "" : newButtonText);
        action.setVisible(hasButton);
        action.setManaged(hasButton);
    }
}
