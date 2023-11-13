package utils

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.sellmair.pacemaker.SafePacemakerDatabase
import io.sellmair.pacemaker.sql.PacemakerDatabase

internal fun createInMemoryDatabase(): SafePacemakerDatabase = SafePacemakerDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    PacemakerDatabase.Schema.create(driver)
    PacemakerDatabase(driver)
}
