package io.github.kellyson71.supaco.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.notifications.FaltasNotifier
import io.github.kellyson71.supaco.notifications.FaltasWorker
import org.koin.compose.koinInject
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kellyson71.supaco.ui.dashboard.components.MateriaDetailSheet
import io.github.kellyson71.supaco.ui.dashboard.components.SpeedDialAction
import io.github.kellyson71.supaco.ui.dashboard.components.SpeedDialFab
import io.github.kellyson71.supaco.ui.dashboard.components.VerdictOverlay
import io.github.kellyson71.supaco.ui.dashboard.tabs.*
import org.koin.androidx.compose.koinViewModel

private enum class Tab(
    val label: String,
    val icon: ImageVector,
    val iconFilled: ImageVector,
    val index: Int,
) {
    INICIO("Início", Icons.Rounded.Home, Icons.Rounded.Home, 0),
    MATERIAS("Matérias", Icons.Rounded.AutoStories, Icons.Rounded.AutoStories, 1),
    HORARIOS("Horários", Icons.Rounded.CalendarMonth, Icons.Rounded.CalendarMonth, 2),
    PERFIL("Eu", Icons.Rounded.Mood, Icons.Rounded.Mood, 3),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit = {},
    initialDest: String? = null,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember {
        mutableStateOf(
            when (initialDest) {
                "horarios" -> Tab.HORARIOS
                "materias" -> Tab.MATERIAS
                else -> Tab.INICIO
            }
        )
    }

    var showAchievements by remember { mutableStateOf(false) }
    var showSearchServidores by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings: SettingsManager = koinInject()

    // Android 13+: explica e pede a permissão de notificação uma única vez,
    // depois que o aluno já viu os próprios dados.
    var showNotifPrompt by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        settings.setNotificationsEnabled(granted)
        if (granted) FaltasWorker.schedule(context) else FaltasWorker.cancel(context)
    }
    LaunchedEffect(uiState.materias.isNotEmpty()) {
        if (uiState.materias.isNotEmpty() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !settings.notificationPermissionAsked &&
            settings.notificationsEnabled.value &&
            !FaltasNotifier.hasPermission(context)
        ) {
            showNotifPrompt = true
        }
    }
    if (showNotifPrompt) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.NotificationsActive, contentDescription = null) },
            title = { Text("Avisos de faltas") },
            text = {
                Text("O Supaco pode checar seu boletim algumas vezes por dia e avisar quando uma matéria estiver perto do limite de faltas. Nada sai do seu celular além da consulta ao SUAP.")
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.notificationPermissionAsked = true
                    showNotifPrompt = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Ativar avisos") }
            },
            dismissButton = {
                TextButton(onClick = {
                    settings.notificationPermissionAsked = true
                    settings.setNotificationsEnabled(false)
                    FaltasWorker.cancel(context)
                    showNotifPrompt = false
                }) { Text("Agora não") }
            },
        )
    }


    // Shortcut "posso faltar hoje?" — open verdict as soon as data arrives
    var verdictShortcutHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.materias, initialDest) {
        if (initialDest == "verdict" && !verdictShortcutHandled && uiState.materias.isNotEmpty()) {
            verdictShortcutHandled = true
            val alvo = vereditoDoDia(uiState.materias) ?: materiaMaisCritica(uiState.materias)
            alvo?.let { viewModel.openVerdict(it.codigoDiario) }
        }
    }

    LaunchedEffect(initialDest) {
        initialDest?.let { dest ->
            when (dest) {
                "horarios" -> currentTab = Tab.HORARIOS
                "materias" -> currentTab = Tab.MATERIAS
                "verdict" -> {
                    verdictShortcutHandled = false
                }
            }
        }
    }

    val detailMateria = uiState.materias.find { it.codigoDiario == uiState.detailMateriaId }
    val verdictMateria = uiState.materias.find { it.codigoDiario == uiState.verdictMateriaId }

    if (showAchievements) {
        io.github.kellyson71.supaco.ui.conquistas.ConquistasScreen(
            materias = uiState.materias,
            onBack = { showAchievements = false }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = { Icon(if (selected) tab.iconFilled else tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                AnimatedContent(
                    targetState = currentTab,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    transitionSpec = {
                        val forward = targetState.index > initialState.index
                        if (forward) {
                            slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(200)) togetherWith
                                slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200))
                        } else {
                            slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(200)) togetherWith
                                slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(200))
                        }
                    },
                    label = "tab_transition",
                ) { tab ->
                    when (tab) {
                        Tab.INICIO -> {
                            if (uiState.isLoading && uiState.profile == null) {
                                LoadingScreen()
                            } else if (uiState.error != null) {
                                ErrorScreen(
                                    message = uiState.error!!,
                                    onRetry = { viewModel.fetchData() },
                                )
                            } else {
                                HomeTab(
                                    nomeUsual = uiState.profile?.nomeUsual ?: "aluno",
                                    materias = uiState.materias,
                                    isLoading = uiState.isLoading || uiState.isSyncing,
                                    lastSyncAt = uiState.lastSyncAt,
                                    syncWarning = uiState.syncWarning,
                                    faltasNovas = uiState.faltasNovas,
                                    onDismissFaltasNovas = { viewModel.marcarFaltasVistas() },
                                    onSync = { viewModel.sync() },
                                    onOpenDetail = { viewModel.openDetail(it) },
                                    onAskVerdict = { viewModel.openVerdict(it) },
                                )
                            }
                        }
                        Tab.MATERIAS -> if (uiState.error != null && uiState.materias.isEmpty() && uiState.periodos.isEmpty()) {
                            ErrorScreen(message = uiState.error!!, onRetry = { viewModel.fetchData() })
                        } else MateriasTab(
                            materias = uiState.materias,
                            periodos = uiState.periodos,
                            selectedPeriodo = uiState.selectedPeriodo,
                            onSelectPeriodo = { viewModel.selectPeriodo(it) },
                            onOpenDetail = { viewModel.openDetail(it) },
                        )
                        Tab.HORARIOS -> if (uiState.error != null && uiState.materias.isEmpty()) {
                            ErrorScreen(message = uiState.error!!, onRetry = { viewModel.fetchData() })
                        } else HorariosTab(
                            materias = uiState.materias,
                            onOpenDetail = { viewModel.openDetail(it) },
                        )
                        Tab.PERFIL -> PerfilTab(
                            matricula = uiState.profile?.matricula ?: "—",
                            nomeUsual = uiState.profile?.nomeUsual ?: "Aluno",
                            materias = uiState.materias,
                            streakDays = uiState.streakDays,
                            onSearchServidores = { showSearchServidores = true },
                            onDownloadDeclaration = {
                                val matricula = uiState.profile?.matricula ?: ""
                                if (matricula.isNotEmpty()) {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://suap.ifrn.edu.br/edu/aluno/$matricula/?tab=documentos")
                                        )
                                    )
                                }
                            },
                            onOpenSettings = onOpenSettings,
                            onViewAchievements = { showAchievements = true },
                            onLogout = { viewModel.logout() },
                        )

                    }
                }
            }

            // Speed-dial FAB (visible on all tabs except Perfil)
            val context = androidx.compose.ui.platform.LocalContext.current
            if (currentTab != Tab.PERFIL) {
                SpeedDialFab(
                    onSync = { viewModel.sync() },
                    syncing = uiState.isSyncing,
                    actions = listOf(
                        SpeedDialAction(
                            icon = Icons.Rounded.EmojiEvents,
                            label = "Ver Conquistas",
                            onClick = { showAchievements = true },
                        ),
                        SpeedDialAction(
                            icon = Icons.Rounded.Language,
                            label = "Abrir SUAP no navegador",
                            onClick = {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://suap.ifrn.edu.br"),
                                    )
                                )
                            },
                        ),
                        SpeedDialAction(
                            icon = Icons.Rounded.Settings,
                            label = "Configurações",
                            onClick = onOpenSettings,
                        ),
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 120.dp),
                )
            }

            // Alert customizado de sincronização
            AnimatedVisibility(
                visible = uiState.snackMessage != null,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn() + scaleIn(initialScale = 0.9f),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .padding(horizontal = 24.dp),
            ) {
                val msg = uiState.snackMessage ?: ""
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = MaterialTheme.shapes.extraLarge,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.inversePrimary,
                        )
                        Text(msg, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // Verdict overlay
    verdictMateria?.let {
        VerdictOverlay(
            materia = it,
            onClose = { viewModel.closeVerdict() },
        )
    }

    // Detail bottom sheet
    detailMateria?.let {
        MateriaDetailSheet(
            materia = it,
            onClose = { viewModel.closeDetail() },
        )
    }

    if (showSearchServidores) {
        io.github.kellyson71.supaco.ui.dashboard.components.ServidorSearchSheet(
            servers = uiState.servers,
            isSearching = uiState.isSearchingServers,
            error = uiState.searchServersError,
            onSearch = { viewModel.searchServidores(it) },
            onClose = { showSearchServidores = false },
            hasMore = uiState.hasMoreServers,
            onLoadMore = { viewModel.loadMoreServidores() },
        )
    }
}



@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Carregando seus dados…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tentar novamente")
            }
        }
    }
}
