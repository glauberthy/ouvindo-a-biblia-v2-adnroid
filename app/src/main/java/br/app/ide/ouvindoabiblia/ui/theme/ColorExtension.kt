package br.app.ide.ouvindoabiblia.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Extrai a cor dominante da imagem
suspend fun extractDominantColorFromUrl(context: Context, imageUrl: String): Color? {
    if (imageUrl.isEmpty()) return null

    return withContext(Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false) // Necessário para ler o Bitmap
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        val bitmap = result?.toBitmap()

        if (bitmap != null) {
            val palette = Palette.from(bitmap).generate()
            // Tenta pegar cores vibrantes, se não der, pega a dominante
            val colorInt = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb

            colorInt?.let { Color(it) }
        } else {
            null
        }
    }
}

// Verifica se a cor é escura (para mudar o texto para branco)
fun Color.isDark(): Boolean {
    val luminance = (0.299 * this.red + 0.587 * this.green + 0.114 * this.blue)
    return luminance < 0.5
}