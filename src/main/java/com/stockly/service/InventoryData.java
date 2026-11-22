package com.stockly.service;

import com.stockly.model.Activity;
import com.stockly.model.Product;
import java.util.List;

/**
 * A plain snapshot of everything that must survive an application restart.
 *
 * @param nextId     the id that will be given to the next new product (ids are never reused)
 * @param products   all products
 * @param activities the recent-activity feed, newest first
 * @author Umama Wahab
 */
public record InventoryData(int nextId, List<Product> products, List<Activity> activities) {

    /**
     * Creates the snapshot of a brand-new, empty inventory.
     *
     * @return an empty snapshot whose first product will get id 1
     */
    public static InventoryData empty() {
        return new InventoryData(1, List.of(), List.of());
    }
}
