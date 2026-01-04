package br.app.ide.ouvindoabiblia

import HomeScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OuvindoABibliaTheme {
                // Aqui chamamos a tela principal que criamos
                HomeScreen()
            }
        }
    }
}