package com.dancing_koala.exposed_migration.lib


class MissingMigrationException(
    private val fromVersion: Int,
    private val toVersion: Int
) : Exception("Missing migration(s) [$fromVersion -> $toVersion]")