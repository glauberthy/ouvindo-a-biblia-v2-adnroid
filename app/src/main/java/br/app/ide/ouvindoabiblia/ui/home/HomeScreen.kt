import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.ide.ouvindoabiblia.R
// Importe seu tema aqui
// import br.app.ide.ouvindoabiblia.ui.theme.OuvindoABibliaTheme

@Composable
fun HomeScreen(
    // viewModel: HomeViewModel = hiltViewModel() // Injetaremos depois
    onBookClick: (String) -> Unit = {}
) {
    // O Scaffold principal da tela Home
    Scaffold(
        topBar = { HomeTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Seção 1: Grid de Destaques
            item {
                FeaturedGridSection()
            }

            // Seção 2: Título + Lista Vertical
            item {
                SectionTitle(title = "Pra ir direto pros seus favoritos")
                FavoriteTracksList()
            }

            // Seção 3: Título + Lista Horizontal
            item {
                SectionTitle(title = "Tocados recentementes")
                RecentlyPlayedRow()
            }

            // Exemplo: Mantendo a lista de livros que você já fez funcionar
            item {
                SectionTitle(title = "Antigo Testamento (Sua Lista Atual)")
            }
            // Aqui viria o items(state.books) { book -> ... } que você já tem.
        }
    }
}

// --- COMPONENTES MENORES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Início",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            // Placeholder para avatar do usuário
            Surface(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .size(32.dp),
                shape = CircleShape,
                color = Color.Gray
            ) {
                // Icone temporário, substituir por AsyncImage (Coil) depois
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        },
        actions = {
            IconButton(onClick = { /* TODO */ }) {
                // Usando um Badge para o pontinho vermelho de notificação
                BadgedBox(
                    badge = { Badge { Text("1") } } // Exemplo simples
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificações")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
    )
}

// --- Seção de Destaques (Grid Horizontal) ---
@Composable
fun FeaturedGridSection() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { FeaturedSquareItem(icon = Icons.Default.Add, label = "Fixar conte...", backgroundColor = Color(0xFFE0E0E0)) }
        item { FeaturedSquareItem(icon = Icons.Default.Favorite, label = "Mais querid...", backgroundColor = Color(0xFFFFD1C5), iconTint = Color.Red) }
        item { FeaturedCircleItem(label = "Flow", backgroundColor = Color(0xFFFF4081)) }
        // Adicione mais itens conforme necessário
    }
}

@Composable
fun FeaturedSquareItem(icon: ImageVector, label: String, backgroundColor: Color, iconTint: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(32.dp))
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun FeaturedCircleItem(label: String, backgroundColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
    }
}

// --- Seção Vertical ("Pra ir direto...") ---
@Composable
fun FavoriteTracksList() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Placeholders
        TrackListItem(title = "Ele É Deus", subtitle = "Grupo Mensagem")
        Spacer(modifier = Modifier.height(8.dp))
        TrackListItem(title = "Alegria", subtitle = "Josué Rodrigues")
        Spacer(modifier = Modifier.height(8.dp))
        TrackListItem(title = "Bem Supremo (Jó 42:5)", subtitle = "Adhemar De Campos")
    }
}

@Composable
fun TrackListItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable() { /* TODO */ }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder de Imagem
        Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(4.dp), color = Color.Gray) {}

        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Seção Horizontal ("Tocados recentemente") ---
@Composable
fun RecentlyPlayedRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) { // 5 itens de exemplo
            AlbumCardItem()
        }
    }
}

@Composable
fun AlbumCardItem() {
    Column(modifier = Modifier.width(100.dp)) {
        // Placeholder de Imagem
        Surface(modifier = Modifier.size(100.dp), shape = RoundedCornerShape(8.dp), color = Color.Gray) {}
        Text(text = "Nova Jerusalém", style = MaterialTheme.typography.bodyMedium, maxLines = 2, modifier = Modifier.padding(top = 8.dp))
    }
}

// --- PREVIEW ---
@Preview(showBackground = true )
@Composable
fun HomeScreenPreview() {
    // OuvindoABibliaTheme { // Descomente quando tiver o tema configurado
    HomeScreen()
    // }
}