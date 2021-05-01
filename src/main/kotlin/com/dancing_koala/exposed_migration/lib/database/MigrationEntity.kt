package com.dancing_koala.exposed_migration.lib.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MigrationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MigrationEntity>(MigrationsTable)

    var version by MigrationsTable.version
    var kclass by MigrationsTable.kclass
    var migratedAt by MigrationsTable.migratedAt
    var executionTime by MigrationsTable.executionTime
    var successful by MigrationsTable.successful
}