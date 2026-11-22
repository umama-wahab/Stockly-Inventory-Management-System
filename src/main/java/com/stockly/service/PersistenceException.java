package com.stockly.service;

/**
 * Thrown when inventory data cannot be loaded from or saved to permanent storage.
 *
 * @author Umama Wahab
 */
public class PersistenceException extends InventoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a persistence error with a cause.
     *
     * @param message a user-friendly explanation
     * @param cause   the underlying I/O or parsing problem
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
