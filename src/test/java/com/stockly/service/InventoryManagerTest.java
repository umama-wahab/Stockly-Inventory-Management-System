package com.stockly.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockly.model.ActivityType;
import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the business logic of {@link InventoryManager} without any user interface. */
class InventoryManagerTest {

    private InMemoryRepository repository;
    private InventoryManager manager;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        manager = new InventoryManager(repository);
    }

    // ---- Add ----

    @Test
    void addValidProduct() throws InventoryException {
        Product product = manager.addProduct("  Wireless   Mouse ", 25);
        assertEquals("Wireless Mouse", product.getName());
        assertEquals(25, product.getQuantity());
        assertEquals(StockStatus.IN_STOCK, product.getStatus());
        assertEquals(1, manager.getTotalProducts());
        assertEquals(1, repository.getSaveCount());
        assertEquals(ActivityType.ADDED, manager.getActivities().get(0).type());
    }

    @Test
    void idsAreUniqueAndNeverReused() throws InventoryException {
        Product first = manager.addProduct("A", 1);
        manager.deleteProduct(first.getId());
        Product second = manager.addProduct("B", 1);
        assertEquals(2, second.getId());
    }

    @Test
    void addWithEmptyNameIsRejected() {
        ValidationException e = assertThrows(ValidationException.class, () -> manager.addProduct("   ", 5));
        assertEquals(ValidationException.Field.NAME, e.getField());
        assertEquals(0, manager.getTotalProducts());
    }

    @Test
    void addWithNegativeQuantityIsRejected() {
        ValidationException e = assertThrows(ValidationException.class, () -> manager.addProduct("Pen", -1));
        assertEquals(ValidationException.Field.QUANTITY, e.getField());
    }

    @Test
    void duplicateNameIsRejectedIgnoringCase() throws InventoryException {
        manager.addProduct("Pen", 5);
        assertThrows(ValidationException.class, () -> manager.addProduct("pen", 3));
    }

    // ---- Quantity parsing ----

    @Test
    void parseQuantityAcceptsWholeNumbers() throws ValidationException {
        assertEquals(0, InventoryManager.parseQuantity("0"));
        assertEquals(42, InventoryManager.parseQuantity(" 42 "));
    }

    @Test
    void parseQuantityRejectsBadInput() {
        for (String bad : List.of("", "   ", "abc", "5.5", "12abc", "1e3", "-5", "99999999999999999999")) {
            assertThrows(ValidationException.class, () -> InventoryManager.parseQuantity(bad), "input: " + bad);
        }
        assertThrows(ValidationException.class, () -> InventoryManager.parseQuantity(null));
    }

    @Test
    void parseQuantityGivesSpecificMessages() {
        assertEquals("Quantity cannot be empty.",
                assertThrows(ValidationException.class, () -> InventoryManager.parseQuantity("")).getMessage());
        assertEquals("Quantity cannot be negative.",
                assertThrows(ValidationException.class, () -> InventoryManager.parseQuantity("-3")).getMessage());
        assertTrue(assertThrows(ValidationException.class, () -> InventoryManager.parseQuantity("abc"))
                .getMessage().contains("whole number"));
    }

    // ---- View ----

    @Test
    void emptyInventoryHasNoProducts() {
        assertTrue(manager.getProducts().isEmpty());
        assertEquals(0, manager.getTotalUnits());
    }

    @Test
    void statisticsAreCalculatedFromData() throws InventoryException {
        manager.addProduct("A", 50);
        manager.addProduct("B", 5);
        manager.addProduct("C", 0);
        assertEquals(3, manager.getTotalProducts());
        assertEquals(55, manager.getTotalUnits());
        assertEquals(1, manager.countByStatus(StockStatus.IN_STOCK));
        assertEquals(1, manager.countByStatus(StockStatus.LOW_STOCK));
        assertEquals(1, manager.countByStatus(StockStatus.OUT_OF_STOCK));
        assertEquals(List.of("B"), manager.getLowStockProducts().stream().map(Product::getName).toList());
    }

    // ---- Update ----

    @Test
    void updateExistingProduct() throws InventoryException {
        Product product = manager.addProduct("Pen", 50);
        assertTrue(manager.updateProduct(product.getId(), "Blue Pen", 3));
        assertEquals("Blue Pen", product.getName());
        assertEquals(StockStatus.LOW_STOCK, product.getStatus());
        assertEquals(ActivityType.UPDATED, manager.getActivities().get(0).type());
    }

    @Test
    void updateWithSameValuesReportsNoChange() throws InventoryException {
        Product product = manager.addProduct("Pen", 50);
        assertFalse(manager.updateProduct(product.getId(), "Pen", 50));
    }

    @Test
    void updateNonExistingProductFails() {
        assertThrows(ProductNotFoundException.class, () -> manager.updateProduct(99, "X", 1));
    }

    @Test
    void updateCanKeepOwnNameButNotTakeAnothers() throws InventoryException {
        Product a = manager.addProduct("A", 1);
        manager.addProduct("B", 1);
        assertTrue(manager.updateProduct(a.getId(), "A", 7));
        assertThrows(ValidationException.class, () -> manager.updateProduct(a.getId(), "b", 7));
    }

    // ---- Delete ----

    @Test
    void deleteExistingProduct() throws InventoryException {
        Product product = manager.addProduct("Pen", 5);
        manager.deleteProduct(product.getId());
        assertTrue(manager.findById(product.getId()).isEmpty());
        assertEquals(ActivityType.DELETED, manager.getActivities().get(0).type());
    }

    @Test
    void deleteNonExistingProductFails() {
        assertThrows(ProductNotFoundException.class, () -> manager.deleteProduct(99));
    }

    // ---- Search and filter ----

    @Test
    void searchAndFilter() throws InventoryException {
        manager.addProduct("Wireless Mouse", 50);
        manager.addProduct("Wired Mouse", 4);
        manager.addProduct("Keyboard", 0);
        assertEquals(2, manager.search("mouse", null).size());
        assertEquals(1, manager.search("MOUSE", StockStatus.LOW_STOCK).size());
        assertEquals(1, manager.search("", StockStatus.OUT_OF_STOCK).size());
        assertEquals(3, manager.search(null, null).size());
        assertEquals(0, manager.search("laptop", null).size());
    }

    // ---- Persistence behaviour ----

    @Test
    void failedSaveRollsBackAdd() {
        repository.setFailOnSave(true);
        assertThrows(PersistenceException.class, () -> manager.addProduct("Pen", 5));
        assertEquals(0, manager.getTotalProducts());
        assertTrue(manager.getActivities().isEmpty());
    }

    @Test
    void failedSaveRollsBackUpdateAndDelete() throws InventoryException {
        Product product = manager.addProduct("Pen", 5);
        repository.setFailOnSave(true);
        assertThrows(PersistenceException.class, () -> manager.updateProduct(product.getId(), "Ink", 9));
        assertEquals("Pen", product.getName());
        assertEquals(5, product.getQuantity());
        assertThrows(PersistenceException.class, () -> manager.deleteProduct(product.getId()));
        assertEquals(1, manager.getTotalProducts());
    }

    @Test
    void dataSurvivesRestart() throws InventoryException {
        manager.addProduct("Pen", 5);
        manager.addProduct("Ink", 0);

        InventoryManager restarted = new InventoryManager(repository);
        restarted.load();
        assertEquals(2, restarted.getTotalProducts());
        assertEquals(3, restarted.addProduct("Paper", 1).getId());
    }

    @Test
    void activityFeedIsLimited() throws InventoryException {
        for (int i = 0; i < InventoryManager.MAX_ACTIVITIES + 10; i++) {
            manager.addProduct("P" + i, 1);
        }
        assertEquals(InventoryManager.MAX_ACTIVITIES, manager.getActivities().size());
    }
}
