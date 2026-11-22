package com.stockly.service;

import com.stockly.model.Activity;
import com.stockly.model.ActivityType;
import com.stockly.model.Product;
import com.stockly.model.StockStatus;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The heart of the application: all inventory business logic lives here and nowhere else.
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 *   <li>Add, update, delete and look up products (CRUD).</li>
 *   <li>Validate every input and report problems with clear messages.</li>
 *   <li>Keep an {@link ObservableList} of products so that the TableView, dashboard cards and report
 *       automatically refresh when data changes.</li>
 *   <li>Calculate statistics (totals, counts per {@link StockStatus}).</li>
 *   <li>Record a recent-activity feed.</li>
 *   <li>Save to the {@link InventoryRepository} after every change; if saving fails, the change is rolled back
 *       so memory and disk never disagree.</li>
 * </ul>
 * <p>
 * This class contains no user-interface code (no dialogs, no controls), only JavaFX <em>collections</em>.
 * </p>
 *
 * @author Umama Wahab
 */
public class InventoryManager {

    /** Maximum number of characters allowed in a product name. */
    public static final int MAX_NAME_LENGTH = 60;

    /** Maximum quantity allowed for a single product. */
    public static final int MAX_QUANTITY = 1_000_000;

    /** Number of entries kept in the recent-activity feed. */
    public static final int MAX_ACTIVITIES = 30;

    private final InventoryRepository repository;

    /** The extractor makes the list fire "update" events when a product's name or quantity changes. */
    private final ObservableList<Product> products = FXCollections.observableArrayList(
            product -> new Observable[] {product.nameProperty(), product.quantityProperty()});
    private final ObservableList<Product> readOnlyProducts = FXCollections.unmodifiableObservableList(products);

    private final ObservableList<Activity> activities = FXCollections.observableArrayList();
    private final ObservableList<Activity> readOnlyActivities = FXCollections.unmodifiableObservableList(activities);

    private int nextId = 1;

    /**
     * Creates a manager that persists its data through the given repository. The inventory starts empty;
     * call {@link #load()} to read previously saved data.
     *
     * @param repository the storage to use
     */
    public InventoryManager(InventoryRepository repository) {
        this.repository = repository;
    }

    // ------------------------------------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------------------------------------

    /**
     * Replaces the in-memory inventory with the data stored in the repository.
     * Used at start-up and by the "Refresh" button. If reading fails, the current data is left untouched.
     *
     * @throws PersistenceException if the stored data cannot be read
     */
    public void load() throws PersistenceException {
        InventoryData data = repository.load();
        products.setAll(data.products());
        activities.setAll(data.activities());
        nextId = data.nextId();
    }

    // ------------------------------------------------------------------------------------------------
    // CRUD operations
    // ------------------------------------------------------------------------------------------------

    /**
     * Adds a new product. The name is trimmed, the stock status is assigned automatically and the inventory
     * is saved.
     *
     * @param name     the product name (see {@link #validateName(String)})
     * @param quantity the initial number of units (0 to {@value #MAX_QUANTITY})
     * @return the newly created product, including its generated id
     * @throws ValidationException if the name or quantity is invalid, or another product already has this name
     * @throws PersistenceException if the inventory cannot be saved (the addition is rolled back)
     */
    public Product addProduct(String name, int quantity) throws InventoryException {
        String cleanName = validateName(name);
        validateQuantity(quantity);
        ensureNameAvailable(cleanName, -1);

        Product product = new Product(nextId, cleanName, quantity);
        int previousNextId = nextId;
        nextId++;
        products.add(product);
        Activity activity = recordActivity(ActivityType.ADDED,
                "Added \"" + cleanName + "\" with " + quantity + (quantity == 1 ? " unit" : " units"));
        try {
            save();
        } catch (PersistenceException e) {
            products.remove(product);
            activities.remove(activity);
            nextId = previousNextId;
            throw e;
        }
        return product;
    }

