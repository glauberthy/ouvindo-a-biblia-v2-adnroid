package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.app.ide.ouvindoabiblia.ui.theme.CreamBackground
import br.app.ide.ouvindoabiblia.ui.theme.DeepBlueDark
import br.app.ide.ouvindoabiblia.ui.theme.LavenderGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToItem: (String) -> Unit
) {
    Scaffold(
        containerColor = CreamBackground, // Mantém a identidade visual
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mais Opções",
                        color = DeepBlueDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CreamBackground,
                    scrolledContainerColor = CreamBackground,
                    titleContentColor = DeepBlueDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.size(16.dp))

            MoreMenuItem(
                icon = Icons.Default.Info,
                title = "Sobre o App",
                onClick = { onNavigateToItem("sobre") }
            )
            MoreMenuItem(
                icon = Icons.Default.Copyright,
                title = "Direitos dos Áudios",
                onClick = { onNavigateToItem("audios") }
            )
            MoreMenuItem(
                icon = Icons.Default.Description,
                title = "Direitos das Capas",
                onClick = { onNavigateToItem("capas") }
            )
            MoreMenuItem(
                icon = Icons.Default.PrivacyTip,
                title = "Propósito do App",
                onClick = { onNavigateToItem("proposito") }
            )

            // Rodapé
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ouvindo a Bíblia",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepBlueDark.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Versão 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = LavenderGray
                )
            }
        }
    }
}

@Composable
fun MoreMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepBlueDark, // Azul Profundo
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = DeepBlueDark, // Azul Profundo
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = LavenderGray // Cinza Lavanda
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = LavenderGray.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}