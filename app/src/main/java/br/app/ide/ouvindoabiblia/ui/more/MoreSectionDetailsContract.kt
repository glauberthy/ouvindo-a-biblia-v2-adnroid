package br.app.ide.ouvindoabiblia.ui.more

import br.app.ide.ouvindoabiblia.data.remote.dto.MoreSectionDto

// Estado da UI seguindo o padrão LCE (Loading, Content, Error).
sealed interface MoreSectionDetailsUiState {
    data object Loading : MoreSectionDetailsUiState
    data class Success(val section: MoreSectionDto) : MoreSectionDetailsUiState
    data class Error(val message: String) : MoreSectionDetailsUiState
}

// Sem `Intent`: a tela é somente leitura (deriva de um Flow reativo) e o único
// "retry" do estado de erro é navegação (voltar), tratada na própria UI — não há
// ação de usuário roteada de volta ao ViewModel.