    /**
     * Updates the name and quantity of an existing product.
     *
     * @param id       the id of the product to update
     * @param name     the new name
     * @param quantity the new quantity
     * @return {@code true} if something changed, {@code false} if the new values equal the old ones
     * @throws ProductNotFoundException if no product has this id
     * @throws ValidationException      if the name or quantity is invalid, or another product already has this name
     * @throws PersistenceException     if the inventory cannot be saved (the change is rolled back)
     */
    public boolean updateProduct(int id, String name, int quantity) throws InventoryException {
        Product product = getProductById(id);
        String cleanName = validateName(name);
        validateQuantity(quantity);
        ensureNameAvailable(cleanName, id);

        String oldName = product.getName();
        int oldQuantity = product.getQuantity();
        boolean renamed = !oldName.equals(cleanName);
        boolean restocked = oldQuantity != quantity;
        if (!renamed && !restocked) {
            return false;
        }

        product.setName(cleanName);
        product.setQuantity(quantity);

        List<String> parts = new ArrayList<>();
        if (renamed) {
            parts.add("Renamed \"" + oldName + "\" to \"" + cleanName + "\"");
        }
        if (restocked) {
            parts.add("Changed \"" + cleanName + "\" stock from " + oldQuantity + " to " + quantity);
        }
        Activity activity = recordActivity(ActivityType.UPDATED, String.join("; ", parts));
        try {
            save();
        } catch (PersistenceException e) {
            product.setName(oldName);
            product.setQuantity(oldQuantity);
            activities.remove(activity);
            throw e;
        }
        return true;
    }

    /**
     * Deletes a product permanently.
     *
     * @param id the id of the product to delete
     * @return the product that was removed (useful for confirmation messages)
     * @throws ProductNotFoundException if no product has this id
     * @throws PersistenceException     if the inventory cannot be saved (the deletion is rolled back)
     */
    public Product deleteProduct(int id) throws InventoryException {
        Product product = getProductById(id);
        int index = products.indexOf(product);
        products.remove(index);
        Activity activity = recordActivity(ActivityType.DELETED, "Deleted \"" + product.getName() + "\"");
        try {
            save();
        } catch (PersistenceException e) {
            products.add(index, product);
            activities.remove(activity);
            throw e;
        }
        return product;
    }

    /**
     * Looks up a product by id without throwing.
     *
     * @param id the product id
     * @return the product, or an empty Optional if it does not exist
     */
    public Optional<Product> findById(int id) {
        return products.stream().filter(product -> product.getId() == id).findFirst();
    }

