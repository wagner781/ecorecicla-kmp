package pe.upeu.ecorecicla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelProvider
import pe.upeu.ecorecicla.presentation.listado.EcoViewModel
import org.koin.android.ext.android.get
import pe.upeu.ecorecicla.presentation.EcoApp
import pe.upeu.ecorecicla.presentation.administracion.AdministracionViewModel

class MainActivity : ComponentActivity() {
    private lateinit var vm: EcoViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=true
        val provider=ViewModelProvider(this,object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = when(modelClass) {
                EcoViewModel::class.java -> get<EcoViewModel>()
                AdministracionViewModel::class.java -> get<AdministracionViewModel>()
                else -> error("ViewModel no registrado")
            } as T
        })
        vm=provider[EcoViewModel::class.java]
        val admin=provider[AdministracionViewModel::class.java]
        setContent { EcoApp(vm,admin) }
    }
    override fun onResume() { super.onResume(); if(::vm.isInitialized) vm.verificarSesion() }
}
