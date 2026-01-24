package br.app.ide.ouvindoabiblia.util

import android.content.Context
import android.content.Intent

object ShareUtils {
    fun shareCurrentContent(context: Context, title: String, subtitle: String) {
        // Gera o link da loja automaticamente baseado no ID do seu app
        val appLink = "https://play.google.com/store/apps/details?id=${context.packageName}"

        // Monta a mensagem final
        val message = "Estou ouvindo $title - $subtitle.\n\nBaixe o app Ouvindo a Bíblia: $appLink"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Compartilhar via")
        // Flag para garantir que abra em uma nova tarefa se necessário
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(shareIntent)
    }
}