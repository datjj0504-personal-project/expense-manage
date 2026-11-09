# MoneyTrack

A JavaFX desktop application scaffold for expense management.

## Requirements

- JDK 21
- Maven 3.6.3 or later

## Run

From the project directory, run:

```shell
mvn clean javafx:run
```

Run the tests and compile the project with:

```shell
mvn clean verify
```

The interface is defined in `src/main/resources/fxml` and styled in
`src/main/resources/css`.

The dashboard uses SQLite data: the selected month controls income, expenses,
balance, category breakdown and the income/expense comparison. Recent activity
shows the latest five transactions across all dates, ordered by date and ID.
Money totals are shown in Vietnamese VND. Reports provide the same selected-month
totals, a category expense breakdown, and an income/expense comparison. Settings
shows the application version and the actual SQLite file location.

Categories are separated by income and expense. Add, rename, change the type,
or delete a category from its tab. Names must be unique within a type. Categories
used by transactions cannot be deleted or changed to another type; rename them
or reassign their transactions first.

On Transactions, add, select and edit, or confirm deletion of entries. Filter
by type, an exact date, or a case-insensitive description search; Clear resets
the filters. Select a type before choosing a matching category. Amounts must
be positive whole VND values. Database operations run in the background.

## Database

On startup, MoneyTrack creates `${user.home}/.moneytrack/moneytrack.db` and
initializes the schema and default categories in a single transaction. Defaults
are inserted only when the schema is first created, so later category changes
are preserved. Each database connection enables SQLite foreign keys.

The JDBC repositories provide CRUD operations for categories and transactions.
Amounts are stored as 64-bit integer VND, and transaction dates use ISO-8601
calendar dates. A transaction's type must match its category; a category in use
cannot be deleted or changed to a different type. Repository methods propagate
database errors to their caller. New model instances have ID 0; `create` returns
the saved instance with its assigned ID. Updates and deletes return whether a
row was found.
