package com.dancing_koala.exposed_migration.lib.database

import org.jetbrains.exposed.dao.id.IntIdTable
import java.util.*

object MigrationsTable : IntIdTable(name = "MIGRATIONS") {
    val version = integer("version").uniqueIndex()
    val kclass = varchar("kclass", 256)
    val migratedAt = long("migratedAt").clientDefault { Calendar.getInstance().timeInMillis }
    val executionTime = long("executionTime")
    val successful = bool("successful")
}