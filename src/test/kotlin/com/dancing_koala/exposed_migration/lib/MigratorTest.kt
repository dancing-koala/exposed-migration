package com.dancing_koala.exposed_migration.lib

import com.dancing_koala.exposed_migration.lib.database.MigrationsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.test.Test

class MigratorTest {

    object Migrate1To2 : Migration(1, 2) {
        private val sql = """
        ALTER TABLE EXAMPLE ADD "lastName" VARCHAR(64) DEFAULT 'default' NOT NULL
    """.trimIndent()

        override fun migrate(transaction: Transaction) {
            transaction.exec(sql) {
                println(it)
            }
        }
    }

    object Migrate2To3 : Migration(2, 3) {
        private val sql = """
        ALTER TABLE EXAMPLE DROP COLUMN "lastName";
        ALTER TABLE EXAMPLE ADD "AGE" INT DEFAULT 42;
    """.trimIndent()

        override fun migrate(transaction: Transaction) {
            transaction.exec(sql) {
                println(it)
            }
        }
    }

    private fun getVersions(): List<Int> = transaction {
        MigrationsTable.select {
            (MigrationsTable.successful eq true)
        }.map { it[MigrationsTable.version] }
    }


    // Will create DB, add Example table, add Migrations table, read insert examples
    private fun stateV1() {
        Migrator(
            currentVersion = 1,
            migrations = emptyList(),
            initBlock = { SchemaUtils.create(ExampleTable) }
        ).applyMigrations()

        println("stateV1 versions=" + getVersions())

        val result = transaction {
            ExampleTable.select {
                ExampleTable.id.greater(0)
            }.map { it[ExampleTable.name] }
        }

        println(result)

        if (result.isEmpty()) {
            transaction {
                repeat(2) { index ->
                    ExampleTable.insert { it[name] = "name #$index" }
                }
            }
        }
    }

    // Will upgrade Example table to V2, read & insert examples v2
    private fun stateV2() {
        Migrator(
            currentVersion = 2,
            migrations = listOf(Migrate1To2),
            initBlock = { SchemaUtils.create(ExampleTableV2) }
        ).applyMigrations()

        println("stateV2 versions=" + getVersions())

        val resultV2 = transaction {
            ExampleTableV2.select {
                ExampleTableV2.id.greater(0)
            }.map {
                val name = it[ExampleTableV2.name]
                val lastName = it[ExampleTableV2.lastName]
                "name=$name, lastName=$lastName"
            }
        }

        println(resultV2)

        if (resultV2.size < 2) {
            transaction {
                repeat(2) { index ->
                    ExampleTableV2.insert {
                        it[name] = "name #${resultV2.size + index}"
                        it[lastName] = "lastName #${resultV2.size + index}"
                    }
                }
            }
        }
    }

    // Will upgrade Example table to V3, read & insert examples v3
    private fun stateV3() {
        Migrator(
            currentVersion = 3,
            migrations = listOf(Migrate1To2, Migrate2To3),
            initBlock = { SchemaUtils.create(ExampleTableV3) }
        ).applyMigrations()

        println("stateV3 versions=" + getVersions())

        val resultV3 = transaction {
            ExampleTableV3.select {
                ExampleTableV3.id.greater(0)
            }.map {
                val name = it[ExampleTableV3.name]
                val age = it[ExampleTableV3.age]
                "name=$name, age=$age"
            }
        }

        println(resultV3)

        if (resultV3.size < 4) {
            transaction {
                repeat(2) { index ->
                    ExampleTableV3.insert {
                        it[name] = "name #${resultV3.size + index}"
                        it[age] = resultV3.size + index
                    }
                }
            }
        }

        transaction {
            ExampleTableV3.select {
                ExampleTableV3.id.greater(0)
            }.map {
                val name = it[ExampleTableV3.name]
                val age = it[ExampleTableV3.age]
                "name=$name, age=$age"
            }
        }.let { println(it) }
    }

    @Test
    fun testMigrationFlow() {
        // Delete DB if already exists
        File("src/test/resources/test.mv.db").apply { if (exists()) delete() }

        val dbFile = File("src/test/resources/test")
        // Database must be connected before migrating
        Database.connect("jdbc:h2:${dbFile.absolutePath}", driver = "org.h2.Driver")

        stateV1()
        stateV2()
        stateV3()
    }
}