package com.stockly.model;

/**
 * The stock level of a product. The status is never typed in by the user - it is always derived from the
 * quantity using {@link #fromQuantity(int)}, which guarantees it can never be inconsistent.
 *
 * <ul>
 *   <li>quantity = 0 &rarr; {@link #OUT_OF_STOCK}</li>
 *   <li>quantity 1 to 10 &rarr; {@link #LOW_STOCK}</li>
 *   <li>quantity &gt; 10 &rarr; {@link #IN_STOCK}</li>
 * </ul>
 *
 * @author Umama Wahab
 */
public enum StockStatus {

    /** More than {@value #LOW_STOCK_LIMIT} units available. */
    IN_STOCK("In Stock", "in-stock"),

    /** Between 1 and {@value #LOW_STOCK_LIMIT} units available - time to reorder. */
    LOW_STOCK("Low Stock", "low-stock"),

    /** No units available. */
    OUT_OF_STOCK("Out of Stock", "out-of-stock");

    /** The highest quantity that is still considered "low stock". */
    public static final int LOW_STOCK_LIMIT = 10;

    private final String label;
    private final String cssClass;

    StockStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    /**
     * Applies the stock status business rule to a quantity.
     *
     * @param quantity the number of units in stock
     * @return {@link #OUT_OF_STOCK} for 0, {@link #LOW_STOCK} for 1-10 and {@link #IN_STOCK} for 11 or more
     * @throws IllegalArgumentException if {@code quantity} is negative
     */
    public static StockStatus fromQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        if (quantity == 0) {
            return OUT_OF_STOCK;
        }
        if (quantity <= LOW_STOCK_LIMIT) {
            return LOW_STOCK;
        }
        return IN_STOCK;
    }

    /**
     * Returns the human readable name, for example "Low Stock".
     *
     * @return the display label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the suffix used to build CSS style classes such as {@code badge-low-stock}.
     *
     * @return a lower-case, hyphenated identifier
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * Returns the display label so the status prints nicely in lists and combo boxes.
     *
     * @return the display label
     */
    @Override
    public String toString() {
        return label;
    }
}
