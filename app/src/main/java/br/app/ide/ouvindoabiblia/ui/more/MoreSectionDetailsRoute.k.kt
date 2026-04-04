package br.app.ide.ouvindoabiblia.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.ide.ouvindoabiblia.ui.home.components.ErrorScreen
import br.app.ide.ouvindoabiblia.ui.home.components.LoadingScreen

@Composable
fun MoreSectionDetailsRoute(
    bottomContentPadding: Dp = Dp.Unspecified,
    onBackClick: () -> Unit,
    onUrlClick: (String) -> Unit = {},
    viewModel: MoreSectionDetailsViewModel = hiltViewModel()
) {
    when (val state = viewModel.uiState.collectAsStateWithLifecycle().value) {
        MoreSectionDetailsUiState.Loading -> LoadingScreen()

        is MoreSectionDetailsUiState.Error -> ErrorScreen(state.message) {
            onBackClick()
        }

        is MoreSectionDetailsUiState.Success -> {
            MoreSectionDetailsScreen(
                section = state.section,
                bottomContentPadding = if (bottomContentPadding == Dp.Unspecified) 0.dp else bottomContentPadding,
                onBackClick = onBackClick,
                onUrlClick = onUrlClick
            )
        }
    }
}