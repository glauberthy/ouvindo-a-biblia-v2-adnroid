package br.app.ide.ouvindoabiblia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Esquema de cores CLARO (Papel/Creme)
private val LightColors = lightColorScheme(
    primary = DeepBlueDark,         // Texto principal e ícones ativos (Azul Escuro)
    onPrimary = CreamBackground,    // Texto sobre botões primários
    primaryContainer = DeepBlueDark,
    onPrimaryContainer = CreamBackground,

    secondary = SlateBlue,          // Elementos secundários
    onSecondary = Color.White,

    tertiary = RosyBeige,           // Destaques

    background = CreamBackground,   // O fundo Creme (Isabella)
    onBackground = DeepBlueDark,    // Texto sobre o fundo

    surface = CreamBackground,      // Superfícies (Cards, BottomBar no modo padrão)
    onSurface = DeepBlueDark,

    surfaceVariant = Color(0xFFEBE0DB), // Um pouco mais escuro que o creme para variações
    onSurfaceVariant = SlateBlue,       // Texto secundário

    error = ErrorRed
)

// 2. Esquema de cores ESCURO (Azul Profundo Sofisticado)
private val DarkColors = darkColorScheme(
    primary = CreamBackground,      // Texto principal (Creme sobre escuro)
    onPrimary = DeepBlueDark,

    primaryContainer = SlateBlue,
    onPrimaryContainer = CreamBackground,

    secondary = RosyBeige,
    onSecondary = DeepBlueDark,

    background = DeepBlueDark,      // O fundo Azul Profundo
    onBackground = CreamBackground,

    surface = DeepBlueDark,         // Superfícies alinhadas ao fundo
    onSurface = CreamBackground,

    surfaceVariant = SlateBlue,     // Cards mais claros que o fundo
    onSurfaceVariant = LavenderGray, // Texto secundário (Lavanda)

    error = ErrorRed
)

@Composable
fun OuvindoABibliaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Seleção do esquema de cores
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Define a cor da barra de status como transparente para efeito Edge-to-Edge
            window.statusBarColor = Color.Transparent.toArgb()

            // Controla a cor dos ícones da barra de status
            // Se o tema for escuro, os ícones devem ser claros (isAppearanceLightStatusBars = false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}