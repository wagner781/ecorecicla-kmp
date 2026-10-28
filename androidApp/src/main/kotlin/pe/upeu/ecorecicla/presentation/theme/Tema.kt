package pe.upeu.ecorecicla.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable fun EcoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF146B45),secondary=Color(0xFF516A5B),background=Color(0xFFF3F6F0),surface=Color.White,primaryContainer=Color(0xFFD9EDDB)),content=content)
}
