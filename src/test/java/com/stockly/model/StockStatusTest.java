package com.stockly.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests the stock status business rule (test case 13 of the test plan). */
class StockStatusTest {

    @Test
    void quantityZeroIsOutOfStock() {
        assertEquals(StockStatus.OUT_OF_STOCK, StockStatus.fromQuantity(0));
    }

    @Test
    void quantityOneIsLowStock() {
        assertEquals(StockStatus.LOW_STOCK, StockStatus.fromQuantity(1));
    }

    @Test
    void quantityTenIsLowStock() {
        assertEquals(StockStatus.LOW_STOCK, StockStatus.fromQuantity(10));
    }

    @Test
    void quantityElevenIsInStock() {
        assertEquals(StockStatus.IN_STOCK, StockStatus.fromQuantity(11));
    }

    @Test
    void negativeQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> StockStatus.fromQuantity(-1));
    }

    @Test
    void productStatusFollowsQuantityChanges() {
        Product product = new Product(1, "Pen", 50);
        assertEquals(StockStatus.IN_STOCK, product.getStatus());
        product.setQuantity(10);
        assertEquals(StockStatus.LOW_STOCK, product.getStatus());
        product.setQuantity(0);
        assertEquals(StockStatus.OUT_OF_STOCK, product.getStatus());
    }
}
