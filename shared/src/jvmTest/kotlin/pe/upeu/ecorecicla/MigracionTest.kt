package pe.upeu.ecorecicla

import kotlin.test.*
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.upeu.ecorecicla.db.EcoDatabase

class MigracionTest {
    @Test fun conservaRutaExistenteAlAgregarPadronYSupervision() {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null,"CREATE TABLE RutaLocal(id INTEGER NOT NULL PRIMARY KEY,fecha TEXT NOT NULL,cerrada INTEGER NOT NULL,ultimaSync TEXT NOT NULL)",0)
            driver.execute(null,"INSERT INTO RutaLocal VALUES(7,'2026-10-28',0,'sincronizado')",0)
            EcoDatabase.Schema.migrate(driver,1,2)
            val q=EcoDatabase(driver).ecoQueries
            assertEquals(7L,q.ruta().executeAsOne().id); assertEquals(0L,q.ruta().executeAsOne().usuarioId)
            q.guardarPadron(1,"Empresa","Direccion","Norte","Papel")
            assertEquals("Empresa",q.padron().executeAsOne().razonSocial)
        } finally { driver.close() }
    }
}
