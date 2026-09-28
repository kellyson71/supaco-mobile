package io.github.kellyson71.supaco.data.session

import android.content.Context
import androidx.glance.appwidget.updateAll
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.local.CacheStore
import io.github.kellyson71.supaco.data.local.FaltasHistory
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.data.local.TokenManager
import io.github.kellyson71.supaco.notifications.FaltasNotifier
import io.github.kellyson71.supaco.notifications.FaltasWorker
import io.github.kellyson71.supaco.widget.HorariosWidget
import io.github.kellyson71.supaco.widget.SupacoVerdictWidget
import io.github.kellyson71.supaco.widget.SupacoWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SessionState { LOGGED_IN, LOGGED_OUT, EXPIRED }

/**
 * Dono do ciclo de vida da sessão. Todo caminho de saída (logout, sessão expirada,
 * "entrar com outra conta" na tela bloqueada) passa por [wipe], para que nenhum dado
 * do usuário anterior sobreviva em cache, widget, notificação ou tarefa agendada.
 */
class SessionManager(
    private val context: Context,
    private val tokenManager: TokenManager,
    private val database: AppDatabase,
    private val cacheStore: CacheStore,
    private val faltasHistory: FaltasHistory,
    private val settings: SettingsManager,
    private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow(
        if (tokenManager.getAccessToken() != null) SessionState.LOGGED_IN else SessionState.LOGGED_OUT
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun onLoggedIn() {
        _state.value = SessionState.LOGGED_IN
        if (settings.notificationsEnabled.value) FaltasWorker.schedule(context)
    }

    /** Logout pedido pelo usuário. */
    suspend fun logout() {
        withContext(Dispatchers.IO) { wipe() }
        _state.value = SessionState.LOGGED_OUT
    }

    /** Chamado pela camada de rede quando o refresh token é recusado. Seguro em qualquer thread. */
    fun expire() {
        if (_state.value != SessionState.LOGGED_IN) return
        _state.value = SessionState.EXPIRED
        appScope.launch(Dispatchers.IO) { wipe() }
    }

    /** A tela de login já mostrou o aviso de sessão expirada. */
    fun acknowledgeExpired() {
        if (_state.value == SessionState.EXPIRED) _state.value = SessionState.LOGGED_OUT
    }

    private suspend fun wipe() {
        tokenManager.clear()
        database.clearAllTables()
        cacheStore.clear()
        faltasHistory.clear()
        FaltasNotifier.reset(context)
        FaltasWorker.cancel(context)
        runCatching {
            SupacoWidget().updateAll(context)
            SupacoVerdictWidget().updateAll(context)
            HorariosWidget().updateAll(context)
        }
    }
}
