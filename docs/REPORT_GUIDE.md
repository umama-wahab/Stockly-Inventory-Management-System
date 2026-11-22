# Report and viva preparation

## Suggested report structure

1. **Title page** - Stockly, course, OEL title, name, registration number, date.
2. **Introduction** - the problem (a small store tracking stock), objectives, scope.
3. **Requirements** - list the 9 required operations and the non-functional requirements (usability, persistence, documentation, version control).
4. **Design** - layered architecture diagram (copy from `docs/ARCHITECTURE.md`), class diagram (draw the classes in the table: Product, StockStatus, InventoryManager, InventoryRepository, JsonInventoryRepository, controllers), a screen-flow description (Dashboard / Inventory / Reports / Dialog).
5. **Implementation** - technology choices and why; how the stock status rule works; how validation works; how persistence works; how the UI refreshes automatically (ObservableList).
6. **Software construction practices** - OOP principles used, separation of concerns, exception handling with rollback, Javadoc, Git history, Maven build, unit tests.
7. **Testing** - the completed table from `docs/TEST_CASES.md` with screenshots; result of `mvn test`.
8. **Screenshots** - dashboard, inventory, dialog with an error, empty state, reports (see `docs/screenshots`; take your own on your machine).
9. **Challenges and limitations** - single user, JSON file instead of a database, no authentication, Settings page not implemented.
10. **Future work** - SQLite, categories, prices, barcode scanning, export to CSV, multi-user.
11. **Conclusion**, **References** (JavaFX docs, Maven docs, Javadoc guide).
12. **Appendix** - Git log (`git log --oneline`), Javadoc index screenshot.

## Viva questions to prepare

- Why is the stock status not stored or typed in? (derived from quantity; cannot become inconsistent)
- Where does validation happen and why is it `static`?
- What is an `ObservableList` and how does the table update without calling refresh?
- Why is there an `InventoryRepository` interface? Could you switch to SQLite? (yes - write a new class implementing it)
- What happens if saving fails? (rollback, the user sees an error)
- Why does `Main` not extend `Application`? (runnable JAR launch limitation)
- Explain MVC here: Product = model, FXML = view, controllers = controller, `InventoryManager` = the model's business logic.
- What exceptions did you create and why checked ones?
- Show a Javadoc page and explain `@param`, `@return`, `@throws`.
- Walk through the code path of "Add Product" from button click to the JSON file.

## Add-product code path (for explanation)

`Add Product button` -> `MainController.handleAddProduct()` -> `ProductDialogController.open()` -> user presses Save ->
`handleSave()` validates with `InventoryManager.validateName/parseQuantity` -> `manager.addProduct()` -> new `Product` added to the
ObservableList -> activity recorded -> `JsonInventoryRepository.save()` writes the file -> table, dashboard and report listeners refresh -> toast shown.
