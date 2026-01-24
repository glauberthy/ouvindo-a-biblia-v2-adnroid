package br.app.ide.ouvindoabiblia.ui.player.components

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun CastButton(
    color: Color,
    modifier: Modifier = Modifier
) {
    // Detecta se a cor pedida (o texto/ícone) é escura ou clara
    val isColorDark = color.luminance() < 0.5

    AndroidView(
        modifier = modifier.size(24.dp),
        factory = { context ->
            // TRUQUE DO TEMA:
            // O MediaRouteButton decide a cor do ícone baseado no tema do contexto.
            // - Tema Dark (Theme_AppCompat) -> Ícone Branco
            // - Tema Light (Theme_AppCompat_Light) -> Ícone Preto

            val themeId = if (isColorDark) {
                // Queremos ícone ESCURO (ex: Preto) -> Usamos tema Light
                androidx.appcompat.R.style.Theme_AppCompat_Light
            } else {
                // Queremos ícone CLARO (ex: Branco) -> Usamos tema Dark
                androidx.appcompat.R.style.Theme_AppCompat
            }

            val wrappedContext = ContextThemeWrapper(context, themeId)

            MediaRouteButton(wrappedContext).apply {
                CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
            }
        }
    )
}