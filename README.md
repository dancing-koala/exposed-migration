# exposed-migration
A simple migration system for Exposed Framework loosely based on Room Migration system & source code

## Compatibility

Compatible with Exposed `0.31.1` (I did not try with previous versions)

## Download

Add JitPack to your repositories

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Add dependencies

```groovy
dependencies {
    implementation 'org.jetbrains.exposed:exposed-core:$exposed_version'
    implementation 'com.github.dancing-koala:exposed-migration:0.0.3'
}
```

## How it works

The `Migrator` adds a **MIGRATION** table into the database to store data about the last migration applied.
It mainly uses the `version` field of the table to check against the current version.

## Usage

### Creating migrations

```kotlin
object Migrate1To2 : Migration(fromVersion = 1, toVersion = 2) {

    // The raw sql query to execute
    private val sql = """
        ALTER TABLE EXAMPLE ADD "lastName" VARCHAR(64) DEFAULT '' NOT NULL
    """.trimIndent()

    // This method is executed in a transaction { }
    override fun migrate(transaction: Transaction) {
        transaction.exec(sql) {
            println(it)
        }
    }
}
```
Notes:
* The example above uses a raw SQL query but you can also use Exposed DAO or SQL DSL
* Since `migrate()` returns nothing, it is advised to throw an exception if something goes wrong in your code

### Apply migrations

First connect to your database with Exposed :

```kotlin
Database.connect(...)
```

Then instantiate the `Migrator`:

```kotlin
val migrator = Migrator(
    currentVersion = 2, //The current version of your database
    migrations = listOf(Migrate1To2), //The list of available migrations
    initBlock = { SchemaUtils.create(Table1, Table2, Table3) } // This block is invoked if no previous migration exists
)
```

Finally, you just need to call `applyMigrations()`:

```kotlin
migrator.applyMigrations()
```
