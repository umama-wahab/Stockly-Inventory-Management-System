package com.stockly.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockly.service.InventoryData;
import com.stockly.service.InventoryException;
import com.stockly.service.InventoryManager;
import com.stockly.service.PersistenceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests saving and loading the JSON file (test case 14: data survives a restart). */
class JsonInventoryRepositoryTest {

    @TempDir
    Path folder;

    @Test
    void missingFileGivesEmptyInventory() throws PersistenceException {
        InventoryData data = new JsonInventoryRepository(folder.resolve("none.json")).load();
        assertTrue(data.products().isEmpty());
        assertEquals(1, data.nextId());
    }

    @Test
    void roundTripKeepsEverythingIncludingSpecialCharacters() throws InventoryException {
        Path file = folder.resolve("nested").resolve("inventory.json");
        InventoryManager first = new InventoryManager(new JsonInventoryRepository(file));
        first.addProduct("Monitor 27\" \\ \u00e9 \u4e2d", 12);
        first.addProduct("Pen", 0);
        first.deleteProduct(2);

        InventoryManager second = new InventoryManager(new JsonInventoryRepository(file));
        second.load();
        assertEquals(1, second.getTotalProducts());
        assertEquals("Monitor 27\" \\ \u00e9 \u4e2d", second.getProducts().get(0).getName());
        assertEquals(3, second.addProduct("New", 1).getId());
        assertEquals(4, second.getActivities().size());
        assertFalse(Files.exists(file.resolveSibling("inventory.json.tmp")));
    }

    @Test
    void corruptFileIsBackedUpAndReported() throws IOException {
        Path file = folder.resolve("inventory.json");
        Files.writeString(file, "{ this is not json");
        PersistenceException e = assertThrows(PersistenceException.class,
                () -> new JsonInventoryRepository(file).load());
        assertTrue(e.getMessage().contains("damaged"));
        assertFalse(Files.exists(file));
        try (Stream<Path> files = Files.list(folder)) {
            assertEquals(1, files.filter(p -> p.getFileName().toString().contains(".corrupt-")).count());
        }
    }

    @Test
    void invalidDataIsTreatedAsCorrupt() throws IOException {
        Path file = folder.resolve("inventory.json");
        Files.writeString(file, "{\"products\":[{\"id\":1,\"name\":\"Pen\",\"quantity\":-4}]}");
        assertThrows(PersistenceException.class, () -> new JsonInventoryRepository(file).load());
    }

    @Test
    void jsonParserHandlesBasicTypes() {
        Object parsed = SimpleJson.parse("{\"a\":[1,2.5,true,null,\"x\\n\\u0041\"],\"b\":{}}");
        assertEquals("{\n  \"a\": [\n    1,\n    2.5,\n    true,\n    null,\n    \"x\\nA\"\n  ],\n  \"b\": {}\n}\n",
                SimpleJson.toJson(parsed));
        assertThrows(IllegalArgumentException.class, () -> SimpleJson.parse("[1,]"));
    }
}
