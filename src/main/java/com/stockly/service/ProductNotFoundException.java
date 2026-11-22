package com.stockly.service;

import com.stockly.model.Product;

/**
 * Thrown when an operation refers to a product id that does not exist (any more), for example when
 * updating or deleting a product that was already removed.
 *
 * @author Umama Wahab
 */
public class ProductNotFoundException extends InventoryException {

    private static final long serialVersionUID = 1L;

    /** The id that was looked up but not found. */
    private final int productId;

    /**
     * Creates the exception for a missing product.
     *
     * @param productId the id that could not be found
     */
    public ProductNotFoundException(int productId) {
        super("Product " + Product.formatId(productId) + " no longer exists in the inventory.");
        this.productId = productId;
    }

    /**
     * Returns the id that was not found.
     *
     * @return the missing product id
     */
    public int getProductId() {
        return productId;
    }
}
