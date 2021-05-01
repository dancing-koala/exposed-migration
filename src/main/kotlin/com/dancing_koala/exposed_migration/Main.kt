package com.dancing_koala.exposed_migration

import com.dancing_koala.exposed_migration.database.Migrations
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


fun migration(
    startVersion: Int,
    endVersion: Int,
    migrateBlock: Transaction.() -> Unit
) = object : Migration(startVersion, endVersion) {
    override fun migrate(transaction: Transaction) = transaction.migrateBlock()
}

class MigrationContainer() {
    private val migrations: HashMap<Int, TreeMap<Int, Migration>> = hashMapOf()

    constructor(migrations: List<Migration>) : this() {
        addMigrations(migrations)
    }

    fun addMigrations(vararg migrations: Migration) =
        migrations.forEach(this::addMigration)


    fun addMigrations(migrations: List<Migration>) =
        migrations.forEach(this::addMigration)

    private fun addMigration(migration: Migration) {
        val start = migration.startVersion
        val end = migration.endVersion

        val targetMap = migrations[start] ?: TreeMap<Int, Migration>().also {
            migrations[start] = it
        }

        val existing = targetMap[end]

        if (existing != null) {
            println("Overriding migration $existing with $migration")
        }

        targetMap[end] = migration
    }

    fun getMigrations(): Map<Int, Map<Int, Migration>> = migrations.toMap()

    fun findMigrationPath(start: Int, end: Int): List<Migration> {
        if (start == end) return emptyList()
        return findUpMigrationPath(upgrade = end > start, start, end)
    }

    private fun findUpMigrationPath(upgrade: Boolean, initialStart: Int, end: Int): List<Migration> {
        var start = initialStart
        val result = mutableListOf<Migration>()

        while (if (upgrade) start < end else start > end) {
            val targetNodes = migrations[start] ?: return emptyList()

            val keySet = if (upgrade) {
                targetNodes.descendingKeySet()
            } else {
                targetNodes.keys
            }

            var found = false

            for (targetVersion in keySet) {
                val shouldAddToPath = if (upgrade) {
                    targetVersion in (start + 1)..end
                } else {
                    targetVersion in end until start
                }

                if (shouldAddToPath) {
                    result.add(targetNodes[targetVersion]!!)
                    start = targetVersion
                    found = true
                    break
                }
            }

            if (!found) {
                return emptyList()
            }
        }

        return result.toList()
    }
}


val Migrate1To2 = migration(1, 2) { println("migrate 1 to 2") }
val Migrate2To3 = migration(2, 3) { println("migrate 2 to 3") }
val Migrate3To4 = migration(3, 4) { println("migrate 3 to 4") }
val Migrate4To5 = migration(4, 5) { println("migrate 4 to 5") }

fun main() {
    val migrationContainer = MigrationContainer()

    migrationContainer.addMigrations(
        listOf(Migrate1To2, Migrate2To3, Migrate3To4, Migrate4To5, Migrate4To5)
    )

    val db = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.create(Migrations)

        Migrate1To2.migrate(this)
    }

}