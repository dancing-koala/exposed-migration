package com.dancing_koala.exposed_migration.database

import org.jetbrains.exposed.dao.id.IntIdTable
import java.util.*

object Migrations : IntIdTable(name = "MIGRATIONS") {
    val version = integer("version").uniqueIndex()
    val kclass = varchar("kclass", 256)
    val appliedAt = long("appliedAt").clientDefault { Calendar.getInstance().timeInMillis }
    val executionTime = long("executionTime")
    val successful = bool("successful")
}