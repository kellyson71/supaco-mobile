package io.github.kellyson71.supaco

import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import io.github.kellyson71.supaco.data.session.SessionManager
import io.github.kellyson71.supaco.data.session.SessionState
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kellyson71.supaco.data.local.BiometricChoice
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.data.local.ThemeMode
import io.github.kellyson71.supaco.theme.AppBackground
import io.github.kellyson71.supaco.theme.SupacoMobileTheme
import io.github.kellyson71.supaco.theme.resolvePalette
import io.github.kellyson71.supaco.ui.auth.LoginScreen
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.DashboardScreen
import io.github.kellyson71.supaco.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject

@Serializable
object LoginRoute

@Serializable
object DashboardRoute

@Serializable
object SettingsRoute

private enum class Gate { ASK_BIOMETRIC, LOCKED, UNLOCKED }

/** Tempo em background depois do qual a biometria é pedida de novo. */
private const val RELOCK_AFTER_MS = 5 * 60 * 1000L

class MainActivity : FragmentActivity() {

    private val settings: SettingsManager by inject()
    private val sessionManager: SessionManager by inject()
    private var currentShortcutDest by mutableStateOf<String?>(null)
    private var gate by mutableStateOf(Gate.UNLOCKED)
    private var backgroundedAt = 0L

    companion object {
        const val EXTRA_DEST = "dest"
    }

    private fun canUseBiometric(): Boolean =
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun initialGate(): Gate {
        val loggedIn = sessionManager.state.value == SessionState.LOGGED_IN
        return when {
            !loggedIn || !canUseBiometric() -> Gate.UNLOCKED
            settings.biometricChoice.value == BiometricChoice.ENABLED -> Gate.LOCKED
            settings.biometricChoice.value == BiometricChoice.UNSET -> Gate.ASK_BIOMETRIC
            else -> Gate.UNLOCKED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        currentShortcutDest = intent.getStringExtra(EXTRA_DEST)
        if (savedInstanceState == null) gate = initialGate()

        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle()
            val paletteId by settings.paletteId.collectAsStateWithLifecycle()
            val customSeed by settings.customSeed.collectAsStateWithLifecycle()
            val customStyle by settings.customStyle.collectAsStateWithLifecycle()
            val pureBlack by settings.pureBlack.collectAsStateWithLifecycle()
            val bgEnabled by settings.backgroundEnabled.collectAsStateWithLifecycle()
            val bgOpacity by settings.backgroundOpacity.collectAsStateWithLifecycle()
            val bgVersion by settings.backgroundVersion.collectAsStateWithLifecycle()
            val modoSerio by settings.modoSerio.collectAsStateWithLifecycle()
            val sessionState by sessionManager.state.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val palette = remember(paletteId, customSeed, customStyle, pureBlack) {
                resolvePalette(paletteId, customSeed, customStyle, pureBlack)
            }

            // Fundo decodificado fora da main thread (a imagem já é salva reduzida)
            val backgroundBitmap by produceState<ImageBitmap?>(null, bgEnabled, bgVersion) {
                value = if (bgEnabled) {
                    withContext(Dispatchers.IO) { settings.loadBackgroundBitmap()?.asImageBitmap() }
                } else null
            }
            val background = backgroundBitmap?.let { AppBackground(it, bgOpacity) }

            CompositionLocalProvider(LocalModoSerio provides modoSerio) {
                SupacoMobileTheme(
                    darkTheme = darkTheme,
                    dynamicColor = dynamicColor,
                    seedColor = palette.seed,
                    paletteStyle = palette.style,
                    amoled = palette.amoled,
                    background = background,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when (gate) {
                            Gate.ASK_BIOMETRIC -> BiometricAskScreen(
                                onEnable = {
                                    showBiometricPrompt(
                                        onSuccess = {
                                            settings.setBiometricChoice(BiometricChoice.ENABLED)
                                            gate = Gate.UNLOCKED
                                        },
                                        onError = {
                                            settings.setBiometricChoice(BiometricChoice.DISABLED)
                                            Toast.makeText(this, "Não foi possível ativar a biometria.", Toast.LENGTH_SHORT).show()
                                            gate = Gate.UNLOCKED
                                        },
                                    )
                                },
                                onSkip = {
                                    settings.setBiometricChoice(BiometricChoice.DISABLED)
                                    gate = Gate.UNLOCKED
                                },
                            )

                            Gate.LOCKED -> {
                                LaunchedEffect(Unit) {
                                    showBiometricPrompt(onSuccess = { gate = Gate.UNLOCKED }, onError = {})
                                }
                                LockedScreen(
                                    onRetry = {
                                        showBiometricPrompt(onSuccess = { gate = Gate.UNLOCKED }, onError = {})
                                    },
                                    onUseLogin = {
                                        // Outra pessoa pode estar entrando: apaga tudo da sessão atual
                                        lifecycleScope.launch {
                                            sessionManager.logout()
                                            gate = Gate.UNLOCKED
                                        }
                                    },
                                )
                            }

                            Gate.UNLOCKED -> SupacoApp(
                                sessionState = sessionState,
                                initialDest = currentShortcutDest,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        val away = backgroundedAt > 0 && SystemClock.elapsedRealtime() - backgroundedAt > RELOCK_AFTER_MS
        if (away &&
            gate == Gate.UNLOCKED &&
            sessionManager.state.value == SessionState.LOGGED_IN &&
            settings.biometricChoice.value == BiometricChoice.ENABLED &&
            canUseBiometric()
        ) {
            gate = Gate.LOCKED
        }
        backgroundedAt = 0L
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloqueie o Supaco")
            .setSubtitle("Use sua biometria para acessar seus dados")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val dest = intent.getStringExtra(EXTRA_DEST)
        if (dest != null) {
            currentShortcutDest = dest
        }
    }
}

@Composable
private fun BiometricAskScreen(onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ShapeContainer(
            shape = OrgShape.FLOWER,
            size = 96.dp,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Proteger com biometria?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Suas faltas são assunto sério. Quer pedir a digital ou o desbloqueio do celular toda vez que abrir o app?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ativar biometria")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Agora não")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Você pode mudar isso depois nas configurações.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LockedScreen(onRetry: () -> Unit, onUseLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ShapeContainer(
            shape = OrgShape.COOKIE,
            size = 96.dp,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("Supaco bloqueado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Use sua biometria para continuar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Desbloquear")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onUseLogin) {
            Text("Entrar com matrícula e senha")
        }
    }
}

@Composable
fun SupacoApp(sessionState: SessionState, initialDest: String? = null) {
    val navController = rememberNavController()
    val loggedIn = sessionState == SessionState.LOGGED_IN

    // Sessão encerrada (logout, refresh recusado): volta ao login de qualquer tela
    LaunchedEffect(loggedIn) {
        if (!loggedIn && navController.currentDestination?.hasRoute(LoginRoute::class) == false) {
            navController.navigate(LoginRoute) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    // Fixado na primeira composição: mudanças de sessão navegam pelo efeito acima
    val startDestination = remember { if (loggedIn) DashboardRoute else LoginRoute }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(DashboardRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<DashboardRoute> {
            DashboardScreen(
                initialDest = initialDest,
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
