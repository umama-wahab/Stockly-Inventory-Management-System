# Test cases

Run the application (`mvn clean javafx:run`) and execute each test by hand. Fill in **Actual Result** and **Pass/Fail**.
Tip: start with a clean inventory (delete `~/.stockly/inventory.json`) before TC-01, and take a screenshot of each result for your report.

Automated unit tests for the same rules are in `src/test/java` (run `mvn test`); they cover TC-01 to TC-03, TC-05 to TC-10, TC-13 and TC-14 at business-logic level, but you still need to run the manual steps on the real user interface (TC-08 and TC-10 can only be demonstrated through the unit tests, because the interface never offers an action on a product that does not exist).

| ID | Test case | Input / steps | Expected result | Actual result | Pass/Fail |
|----|-----------|---------------|-----------------|---------------|-----------|
| TC-01 | Add valid product | Add Product -> Name `Wireless Mouse`, Quantity `25` -> Save Product | Dialog closes, green toast "was added", product appears with ID P-001, status **In Stock**, dashboard counts increase | | |
| TC-02 | Add product with empty name | Name empty (or only spaces), Quantity `5` -> Save | Dialog stays open, "Product name cannot be empty." under the name field, nothing is added | | |
| TC-03 | Add product with negative quantity | Name `Pen`, Quantity `-5` -> Save | "Quantity cannot be negative." under quantity field, nothing is added | | |
| TC-04 | Add product with non-numeric quantity | Name `Pen`, Quantity `abc` (also try `5.5`, `1e3`, empty) -> Save | "Quantity must be a whole number, for example 25." (empty gives "Quantity cannot be empty."); nothing is added | | |
| TC-05 | View all products | Add 3 products, open **Inventory** | All 3 shown with ID, name, quantity, status badge, edit/delete buttons; "Showing 3 of 3 products" | | |
| TC-06 | View empty inventory | Delete the data file, start the app, open Dashboard and Inventory | Dashboard shows the empty state with zero statistics; Inventory table shows "Your inventory is empty" and an Add Product button | | |
| TC-07 | Update existing product | Edit a product, change name and/or quantity -> Save Product | Dialog closes, toast "was updated", table row, status badge, dashboard numbers and activity feed update immediately | | |
| TC-08 | Update non-existing product | The user interface only offers Edit on rows that exist, so this is verified at business-logic level: run `mvn test -Dtest=InventoryManagerTest#updateNonExistingProductFails`. Optional UI check: with the app running, add a product, stop the app, delete that product from `inventory.json` by hand, start the app and press Refresh -> the row is gone and no Edit button exists for it | Test passes: `ProductNotFoundException` with message "Product P-099 no longer exists in the inventory."; the application never crashes | | |
| TC-09 | Delete existing product | Click the bin icon -> **Delete** in the confirmation | Product disappears, toast "was deleted", statistics update. (Pressing Cancel keeps the product.) | | |
| TC-10 | Delete non-existing product | Same approach: `mvn test -Dtest=InventoryManagerTest#deleteNonExistingProductFails` | Test passes: `ProductNotFoundException`; in the UI the error would appear as a red toast (the delete button handler catches `InventoryException`) | | |
| TC-11 | Search for an existing product | Type part of a name (any letter case) in the top search box | Only matching products shown, "Showing x of y"; clearing the box shows all again. Searching a name that does not exist shows "No matching products" with a Clear filters button | | |
| TC-12 | Filter by stock status | Click the **In Stock**, **Low Stock**, **Out of Stock** chips (and combine with a search) | Only products with that status shown; chip counters show the totals | | |
| TC-13a | Status at quantity 0 | Add product with Quantity `0` | **Out of Stock** (red badge) | | |
| TC-13b | Status at quantity 1 | Quantity `1` | **Low Stock** (amber badge) | | |
| TC-13c | Status at quantity 10 | Quantity `10` | **Low Stock** | | |
| TC-13d | Status at quantity 11 | Quantity `11` | **In Stock** (green badge) | | |
| TC-14 | Data persists after restart | Add/edit/delete some products, close the window, start the app again | The same products, quantities, statuses and recent activity are shown | | |

## Extra cases worth showing in the viva

| ID | Test case | Steps | Expected |
|----|-----------|-------|----------|
| TC-15 | Duplicate name | Add `Pen` twice (also `pen`) | "A product named ... already exists." under the name field |
| TC-16 | Quantity too large | Quantity `99999999999` | "Quantity cannot be more than 1,000,000." |
| TC-17 | Live status preview | Type 0, 5, 50 in the quantity field | Preview badge changes Out / Low / In Stock while typing; shows a dash for invalid text |
| TC-18 | Damaged data file | Replace the content of `inventory.json` with `hello`, start the app | App starts empty, red toast explains, the old file is kept as `inventory.json.corrupt-...` |
| TC-19 | Save failure | Make the data folder read-only, add a product | Red toast "could not be saved"; the product is not added (rollback) |
| TC-20 | Refresh | Edit `inventory.json` in a text editor while the app runs, press Refresh | Table shows the edited data |
