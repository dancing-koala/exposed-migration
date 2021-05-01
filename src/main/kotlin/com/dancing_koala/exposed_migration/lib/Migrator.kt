package com.dancing_koala.exposed_migration.lib

import com.dancing_koala.exposed_migration.lib.database.MigrationsTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.system.measureTimeMillis

class Migrator(
    val currentVersion: Int,
    val initBlock: () -> Unit,
    migrations: List<Migration> = emptyList(),
) {
    private val migrationContainer = MigrationContainer(migrations)

    init {
        if (currentVersion < 0) {
            throw IllegalArgumentException("current version should not be negative, got <$currentVersion>")
        }
    }

    fun applyMigrations() {
        transaction {
            // Make sure migrations table exists
            SchemaUtils.create(MigrationsTable)
        }
        transaction {
            val lastMigratedVersion = MigrationsTable.select {
                (MigrationsTable.successful eq true)
            }.orderBy(MigrationsTable.version to SortOrder.DESC)
                .limit(1)
                .map { it[MigrationsTable.version] }
                .firstOrNull() ?: -1

            if (lastMigratedVersion < 0) {
                val duration = measureTimeMillis { initBlock() }

                val resultedValues = MigrationsTable.insert {
                    it[version] = currentVersion
                    it[kclass] = "initBlock"
                    it[migratedAt] = System.currentTimeMillis()
                    it[executionTime] = duration
                    it[successful] = true
                }.resultedValues
            } else {
                val end = currentVersion

                val migrations = migrationContainer.findMigrationPath(lastMigratedVersion, end)

                if (migrations.isEmpty()) {
                    if (lastMigratedVersion != end) {
                        throw MissingMigrationException(lastMigratedVersion, end)
                    }
                    println("No migration to apply")
                    return@transaction
                }

                migrations.forEach { migration ->
                    val duration = measureTimeMillis { migration.migrate(this) }

                    MigrationsTable.insert {
                        it[version] = migration.toVersion
                        it[kclass] = migration::class.simpleName ?: "None"
                        it[migratedAt] = System.currentTimeMillis()
                        it[executionTime] = duration
                        it[successful] = true
                    }
                }
            }
        }
    }

    private class MigrationContainer(migrations: List<Migration>) {
        private val migrations: HashMap<Int, TreeMap<Int, Migration>> = hashMapOf()

        init {
            addMigrations(migrations)
        }

        fun addMigrations(migrations: List<Migration>) =
            migrations.forEach(this::addMigration)

        private fun addMigration(migration: Migration) {
            val start = migration.fromVersion
            val end = migration.toVersion

            val targetMap = migrations[start] ?: TreeMap<Int, Migration>().also {
                migrations[start] = it
            }

            targetMap[end]?.let { existing->
                println("Overriding migration $existing with $migration")
            }

            targetMap[end] = migration
        }

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
}