    /**
     * Looks up a product by id.
     *
     * @param id the product id
     * @return the matching product
     * @throws ProductNotFoundException if no product has this id
     */
    public Product getProductById(int id) throws ProductNotFoundException {
        return findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    // ------------------------------------------------------------------------------------------------
    // Querying and statistics
    // ------------------------------------------------------------------------------------------------

    /**
     * Returns the live list of all products. The list cannot be modified directly - use
     * {@link #addProduct}, {@link #updateProduct} and {@link #deleteProduct} - but it can be observed, which is
     * how the user interface stays up to date.
     *
     * @return an unmodifiable observable list of products
     */
    public ObservableList<Product> getProducts() {
        return readOnlyProducts;
    }

    /**
     * Returns the live recent-activity feed, newest entry first.
     *
     * @return an unmodifiable observable list of activities
     */
    public ObservableList<Activity> getActivities() {
        return readOnlyActivities;
    }

    /**
     * Tests whether a product passes a search text and a status filter. This single rule is used by the
     * inventory table, so the logic is not duplicated in the user interface.
     *
     * @param product the product to test
     * @param query   text that must appear in the name (case-insensitive); {@code null} or blank matches all
     * @param status  the required status, or {@code null} for any status
     * @return {@code true} if the product matches both conditions
     */
    public static boolean matches(Product product, String query, StockStatus status) {
        boolean statusMatches = status == null || product.getStatus() == status;
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean nameMatches = needle.isEmpty() || product.getName().toLowerCase(Locale.ROOT).contains(needle);
        return statusMatches && nameMatches;
    }

    /**
     * Searches the inventory.
     *
     * @param query  text that must appear in the product name (case-insensitive); blank matches all
     * @param status the required status, or {@code null} for any
     * @return the matching products in inventory order
     */
    public List<Product> search(String query, StockStatus status) {
        return products.stream().filter(product -> matches(product, query, status)).toList();
    }

    /**
     * Returns the number of different products.
     *
     * @return the product count
     */
    public int getTotalProducts() {
        return products.size();
    }

    /**
     * Returns the total number of units across all products.
     *
     * @return the sum of all quantities
     */
    public long getTotalUnits() {
        return products.stream().mapToLong(Product::getQuantity).sum();
    }

    /**
     * Counts the products that currently have the given status.
     *
     * @param status the status to count
     * @return the number of matching products
     */
    public int countByStatus(StockStatus status) {
        return (int) products.stream().filter(product -> product.getStatus() == status).count();
    }

    /**
     * Returns the products that are running low, with the emptiest first.
     *
     * @return products with status {@link StockStatus#LOW_STOCK}, sorted by quantity then name
     */
    public List<Product> getLowStockProducts() {
        return products.stream()
                .filter(product -> product.getStatus() == StockStatus.LOW_STOCK)
                .sorted(Comparator.comparingInt(Product::getQuantity).thenComparing(Product::getName))
                .toList();
    }

    // ------------------------------------------------------------------------------------------------
    // Validation (static, so the dialog can validate while the user types)
    // ------------------------------------------------------------------------------------------------

    /**
     * Validates and normalises a product name: surrounding spaces are removed and repeated inner spaces are
     * collapsed.
     *
     * @param name the raw text typed by the user
     * @return the cleaned name
     * @throws ValidationException if the name is empty or longer than {@value #MAX_NAME_LENGTH} characters
     */
    public static String validateName(String name) throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException(ValidationException.Field.NAME, "Product name cannot be empty.");
        }
        String cleaned = name.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(ValidationException.Field.NAME,
                    "Product name cannot be longer than " + MAX_NAME_LENGTH + " characters.");
        }
        return cleaned;
    }

    /**
     * Converts the text typed into the quantity field into a number, rejecting invalid input.
     * Accepted: whole numbers from 0 to {@value #MAX_QUANTITY}. Rejected: empty text, letters, decimals,
     * negative numbers and values that are too large.
     *
     * @param text the raw text typed by the user
     * @return the quantity as an int
     * @throws ValidationException with a message that explains exactly why the text was rejected
     */
    public static int parseQuantity(String text) throws ValidationException {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(ValidationException.Field.QUANTITY, "Quantity cannot be empty.");
        }
        if (!trimmed.matches("[-+]?\\d+")) {
            throw new ValidationException(ValidationException.Field.QUANTITY,
                    "Quantity must be a whole number, for example 25.");
        }
        BigInteger value = new BigInteger(trimmed);
        if (value.signum() < 0) {
            throw new ValidationException(ValidationException.Field.QUANTITY, "Quantity cannot be negative.");
        }
        if (value.compareTo(BigInteger.valueOf(MAX_QUANTITY)) > 0) {
            throw new ValidationException(ValidationException.Field.QUANTITY,
                    "Quantity cannot be more than " + String.format("%,d", MAX_QUANTITY) + ".");
        }
        return value.intValue();
    }

    /**
     * Validates a quantity that is already a number.
     *
     * @param quantity the quantity to check
     * @throws ValidationException if it is negative or above {@value #MAX_QUANTITY}
     */
    public static void validateQuantity(int quantity) throws ValidationException {
        if (quantity < 0) {
            throw new ValidationException(ValidationException.Field.QUANTITY, "Quantity cannot be negative.");
        }
        if (quantity > MAX_QUANTITY) {
            throw new ValidationException(ValidationException.Field.QUANTITY,
                    "Quantity cannot be more than " + String.format("%,d", MAX_QUANTITY) + ".");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------------------------------------

    /** Rejects a name already used by a different product (case-insensitive). */
    private void ensureNameAvailable(String name, int ignoredId) throws ValidationException {
        boolean taken = products.stream()
                .anyMatch(product -> product.getId() != ignoredId && product.getName().equalsIgnoreCase(name));
        if (taken) {
            throw new ValidationException(ValidationException.Field.NAME,
                    "A product named \"" + name + "\" already exists. Edit that product instead.");
        }
    }

    /** Adds an entry at the top of the activity feed and trims the feed to its maximum size. */
    private Activity recordActivity(ActivityType type, String message) {
        Activity activity = new Activity(type, message, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        activities.add(0, activity);
        while (activities.size() > MAX_ACTIVITIES) {
            activities.remove(activities.size() - 1);
        }
        return activity;
    }

    /** Writes the current state to the repository. */
    private void save() throws PersistenceException {
        repository.save(new InventoryData(nextId, new ArrayList<>(products), new ArrayList<>(activities)));
    }
}
