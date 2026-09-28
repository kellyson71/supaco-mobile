package io.github.kellyson71.supaco.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.data.local.FaltasHistory
import io.github.kellyson71.supaco.data.model.BoletimItem
import io.github.kellyson71.supaco.data.model.PeriodoLetivo
import io.github.kellyson71.supaco.data.model.Profile
import io.github.kellyson71.supaco.data.model.Servidor
import io.github.kellyson71.supaco.data.remote.userMessage
import io.github.kellyson71.supaco.data.repository.AcademicRepository
import io.github.kellyson71.supaco.data.repository.AuthRepository
import io.github.kellyson71.supaco.data.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    /** Carregando sem nada para mostrar ainda. */
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val materias: List<MateriaDisplay> = emptyList(),
    val periodos: List<PeriodoLetivo> = emptyList(),
    val selectedPeriodo: PeriodoLetivo? = null,
    val streakDays: Int = 0,
    /** Erro sem dados para mostrar — ocupa a tela. */
    val error: String? = null,
    /** Falha ao atualizar, mas há dados salvos na tela. */
    val syncWarning: String? = null,
    val lastSyncAt: Long? = null,
    val isSyncing: Boolean = false,
    val detailMateriaId: String? = null,
    val verdictMateriaId: String? = null,
    val snackMessage: String? = null,
    val snackIsError: Boolean = false,
    val servers: List<Servidor> = emptyList(),
    val isSearchingServers: Boolean = false,
    val searchServersError: String? = null,
    val hasMoreServers: Boolean = false,
    /** Diários com falta nova desde a última vez que o aluno olhou. */
    val faltasNovas: Set<String> = emptySet(),
) {
    val isCurrentPeriodo: Boolean get() = selectedPeriodo != null && selectedPeriodo == periodos.firstOrNull()
}

class DashboardViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val academicRepository: AcademicRepository,
    private val faltasHistory: FaltasHistory,
    private val cacheStore: CacheStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var snackJob: Job? = null
    private var serversQuery: String = ""
    private var serversPage: Int = 1
    private var serversCampus: String? = null

    init {
        _uiState.update { it.copy(faltasNovas = cacheStore.unseenChanges) }
        loadJob = viewModelScope.launch { load() }
    }

    /** Tentar de novo a partir da tela de erro. */
    fun fetchData() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { load() }
    }

    /**
     * Cache primeiro (resposta instantânea, funciona offline), rede depois.
     * Retorna o erro da atualização pela rede, ou null se deu tudo certo.
     */
    private suspend fun load(): Throwable? {
        _uiState.update { it.copy(error = null, isLoading = it.materias.isEmpty()) }

        profileRepository.cached()?.let { p -> _uiState.update { it.copy(profile = p) } }

        val periodos = academicRepository.getPeriodosLetivos().getOrElse { e ->
            _uiState.update { it.copy(isLoading = false, error = e.userMessage()) }
            return e
        }
        val atual = periodos.firstOrNull()
        val selecionado = _uiState.value.selectedPeriodo?.takeIf { it in periodos } ?: atual
        _uiState.update { it.copy(periodos = periodos, selectedPeriodo = selecionado) }
        if (selecionado == null) {
            _uiState.update { it.copy(isLoading = false, error = "Nenhum período letivo encontrado no SUAP.") }
            return null
        }
        val isCurrent = selecionado == atual

        if (isCurrent && _uiState.value.materias.isEmpty()) {
            academicRepository.cachedBoletim(selecionado)?.let { showBoletim(it, isCurrent = true) }
        }

        val result = academicRepository.fetchBoletim(selecionado, isCurrent)
        // O usuário pode ter trocado de período enquanto a rede respondia
        if (_uiState.value.selectedPeriodo != selecionado) return null

        result.onSuccess { items ->
            if (isCurrent) {
                faltasHistory.registerSync(items.sumOf { it.numeroFaltas })
                detectarFaltasNovas(items)
            }
            showBoletim(items, isCurrent)
            _uiState.update { it.copy(syncWarning = null) }
        }.onFailure { e ->
            _uiState.update {
                if (it.materias.isNotEmpty()) it.copy(isLoading = false, syncWarning = "Sem atualizar: ${e.userMessage().lowercase().removeSuffix(".")}")
                else it.copy(isLoading = false, error = e.userMessage())
            }
        }

        // Perfil atualizado em paralelo ao fluxo principal; falha aqui não atrapalha nada
        viewModelScope.launch {
            runCatching { profileRepository.refresh() }
            profileRepository.cached()?.let { p -> _uiState.update { it.copy(profile = p) } }
        }
        return result.exceptionOrNull()
    }

    private fun detectarFaltasNovas(items: List<BoletimItem>) {
        val anteriores = cacheStore.lastFaltas
        if (anteriores.isNotEmpty()) {
            val novas = items
                .filter { item -> anteriores[item.codigoDiario]?.let { it < item.numeroFaltas } == true }
                .map { it.codigoDiario }
            if (novas.isNotEmpty()) cacheStore.unseenChanges = cacheStore.unseenChanges + novas
        }
        cacheStore.lastFaltas = items.associate { it.codigoDiario to it.numeroFaltas }
        _uiState.update { it.copy(faltasNovas = cacheStore.unseenChanges) }
    }

    /** O aluno viu a falta nova desta matéria (ou todas, com null). */
    fun marcarFaltasVistas(id: String? = null) {
        cacheStore.unseenChanges = if (id == null) emptySet() else cacheStore.unseenChanges - id
        _uiState.update { it.copy(faltasNovas = cacheStore.unseenChanges) }
    }

    private suspend fun showBoletim(items: List<BoletimItem>, isCurrent: Boolean) {
        // A grade salva é a do período corrente; semestres antigos ficam sem horário
        val horarios = if (isCurrent) academicRepository.getHorarios() else emptyMap()
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                materias = buildMaterias(items, horarios),
                streakDays = faltasHistory.streakDays(),
                lastSyncAt = if (isCurrent) academicRepository.lastSyncAt else null,
            )
        }
    }

    fun selectPeriodo(periodo: PeriodoLetivo) {
        if (periodo == _uiState.value.selectedPeriodo) return
        loadJob?.cancel()
        _uiState.update { it.copy(selectedPeriodo = periodo, materias = emptyList(), isLoading = true, error = null, syncWarning = null) }
        loadJob = viewModelScope.launch { load() }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val error = load()
            _uiState.update { it.copy(isSyncing = false) }
            showSnack(error?.userMessage() ?: "Boletim sincronizado com o SUAP.", isError = error != null)
        }
    }

    private fun showSnack(message: String, isError: Boolean = false) {
        snackJob?.cancel()
        snackJob = viewModelScope.launch {
            _uiState.update { it.copy(snackMessage = message, snackIsError = isError) }
            delay(3000)
            _uiState.update { it.copy(snackMessage = null) }
        }
    }

    fun openDetail(id: String) {
        if (id in _uiState.value.faltasNovas) marcarFaltasVistas(id)
        _uiState.update { it.copy(detailMateriaId = id) }
    }
    fun closeDetail() = _uiState.update { it.copy(detailMateriaId = null) }
    fun openVerdict(id: String) = _uiState.update { it.copy(verdictMateriaId = id) }
    fun closeVerdict() = _uiState.update { it.copy(verdictMateriaId = null) }

    /** A navegação para o login acontece na MainActivity, ao observar a sessão. */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun searchServidores(query: String) {
        serversQuery = query.trim()
        serversPage = 1
        if (serversQuery.isBlank()) {
            _uiState.update { it.copy(servers = emptyList(), searchServersError = null, isSearchingServers = false, hasMoreServers = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingServers = true, searchServersError = null) }
            // Primeiro no campus do aluno; sem resultado, na instituição toda
            serversCampus = _uiState.value.profile?.vinculo?.campus?.takeIf { it.isNotBlank() }
            var result = academicRepository.buscarServidores(serversQuery, serversCampus)
            if (result.getOrNull()?.results.isNullOrEmpty() && serversCampus != null) {
                serversCampus = null
                result = academicRepository.buscarServidores(serversQuery, null)
            }
            result.onSuccess { page ->
                _uiState.update { it.copy(servers = page.results, isSearchingServers = false, hasMoreServers = page.next != null) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSearchingServers = false, searchServersError = e.userMessage(), hasMoreServers = false) }
            }
        }
    }

    fun loadMoreServidores() {
        val state = _uiState.value
        if (state.isSearchingServers || !state.hasMoreServers) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingServers = true) }
            academicRepository.buscarServidores(serversQuery, serversCampus, serversPage + 1)
                .onSuccess { page ->
                    serversPage++
                    _uiState.update { it.copy(servers = it.servers + page.results, isSearchingServers = false, hasMoreServers = page.next != null) }
                }
                .onFailure { e -> _uiState.update { it.copy(isSearchingServers = false, searchServersError = e.userMessage()) } }
        }
    }
}
