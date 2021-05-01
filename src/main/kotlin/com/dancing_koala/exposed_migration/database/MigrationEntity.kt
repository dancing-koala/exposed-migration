package com.dancing_koala.exposed_migration.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MigrationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MigrationEntity>(Migrations)

    var version by Migrations.version
    var kclass by Migrations.kclass
    var appliedAt by Migrations.appliedAt
    var executionTime by Migrations.executionTime
    var successful by Migrations.successful
}