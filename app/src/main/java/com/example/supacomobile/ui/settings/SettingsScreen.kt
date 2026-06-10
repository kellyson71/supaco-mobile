package com.example.supacomobile.ui.settings

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.supacomobile.data.local.BiometricChoice
import com.example.supacomobile.data.local.NotifyLevel
import com.example.supacomobile.data.local.SettingsManager
import com.example.supacomobile.data.local.ThemeMode
import com.example.supacomobile.notifications.FaltasWorker
import com.example.supacomobile.ui.components.OrgShape
import com.example.supacomobile.ui.components.ShapeContainer
import com.example.supacomobile.widget.SupacoWidgetReceiver
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: SettingsManager = koinInject(),
) {
    val context = LocalContext.current
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle()
    val biometricChoice by settings.biometricChoice.collectAsStateWithLifecycle()

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    var showBiometricDialog by remember { mutableStateOf(false) }

    val notificationsEnabled by settings.notificationsEnabled.collectAsStateWithLifecycle()
    val notifyLevel by settings.notifyLevel.collectAsStateWithLifecycle()
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        settings.setNotificationsEnabled(granted)
        if (granted) FaltasWorker.schedule(context) else FaltasWorker.cancel(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Aparência ──
            SectionHeader("Aparência")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SettingIcon(Icons.Rounded.Palette, OrgShape.FLOWER, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        Column {
                            Text("Tema", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Escolha como o Supaco aparece",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeOption.entries.forEachIndexed { i, option ->
                            SegmentedButton(
                                selected = themeMode == option.mode,
                                onClick = { settings.setThemeMode(option.mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = i, count = ThemeOption.entries.size),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = themeMode == option.mode) {
                                        Icon(option.icon, null, modifier = Modifier.size(16.dp))
                                    }
                                },
                            ) {
                                Text(option.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingRow(
                        icon = Icons.Rounded.Colorize,
                        shape = OrgShape.COOKIE,
                        iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                        title = "Material You",
                        subtitle = "Cores dinâmicas do seu papel de parede",
                        trailing = {
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = { settings.setDynamicColor(it) },
                                thumbContent = if (dynamicColor) {
                                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                } else null,
                            )
                        },
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    var showLangDialog by remember { mutableStateOf(false) }
                    val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                    val currentLang = localeManager.applicationLocales.toLanguageTags().ifEmpty { "pt-BR" }
                    
                    SettingRow(
                        icon = Icons.Rounded.Language,
                        shape = OrgShape.PEBBLE,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        title = "Idioma do aplicativo",
                        subtitle = if (currentLang.startsWith("en")) "English (United States)" else "Português (Brasil)",
                        onClick = { showLangDialog = true }
                    )
                    
                    if (showLangDialog) {
                        AlertDialog(
                            onDismissRequest = { showLangDialog = false },
                            icon = { Icon(Icons.Rounded.Language, null) },
                            title = { Text("Escolha o idioma") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            localeManager.applicationLocales = android.os.LocaleList.forLanguageTags("pt-BR")
                                            showLangDialog = false
                                        }.padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(selected = !currentLang.startsWith("en"), onClick = {
                                            localeManager.applicationLocales = android.os.LocaleList.forLanguageTags("pt-BR")
                                            showLangDialog = false
                                        })
                                        Text("Português (Brasil)", modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            localeManager.applicationLocales = android.os.LocaleList.forLanguageTags("en-US")
                                            showLangDialog = false
                                        }.padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(selected = currentLang.startsWith("en"), onClick = {
                                            localeManager.applicationLocales = android.os.LocaleList.forLanguageTags("en-US")
                                            showLangDialog = false
                                        })
                                        Text("English (United States)", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showLangDialog = false }) { Text("Fechar") }
                            }
                        )
                    }
                }
            }

            // ── Segurança ──
            SectionHeader("Segurança")
            SettingsCard {
                SettingRow(
                    icon = Icons.Rounded.Fingerprint,
                    shape = OrgShape.CLOVER,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "Bloqueio por biometria",
                    subtitle = when {
                        !biometricAvailable -> "Indisponível neste aparelho"
                        biometricChoice == BiometricChoice.ENABLED -> "Pede biometria ao abrir o app"
                        else -> "Desativado"
                    },
                    trailing = {
                        Switch(
                            checked = biometricChoice == BiometricChoice.ENABLED,
                            enabled = biometricAvailable,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    settings.setBiometricChoice(BiometricChoice.ENABLED)
                                } else {
                                    showBiometricDialog = true
                                }
                            },
                            thumbContent = if (biometricChoice == BiometricChoice.ENABLED) {
                                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    },
                )
            }

            // ── Notificações ──
            SectionHeader("Notificações")
            SettingsCard {
                SettingRow(
                    icon = Icons.Rounded.NotificationsActive,
                    shape = OrgShape.FLOWER,
                    iconBg = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.onErrorContainer,
                    title = "Alertas de perigo",
                    subtitle = if (notificationsEnabled) "Avisa quando uma matéria fica perigosa de faltar"
                               else "Sem alertas — boa sorte aí",
                    trailing = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        settings.setNotificationsEnabled(true)
                                        FaltasWorker.schedule(context)
                                    }
                                } else {
                                    settings.setNotificationsEnabled(false)
                                    FaltasWorker.cancel(context)
                                }
                            },
                            thumbContent = if (notificationsEnabled) {
                                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    },
                )

                if (notificationsEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Avisar a partir de",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = notifyLevel == NotifyLevel.WARN,
                                onClick = { settings.setNotifyLevel(NotifyLevel.WARN) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            ) { Text("Cedo (amarelo)", style = MaterialTheme.typography.labelLarge) }
                            SegmentedButton(
                                selected = notifyLevel == NotifyLevel.LAST,
                                onClick = { settings.setNotifyLevel(NotifyLevel.LAST) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            ) { Text("Última falta", style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                }
            }

            // ── Android ──
            SectionHeader("Integração com o Android")
            SettingsCard {
                SettingRow(
                    icon = Icons.Rounded.Widgets,
                    shape = OrgShape.PEBBLE,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Adicionar widget",
                    subtitle = "Suas faltas direto na tela inicial",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        val manager = AppWidgetManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
                            manager.requestPinAppWidget(
                                ComponentName(context, SupacoWidgetReceiver::class.java),
                                null, null,
                            )
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(
                    icon = Icons.Rounded.Notifications,
                    shape = OrgShape.FLOWER,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Notificações",
                    subtitle = "Gerenciar nas configurações do sistema",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(
                    icon = Icons.Rounded.AppShortcut,
                    shape = OrgShape.COOKIE,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "Atalhos rápidos",
                    subtitle = "Segure o ícone do app na tela inicial",
                )
            }

            // ── Sobre ──
            SectionHeader("Sobre")
            SettingsCard {
                SettingRow(
                    icon = Icons.Rounded.School,
                    shape = OrgShape.FLOWER,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "SUAP · IFRN",
                    subtitle = "Os dados vêm direto do suap.ifrn.edu.br",
                    trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://suap.ifrn.edu.br")))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(
                    icon = Icons.Rounded.Info,
                    shape = OrgShape.PEBBLE,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Supaco Mobile",
                    subtitle = "Versão ${remember { appVersion(context) }} · feito pra quem vive no limite",
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            icon = { Icon(Icons.Rounded.Fingerprint, null) },
            title = { Text("Desativar biometria?") },
            text = { Text("Qualquer pessoa com o seu celular vai poder ver quantas vezes você faltou. Tem certeza?") },
            confirmButton = {
                TextButton(onClick = {
                    settings.setBiometricChoice(BiometricChoice.DISABLED)
                    showBiometricDialog = false
                }) { Text("Desativar") }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

private enum class ThemeOption(val label: String, val icon: ImageVector, val mode: ThemeMode) {
    SYSTEM("Sistema", Icons.Rounded.PhoneAndroid, ThemeMode.SYSTEM),
    LIGHT("Claro", Icons.Rounded.LightMode, ThemeMode.LIGHT),
    DARK("Escuro", Icons.Rounded.DarkMode, ThemeMode.DARK),
}

private fun appVersion(context: android.content.Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        content = content,
    )
}

@Composable
private fun SettingIcon(icon: ImageVector, shape: OrgShape, bg: Color, tint: Color) {
    ShapeContainer(shape = shape, size = 42.dp, containerColor = bg, contentColor = tint) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    shape: OrgShape,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier.fillMaxWidth()
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = Color.Transparent,
    ) {
        Row(
            modifier = rowModifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingIcon(icon, shape, iconBg, iconTint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
    }
}
