package com.stockly.service;

/**
 * A fake repository for unit tests: it keeps the data in memory and can be told to fail.
 *
 * @author Umama Wahab
 */
class InMemoryRepository implements InventoryRepository {

    private InventoryData stored = InventoryData.empty();
    private boolean failOnSave;
    private int saveCount;

    void setFailOnSave(boolean failOnSave) {
        this.failOnSave = failOnSave;
    }

    int getSaveCount() {
        return saveCount;
    }

    @Override
    public InventoryData load() {
        return stored;
    }

    @Override
    public void save(InventoryData data) throws PersistenceException {
        if (failOnSave) {
            throw new PersistenceException("disk full", new java.io.IOException("simulated"));
        }
        stored = data;
        saveCount++;
    }
}
