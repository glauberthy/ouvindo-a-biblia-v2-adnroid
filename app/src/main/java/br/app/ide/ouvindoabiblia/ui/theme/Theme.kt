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

    surface = CardSurface,          // Superfície de card (quase branco, acima do creme)
    onSurface = DeepBlueDark,

    surfaceVariant = Color(0xFFEBE0DB), // Um pouco mais escuro que o creme para variações
    onSurfaceVariant = SlateBlue,       // Texto de apoio (6,79:1 no creme — passa AA)

    outline = RosyBeige,            // Bordas sutis dos cards

    error = ErrorRed
)

// 2. Esquema de cores ESCURO
//
// Antes este esquema usava surface == background (card sem separação do fundo) e
// SlateBlue como card, o que clareava demais. Agora usa a paleta Dark* do Color.kt,
// com contraste medido — ver comentários lá.
private val DarkColors = darkColorScheme(
    primary = DarkTextPrimary,
    onPrimary = DarkBackground,

    primaryContainer = SlateBlue,
    onPrimaryContainer = DarkTextPrimary,

    secondary = DarkTextSecondary,
    onSecondary = DarkBackground,

    tertiary = DarkOutline,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkOutline,

    error = ErrorRed
)

/**
 * Camada semântica de cor.
 *
 * As telas foram escritas referenciando cores concretas (`CreamBackground`,
 * `DeepBlueDark`, …), que são `val` de topo e portanto não reagem ao tema — é por isso
 * que o modo escuro não funcionava, mesmo com [DarkColors] existindo. Estes getters são
 * `@Composable`, então resolvem do esquema ativo em tempo de composição.
 *
 * Regra ao migrar uma tela: use estes tokens para o que é PÁGINA (fundo, card, texto,
 * borda) e os tokens de marca ([BrandNavy]/[OnBrandNavy]) para o que é escuro nos dois
 * temas (bottom bar e player). Trocar um pelo outro inverte a tela.
 */
object AppColors {
    /** Fundo da página. */
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    /** Superfície de card/sheet sobre o fundo. */
    val card: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    /** Texto e ícones principais sobre fundo/card. */
    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    /** Texto de apoio (descrições, legendas). */
    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** Bordas e divisores sutis. */
    val outline: Color
        @Composable get() = MaterialTheme.colorScheme.outline
}

/**
 * Interruptor do tema escuro.
 *
 * Fica **desligado** até TODAS as telas lerem cor via [AppColors]/tokens de marca. Um app
 * meio-escuro e meio-creme é pior para o usuário do que um app só-claro — e hoje a maior
 * parte das telas ainda referencia as cores claras direto. Ligar antes de concluir a
 * migração entrega telas invertidas.
 */
const val DARK_THEME_ENABLED = false

@Composable
fun OuvindoABibliaTheme(
    darkTheme: Boolean = DARK_THEME_ENABLED && isSystemInDarkTheme(),
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