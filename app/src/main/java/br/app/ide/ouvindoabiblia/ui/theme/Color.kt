package br.app.ide.ouvindoabiblia.ui.theme

import androidx.compose.ui.graphics.Color


val CreamBackground = Color(0xFFF2E9E4) // Fundo Claro (Papel antigo)
val DeepBlueDark = Color(0xFF22223B)    // Fundo Escuro / Texto Principal
val SlateBlue = Color(0xFF4A4E69)       // Superfícies Intermediárias / Player Fallback
val LavenderGray = Color(0xFF9A8C98)    // Ícones Inativos / Texto Secundário
val RosyBeige = Color(0xFFC9ADA7)       // Destaques sutis
val ErrorRed = Color(0xFFEF5466)        // Cor de erro

// Superfície dos cards de lista (Mais, Temas, Estudos, Favoritos): quase branco, um
// tom acima do CreamBackground para o card destacar do fundo. Era repetida hardcoded
// como Color(0xFFFFFCFA) em 5 telas — mudar a identidade exigia editar 5 arquivos.
val CardSurface = Color(0xFFFFFCFA)

val Accent = Color(0xFFE9C46A)        // Ouro Saffron
val Accent2 = Color(0xFFEF5466)
//val Accent = Color(0xFF81B29A)        // Menta Luminosa
//val Accent = Color(0xFFF4A261)        // Coral Suave