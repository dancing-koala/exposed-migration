package com.dancing_koala.exposed_migration

import org.jetbrains.exposed.sql.Transaction

abstract class Migration(
    val startVersion: Int,
    val endVersion: Int,
) {
    abstract fun migrate(transaction: Transaction)

    override fun toString(): String {
        return "Migration($startVersion->$endVersion)"
    }
}