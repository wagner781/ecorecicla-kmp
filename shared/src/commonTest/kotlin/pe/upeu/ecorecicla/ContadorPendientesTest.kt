package pe.upeu.ecorecicla

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import io.ktor.client.engine.mock.MockEngine
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.*
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.repository.EcoRepositoryOffline
import pe.upeu.ecorecicla.db.EcoDatabase
import pe.upeu.ecorecicla.domain.model.*
import pe.upeu.ecorecicla.domain.repository.*
import pe.upeu.ecorecicla.domain.usecase.ObservarPendientesUseCase

class ContadorPendientesTest {
    @Test fun flujoSqlEmiteAlRegistrarEditarDescartarYLimpiar()=runTest {
        val driver=driverMemoria(); val local=BaseLocal(EcoDatabase(driver)); local.q.guardarRuta(1,"2026-10-28",0,"")
        val seguro=SeguroMemoria().apply { guardar(Sesion("jwt","supervisor1","SUPERVISOR",setOf("PESAJE_CREAR","PESAJE_EDITAR","PESAJE_ANULAR")),3600) }
        val http=crearCliente(MockEngine { error("No necesita red para contar") },ApiConfig("https://eco.test/api/v1/"),seguro)
        val reloj=object : Reloj { var n=0; override fun ahoraIso()="2026-10-28T08:00:00-05:00"; override fun claveUnica()="clave-${++n}" }
        val repo=EcoRepositoryOffline(local,EcoApi(http),seguro,reloj)
        val valores=Channel<Long>(Channel.UNLIMITED)
        val job=launch(Dispatchers.Default) { ObservarPendientesUseCase(repo)().collect { valores.send(it) } }
        suspend fun esperar(n: Long)=withContext(Dispatchers.Default) {
            withTimeout(5000) { do { val actual=valores.receive() } while(actual!=n) }
        }
        try {
            esperar(0)
            repo.registrar(FormularioPesaje(1,"Papel",12.0,2.0,null)); esperar(1)
            repo.registrar(FormularioPesaje(1,"Papel",13.0,2.0,null)); esperar(2)
            repo.registrar(FormularioPesaje(1,"Papel",14.0,2.0,null),-1); esperar(2)
            repo.descartarPendiente(-1); esperar(1)
            local.definitivo(PesajeDto(101,1,1,reloj.ahoraIso(),"Papel",12.0,2.0,10.0,"operario1")); esperar(1)
            repo.anular(101,"Duplicado"); esperar(2)
            repo.cerrarSesion(); esperar(0)
        } finally { job.cancelAndJoin(); valores.close(); http.close(); driver.close() }
    }
}
