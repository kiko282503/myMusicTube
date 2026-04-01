package com.musictube.player.platform

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific factory that returns the correct SQLDelight driver. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
