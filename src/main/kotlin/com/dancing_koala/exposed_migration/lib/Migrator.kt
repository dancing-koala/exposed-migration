package com.dancing_koala.exposed_migration.lib

import com.dancing_koala.exposed_migration.lib.database.MigrationEntity
import com.dancing_koala.exposed_migration.lib.database.MigrationsTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
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
            val lastMigration = MigrationEntity.find {
                (MigrationsTable.successful eq true)
            }.orderBy(MigrationsTable.version to SortOrder.DESC).limit(1).firstOrNull()

            if (lastMigration == null) {
                val duration = measureTimeMillis {
                    initBlock()
                }

                val result = MigrationEntity.new {
                    version = currentVersion
                    kclass = "initBlock"
                    migratedAt = System.currentTimeMillis()
                    executionTime = duration
                    successful = true
                }

            } else {
                val start = lastMigration.version
                val end = currentVersion

                val migrations = migrationContainer.findMigrationPath(start, end)

                if (migrations.isEmpty()) {
                    if (start != end) {
                        throw MissingMigrationException(start, end)
                    }
                    println("No migration to apply")
                    return@transaction
                }

                migrations.forEach {
                    val duration = measureTimeMillis {
                        it.migrate(this)
                    }

                    MigrationEntity.new {
                        version = it.toVersion
                        kclass = it::class.simpleName ?: "None"
                        migratedAt = System.currentTimeMillis()
                        executionTime = duration
                        successful = true
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
}