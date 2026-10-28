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

class MainActivity : ComponentActivity() {
    private lateinit var vm: EcoViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm=ViewModelProvider(this,object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = get<EcoViewModel>() as T
        })[EcoViewModel::class.java]
        setContent { EcoApp(vm) }
    }
    override fun onResume() { super.onResume(); if(::vm.isInitialized) vm.verificarSesion() }
}
