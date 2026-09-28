package io.github.kellyson71.supaco.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kellyson71.supaco.data.remote.userMessage
import io.github.kellyson71.supaco.data.repository.AuthRepository
import io.github.kellyson71.supaco.data.session.SessionManager
import io.github.kellyson71.supaco.data.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            error = if (sessionManager.state.value == SessionState.EXPIRED) {
                "Sua sessão no SUAP expirou. Entre de novo."
            } else null
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        sessionManager.acknowledgeExpired()
    }

    fun login(matricula: String, senha: String) {
        if (matricula.isBlank() || senha.isBlank()) {
            _uiState.update { it.copy(error = "Preencha matrícula e senha.", isLoading = false) }
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(matricula, senha)
                .onSuccess { _uiState.update { it.copy(isLoading = false, isSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.userMessage(isLogin = true)) } }
        }
    }

    fun resetState() {
        _uiState.update { AuthUiState() }
    }
}
