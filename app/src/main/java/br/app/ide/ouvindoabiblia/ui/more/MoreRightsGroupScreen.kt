package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.theme.AppColors
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen

/**
 * Sublista de "Direitos e licenças" — privacidade, créditos de áudio/imagens e licenças
 * open source. Existe para que a raiz da tela Mais não ponha 5 itens obrigatórios e
 * raramente lidos com o mesmo peso visual do conteúdo institucional.
 *
 * Reusa o `MoreSectionCard` e o `MoreSectionSheetContent` da tela Mais: o conteúdo de
 * cada item continua abrindo no mesmo bottom sheet, que é o comportamento que roda hoje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreRightsGroupRoute(
    bottomContentPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    var selectedMenuItem by remember { mutableStateOf<MoreMenuItemUi?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (uiState) {
        MoreUiState.Loading -> LoadingScreen()

        is MoreUiState.Error -> ErrorScreen(uiState.message) {
            viewModel.handle(MoreIntent.Retry)
        }

        is MoreUiState.Success -> {
            MoreRightsGroupBody(
                bottomContentPadding = bottomContentPadding,
                onBackClick = onBackClick,
                onItemClick = { item -> selectedMenuItem = item }
            )

            selectedMenuItem?.let { item ->
                val selectedSection = uiState.content.sections.firstOrNull { it.id == item.id }

                ModalBottomSheet(
                    onDismissRequest = { selectedMenuItem = null },
                    sheetState = sheetState,
                    containerColor = AppColors.background
                ) {
                    MoreSectionSheetContent(
                        item = item,
                        section = selectedSection
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreRightsGroupBody(
    bottomContentPadding: Dp,
    onBackClick: () -> Unit,
    onItemClick: (MoreMenuItemUi) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val resolvedBottomPadding = if (bottomContentPadding == 0.dp) {
        navBarPadding + 16.dp
    } else {
        bottomContentPadding
    }

    val horizontalScreenPadding = 16.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = resolvedBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding + 8.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = AppColors.textPrimary
                    )
                }

                MoreScreenTitle(
                    title = "Direitos e licenças",
                    modifier = Modifier.fillMaxWidth(),
                    level = MoreTitleLevel.SUB
                )
            }
        }

        items(
            items = moreRightsMenuItems,
            key = { it.id }
        ) { item ->
            Box(
                modifier = Modifier.padding(horizontal = horizontalScreenPadding)
            ) {
                MoreSectionCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
