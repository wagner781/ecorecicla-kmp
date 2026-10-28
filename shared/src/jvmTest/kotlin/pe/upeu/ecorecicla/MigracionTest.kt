package pe.upeu.ecorecicla

import kotlin.test.*
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.upeu.ecorecicla.db.EcoDatabase

class MigracionTest {
    @Test fun agregarHumedadConservaPesajePendienteVersionDos() {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null,"""CREATE TABLE PesajeLocal(id INTEGER PRIMARY KEY NOT NULL,puntoId INTEGER NOT NULL,rutaId INTEGER NOT NULL,fechaHora TEXT NOT NULL,material TEXT NOT NULL,pesoBruto REAL NOT NULL,tara REAL NOT NULL,pesoNeto REAL NOT NULL,operario TEXT NOT NULL,anulado INTEGER NOT NULL,motivo TEXT,pendiente INTEGER NOT NULL,clave TEXT NOT NULL,operacion TEXT NOT NULL,observacion TEXT,error TEXT)""",0)
            driver.execute(null,"""INSERT INTO PesajeLocal VALUES(-1,1,1,'2026-10-28T08:00:00-05:00','Papel',12,2,0,'operario1',0,NULL,1,'clave-original','CREAR',NULL,NULL)""",0)
            EcoDatabase.Schema.migrate(driver,2,3)
            val p=EcoDatabase(driver).ecoQueries.pendientes().executeAsOne()
            assertEquals(-1L,p.id); assertEquals("clave-original",p.clave); assertNull(p.humedadEstimada)
        } finally { driver.close() }
    }
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
