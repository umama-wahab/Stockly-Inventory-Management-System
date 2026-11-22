package com.stockly.service;

/**
 * Abstraction of the permanent storage used by {@link InventoryManager}.
 * <p>
 * The business logic only knows this interface, so the storage technology (JSON file, SQLite, ...) can be
 * replaced without touching the manager or the user interface. Unit tests use a simple in-memory
 * implementation.
 * </p>
 *
 * @author Umama Wahab
 */
public interface InventoryRepository {

    /**
     * Reads the stored inventory.
     *
     * @return the stored data, or an {@linkplain InventoryData#empty() empty snapshot} if nothing was saved yet
     * @throws PersistenceException if the storage exists but cannot be read
     */
    InventoryData load() throws PersistenceException;

    /**
     * Replaces the stored inventory with the given snapshot.
     *
     * @param data the data to store
     * @throws PersistenceException if the data cannot be written
     */
    void save(InventoryData data) throws PersistenceException;
}
