package pe.upeu.ecorecicla

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.upeu.ecorecicla.db.EcoDatabase

actual fun driverMemoria(): SqlDriver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { EcoDatabase.Schema.create(it) }
