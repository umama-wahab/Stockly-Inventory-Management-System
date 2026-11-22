package com.stockly.model;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

/**
 * A single product in the inventory.
 * <p>
 * The name and quantity are JavaFX properties so that a {@code TableView} or a dashboard card bound to this
 * object refreshes automatically when a value changes. The {@linkplain #getStatus() stock status} is
 * <strong>read-only</strong>: it is recalculated whenever the quantity changes, so it can never disagree with
 * the quantity.
 * </p>
 * <p>
 * The properties are exposed as read-only. All modifications must go through {@link #setName(String)} and
 * {@link #setQuantity(int)}, which protect the object's invariants (non-blank name, non-negative quantity).
 * </p>
 *
 * @author Umama Wahab
 */
public class Product {

    private final int id;
    private final ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
    private final ReadOnlyIntegerWrapper quantity = new ReadOnlyIntegerWrapper();
    private final ReadOnlyObjectWrapper<StockStatus> status = new ReadOnlyObjectWrapper<>();

    /**
     * Creates a product.
     *
     * @param id       the unique, positive identifier of the product
     * @param name     the product name; must not be blank
     * @param quantity the number of units in stock; must not be negative
     * @throws IllegalArgumentException if the id is not positive, the name is blank or the quantity is negative
     */
    public Product(int id, String name, int quantity) {
        if (id <= 0) {
            throw new IllegalArgumentException("Product id must be positive: " + id);
        }
        this.id = id;
        this.name.set(requireName(name));
        this.quantity.set(requireQuantity(quantity));
        this.status.set(StockStatus.fromQuantity(quantity));
        // Whenever the quantity changes, the status is recalculated automatically.
        this.quantity.addListener((observable, oldValue, newValue) ->
                this.status.set(StockStatus.fromQuantity(newValue.intValue())));
    }

    /**
     * Formats a numeric id for display, for example {@code 7} becomes {@code "P-007"}.
     *
     * @param id the numeric product id
     * @return the formatted id
     */
    public static String formatId(int id) {
        return String.format("P-%03d", id);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank.");
        }
        return name.trim();
    }

    private static int requireQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative: " + quantity);
        }
        return quantity;
    }

    /**
     * Returns the unique numeric id.
     *
     * @return the product id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the id formatted for display, such as {@code "P-007"}.
     *
     * @return the display id
     */
    public String getDisplayId() {
        return formatId(id);
    }

    /**
     * Returns the product name.
     *
     * @return the name (never blank)
     */
    public String getName() {
        return name.get();
    }

    /**
     * Changes the product name.
     *
     * @param newName the new name; must not be blank
     * @throws IllegalArgumentException if the name is blank
     */
    public void setName(String newName) {
        name.set(requireName(newName));
    }

    /**
     * Returns the observable name property, for data binding.
     *
     * @return a read-only name property
     */
    public ReadOnlyStringProperty nameProperty() {
        return name.getReadOnlyProperty();
    }

    /**
     * Returns the number of units in stock.
     *
     * @return the quantity (never negative)
     */
    public int getQuantity() {
        return quantity.get();
    }

    /**
     * Changes the quantity. The {@linkplain #getStatus() stock status} updates automatically.
     *
     * @param newQuantity the new number of units; must not be negative
     * @throws IllegalArgumentException if the quantity is negative
     */
    public void setQuantity(int newQuantity) {
        quantity.set(requireQuantity(newQuantity));
    }

    /**
     * Returns the observable quantity property, for data binding.
     *
     * @return a read-only quantity property
     */
    public ReadOnlyIntegerProperty quantityProperty() {
        return quantity.getReadOnlyProperty();
    }

    /**
     * Returns the stock status derived from the current quantity.
     *
     * @return the current status
     */
    public StockStatus getStatus() {
        return status.get();
    }

    /**
     * Returns the observable status property, for data binding.
     *
     * @return a read-only status property
     */
    public ReadOnlyObjectProperty<StockStatus> statusProperty() {
        return status.getReadOnlyProperty();
    }

    /**
     * Returns a short description useful for debugging and log messages.
     *
     * @return the id, name and quantity as text
     */
    @Override
    public String toString() {
        return getDisplayId() + " " + getName() + " (" + getQuantity() + ")";
    }
}
