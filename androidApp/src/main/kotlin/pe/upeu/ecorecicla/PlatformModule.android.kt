package pe.upeu.ecorecicla

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.*
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.di.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class EcoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val seguro=AlmacenSeguroAndroid(this)
        val base=baseLocalAndroid(this)
        if(seguro.leer()==null) base.limpiar()
        val cm=getSystemService(ConnectivityManager::class.java)
        val online=MutableStateFlow(cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) { online.value=false }
            override fun onCapabilitiesChanged(network: Network,capabilities: NetworkCapabilities) { online.value=capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) }
        })
        val platformModule=module {
            single<AlmacenSeguro> { seguro }
            single<BaseLocal> { base }
            single<HttpClientEngine> { OkHttp.create() }
            single { ApiConfig(BuildConfig.API_URL) }
            single<Flow<Boolean>> { online }
            single<Reloj> { object : Reloj {
                override fun ahoraIso()=OffsetDateTime.now(ZoneId.of("America/Lima")).toString()
                override fun claveUnica()=UUID.randomUUID().toString()
            } }
        }
        startKoin { androidContext(this@EcoApplication); modules(platformModule,redModule,dataModule,domainModule,presentationModule) }
    }
}
