package com.stockly.service;

/**
 * Thrown when user input is rejected, for example an empty name or a negative quantity.
 * It records <em>which</em> input field was wrong so the dialog can show the message next to that field.
 *
 * @author Umama Wahab
 */
public class ValidationException extends InventoryException {

    private static final long serialVersionUID = 1L;

    /** The input field a validation message belongs to. */
    public enum Field {
        /** The product name field. */
        NAME,
        /** The quantity field. */
        QUANTITY,
        /** Not tied to one particular field. */
        GENERAL
    }

    /** The input field the message belongs to. */
    private final Field field;

    /**
     * Creates a validation error.
     *
     * @param field   the input field that failed validation
     * @param message the user-friendly explanation
     */
    public ValidationException(Field field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Returns the field that failed validation.
     *
     * @return the offending field
     */
    public Field getField() {
        return field;
    }
}
