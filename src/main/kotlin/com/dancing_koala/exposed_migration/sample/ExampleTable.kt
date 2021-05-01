package com.dancing_koala.exposed_migration.sample

import org.jetbrains.exposed.dao.id.IntIdTable

internal object ExampleTable : IntIdTable(name = "Example") {
    val name = varchar("name", 64)
}

internal object ExampleTableV2 : IntIdTable(name = "Example") {
    val name = varchar("name", 64)
    val lastName = varchar("lastName", 64)
}

internal object ExampleTableV3 : IntIdTable(name = "Example") {
    val name = varchar("name", 64)
    val age = integer("age")
}