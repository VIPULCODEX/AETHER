package com.aether.core.data

import app.cash.sqldelight.db.SqlDriver

expect fun generateId(): String

expect fun currentTimeMillis(): Long

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
