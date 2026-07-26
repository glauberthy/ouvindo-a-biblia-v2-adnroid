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

// ---------------------------------------------------------------------------
// Paleta ESCURA
//
// Não é cinza/preto genérico: mantém a identidade quente do app (papel antigo +
// azul profundo + ouro). Preto puro é evitado de propósito, como o M3 recomenda.
// Contraste medido (WCAG): texto primário 14,3:1 no fundo e 12,9:1 no card; texto
// secundário 6,98:1; ouro 10,7:1 — todos acima do mínimo de 4,5:1 para texto.
// ---------------------------------------------------------------------------
val DarkBackground = Color(0xFF17161F)   // Fundo: quase-preto levemente azulado/quente
val DarkCardSurface = Color(0xFF221F2C)  // Card: um degrau acima do fundo
val DarkTextPrimary = Color(0xFFEDE4DE)  // Creme esmaecido (não branco puro)
val DarkTextSecondary = Color(0xFFB3A7B1) // Lavanda clara
val DarkOutline = Color(0xFF423C50)      // Bordas sutis

// ---------------------------------------------------------------------------
// Cores de MARCA — invariantes ao tema
//
// A barra de navegação e o player são escuros nos DOIS temas, por design. Se essas
// superfícies lessem o color scheme, elas clareariam no tema escuro e inverteriam a
// identidade do app. Por isso têm token próprio, fora do esquema de cores.
// ---------------------------------------------------------------------------
val BrandNavy = Color(0xFF22223B)      // Fundo da bottom bar e do player
val OnBrandNavy = Color(0xFFF2E9E4)    // Texto/ícones sobre o BrandNavy

// Selos AT/NT (Favoritos). São rótulos semânticos, não superfície de página, por isso
// não seguem o tema. Antes usavam RosyBeige e LavenderGray, que davam 2,10:1 e 3,19:1
// com o texto branco — o mínimo é 4,5:1. Estes dão 6,30:1 e 8,12:1, e continuam se
// distinguindo por matiz (quente x frio) além do próprio rótulo.
val BadgeOldTestament = Color(0xFF6E5C57)  // Marrom-rosado (Antigo)
val BadgeNewTestament = Color(0xFF4A4E69)  // Azul-ardósia (Novo)

val Accent = Color(0xFFE9C46A)        // Ouro Saffron

// Conteúdo (ícone/texto) sobre o [Accent]. O dourado é invariante ao tema, então o que
// fica sobre ele também tem que ser: usar a cor de texto do tema deixava ícone claro
// sobre ouro no tema escuro (1,33:1, invisível). Este dá 9,24:1.
val OnAccent = Color(0xFF22223B)
val Accent2 = Color(0xFFEF5466)
//val Accent = Color(0xFF81B29A)        // Menta Luminosa
//val Accent = Color(0xFFF4A261)        // Coral Suave