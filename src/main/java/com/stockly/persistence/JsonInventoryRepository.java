package com.stockly.persistence;

import com.stockly.model.Activity;
import com.stockly.model.ActivityType;
import com.stockly.model.Product;
import com.stockly.service.InventoryData;
import com.stockly.service.InventoryRepository;
import com.stockly.service.PersistenceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the inventory in a single human-readable JSON file.
 * <p>
 * Why JSON? It needs no database server and no external library, the file can be opened in any text editor
 * (handy during a viva demonstration), and it is perfectly adequate for a small retail store.
 * </p>
 * <p>
 * Safety features:
 * </p>
 * <ul>
 *   <li><strong>Atomic saves</strong> - data is written to a temporary file first and then moved over the real
 *       file, so a crash in the middle of saving can never leave a half-written inventory.</li>
 *   <li><strong>Corruption handling</strong> - if the file cannot be parsed, it is renamed to
 *       {@code inventory.json.corrupt-...} instead of being overwritten, and the user is informed.</li>
 * </ul>
 *
 * @author Umama Wahab
 */
public class JsonInventoryRepository implements InventoryRepository {

    private static final int FORMAT_VERSION = 1;

    private final Path file;

    /**
     * Creates a repository that reads and writes the given file. The file and its parent folders are created
     * on the first save.
     *
     * @param file the location of the JSON file
     */
    public JsonInventoryRepository(Path file) {
        this.file = file;
    }

    /**
     * Returns the default data file: {@code <user home>/.stockly/inventory.json}. It can be overridden with the
     * JVM option {@code -Dstockly.data=/path/to/file.json}.
     *
     * @return the path of the data file
     */
    public static Path defaultFile() {
        String override = System.getProperty("stockly.data");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".stockly", "inventory.json");
    }

    /**
     * Returns the file this repository uses.
     *
     * @return the JSON file path
     */
    public Path getFile() {
        return file;
    }

    /**
     * Reads the inventory file.
     *
     * @return the stored data, or an empty inventory if the file does not exist yet
     * @throws PersistenceException if the file cannot be read, or its content is invalid (it is then backed up)
     */
    @Override
    public InventoryData load() throws PersistenceException {
        if (!Files.exists(file)) {
            return InventoryData.empty();
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("The inventory file could not be read: " + file, e);
        }
        if (text.isBlank()) {
            return InventoryData.empty();
        }
        try {
            return parse(text);
        } catch (RuntimeException e) {
            Path backup = backUpCorruptFile();
            throw new PersistenceException("The saved inventory file was damaged and could not be read. "
                    + "It was kept as \"" + backup.getFileName() + "\" and Stockly started with an empty inventory.", e);
        }
    }

    /**
     * Writes the inventory to the file, replacing the previous content atomically.
     *
     * @param data the data to store
     * @throws PersistenceException if the file cannot be written
     */
    @Override
    public void save(InventoryData data) throws PersistenceException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", FORMAT_VERSION);
        root.put("nextId", data.nextId());

        List<Object> productList = new ArrayList<>();
        for (Product product : data.products()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", product.getId());
            entry.put("name", product.getName());
            entry.put("quantity", product.getQuantity());
            productList.add(entry);
        }
        root.put("products", productList);

        List<Object> activityList = new ArrayList<>();
        for (Activity activity : data.activities()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", activity.type().name());
            entry.put("message", activity.message());
            entry.put("timestamp", activity.timestamp().toString());
            activityList.add(entry);
        }
        root.put("activities", activityList);

        String json = SimpleJson.toJson(root);
        try {
            Path directory = file.toAbsolutePath().getParent();
            if (directory != null) {
                Files.createDirectories(directory);
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenceException("Your changes could not be saved to " + file
                    + ". Check that the folder is writable.", e);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------------------------------------

    private InventoryData parse(String text) {
        Map<String, Object> root = asMap(SimpleJson.parse(text));

        List<Product> products = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();
        int highestId = 0;
        for (Object item : asList(root.get("products"))) {
            Map<String, Object> entry = asMap(item);
            int id = asInt(entry.get("id"));
            if (!usedIds.add(id)) {
                throw new IllegalArgumentException("Duplicate product id " + id);
            }
            products.add(new Product(id, asString(entry.get("name")), asInt(entry.get("quantity"))));
            highestId = Math.max(highestId, id);
        }

        List<Activity> activities = new ArrayList<>();
        for (Object item : asList(root.get("activities"))) {
            Map<String, Object> entry = asMap(item);
            activities.add(new Activity(
                    ActivityType.valueOf(asString(entry.get("type"))),
                    asString(entry.get("message")),
                    LocalDateTime.parse(asString(entry.get("timestamp")))));
        }

        int nextId = root.get("nextId") == null ? 1 : asInt(root.get("nextId"));
        return new InventoryData(Math.max(nextId, highestId + 1), products, activities);
    }

    /** Renames a damaged file so it is never overwritten. Returns the new name (or the old one if renaming failed). */
    private Path backUpCorruptFile() {
        Path backup = file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } catch (IOException e) {
            return file;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException("Expected a JSON object");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List) {
            return (List<Object>) value;
        }
        throw new IllegalArgumentException("Expected a JSON array");
    }

    private static int asInt(Object value) {
        if (value instanceof Long number && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected a whole number");
    }

    private static String asString(Object value) {
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("Expected text");
    }
}
