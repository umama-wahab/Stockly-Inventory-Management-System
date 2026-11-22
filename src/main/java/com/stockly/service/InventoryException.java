package com.stockly.service;

/**
 * Base class of all checked, business-level errors raised by the inventory layer.
 * The message of every subclass is written to be shown directly to the user.
 *
 * @author Umama Wahab
 */
public class InventoryException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-friendly message.
     *
     * @param message the description of what went wrong
     */
    public InventoryException(String message) {
        super(message);
    }

    /**
     * Creates an exception that wraps a lower-level cause.
     *
     * @param message the description of what went wrong
     * @param cause   the underlying exception
     */
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
