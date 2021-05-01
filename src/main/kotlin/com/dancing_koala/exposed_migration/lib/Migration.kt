package com.dancing_koala.exposed_migration.lib

import org.jetbrains.exposed.sql.Transaction

abstract class Migration(
    val fromVersion: Int,
    val toVersion: Int,
) {
    abstract fun migrate(transaction: Transaction)
}