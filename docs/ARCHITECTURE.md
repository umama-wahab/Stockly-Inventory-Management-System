# Architecture and design decisions

## Layers

```
 FXML views + CSS          what the user sees
        |
 Controllers               react to clicks, show data, call the manager (no business rules!)
        |
 InventoryManager          ALL business logic: CRUD, validation, statistics, search rule, activity log
        |
 InventoryRepository       interface ("save/load")
        |
 JsonInventoryRepository   writes inventory.json
```

Dependencies only point downward. `InventoryManager` knows nothing about the user interface, and it only knows the
`InventoryRepository` *interface* (dependency inversion), so JSON could be replaced by SQLite without touching it.

## Technology choices

| Choice | Reason |
|---|---|
| Maven | dependency management, `mvn javafx:run`, `mvn test`, Javadoc plugin, fat jar |
| FXML + CSS | layout separated from code; Scene Builder compatible; professional styling |
| ObservableList | TableView, dashboard and report refresh automatically when data changes |
| JSON file (own tiny parser) | no database server, no external dependency, human-readable during the viva |
| JUnit 5 (test scope only) | business logic is tested without opening a window |

## How automatic refresh works

`InventoryManager.products` is an `ObservableList` created with an *extractor* (name + quantity properties), so it
fires an event when a product is added, removed **or edited**. The chain
`products -> FilteredList (search + status) -> SortedList (column sort) -> TableView` updates itself. The dashboard and
reports controllers add a `ListChangeListener` that recalculates their numbers.

## Stock status rule

`StockStatus.fromQuantity(int)` is the only place the rule exists. `Product` listens to its own quantity property and
recalculates a **read-only** status property, so a wrong status is impossible.

## Validation

`InventoryManager.validateName` and `parseQuantity` are `static` and throw `ValidationException` with a user-friendly
message and the offending field. The dialog shows the message under that field. Rejected: empty name, name > 60 chars,
duplicate name, empty quantity, letters/decimals, negative numbers, quantity > 1,000,000.

## Error handling

| Situation | Behaviour |
|---|---|
| Update / delete a product that no longer exists | `ProductNotFoundException` -> message in dialog / toast |
| Saving fails (read-only folder, disk full) | change is **rolled back** in memory, error shown, no inconsistent state |
| Data file damaged | renamed to `inventory.json.corrupt-<time>`, app starts empty and tells the user |
| Data file saved half-way (crash) | impossible: written to `.tmp` first, then atomically moved |

## Class overview

| Class | Responsibility |
|---|---|
| `Product` | one product; observable name/quantity, read-only status |
| `StockStatus` | enum + business rule |
| `Activity`, `ActivityType` | entries of the recent-activity feed |
| `InventoryManager` | business logic |
| `InventoryRepository`, `InventoryData` | storage abstraction and snapshot |
| `JsonInventoryRepository`, `SimpleJson` | JSON storage |
| `MainController` | sidebar, top bar, navigation, toasts (implements `AppShell`) |
| `DashboardController`, `InventoryController`, `ReportsController` | one page each |
| `ProductDialogController` | add/edit dialog |
| `Icon`, `StatusBadge`, `EmptyState`, `ConfirmDialog` | small reusable UI building blocks |

## OOP concepts 

- **Encapsulation** - `Product` fields are private; properties are read-only; changes only through validated setters.
- **Abstraction / interfaces** - `InventoryRepository`, `AppShell`.
- **Polymorphism** - `JsonInventoryRepository` and the test `InMemoryRepository` are used interchangeably.
- **Inheritance** - exception hierarchy (`InventoryException` -> `ValidationException`, ...); custom controls extend `Region`, `Label`, `VBox`.
- **Single responsibility** - see the class table.
- **Enums with behaviour** - `StockStatus`, `Page`, `ToastType`.
