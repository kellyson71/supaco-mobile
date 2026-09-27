package io.github.kellyson71.supaco.ui.settings

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kellyson71.supaco.theme.AppPaletteStyle
import io.github.kellyson71.supaco.theme.CUSTOM_PALETTE_ID
import io.github.kellyson71.supaco.theme.ThemePresets
import io.github.kellyson71.supaco.data.local.BiometricChoice
import io.github.kellyson71.supaco.data.local.NotifyLevel
import io.github.kellyson71.supaco.data.local.SettingsManager
import io.github.kellyson71.supaco.data.local.ThemeMode
import io.github.kellyson71.supaco.notifications.FaltasWorker
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.widget.SupacoWidgetReceiver
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
    val paletteId by settings.paletteId.collectAsStateWithLifecycle()
    val customSeed by settings.customSeed.collectAsStateWithLifecycle()
    val customStyle by settings.customStyle.collectAsStateWithLifecycle()
    val pureBlack by settings.pureBlack.collectAsStateWithLifecycle()
    val bgEnabled by settings.backgroundEnabled.collectAsStateWithLifecycle()
    val bgOpacity by settings.backgroundOpacity.collectAsStateWithLifecycle()
    val biometricChoice by settings.biometricChoice.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) settings.saveBackgroundFromUri(uri) }

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

            // ── Cores ──
            SectionHeader("Cores")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SettingIcon(Icons.Rounded.ColorLens, OrgShape.COOKIE, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        Column {
                            Text("Tema de cores", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (dynamicColor) "Desative o Material You para escolher"
                                else "Escolha uma paleta ou crie a sua",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    PalettePicker(
                        selectedId = paletteId,
                        customSeed = Color(customSeed),
                        enabled = !dynamicColor,
                        onSelectPreset = { settings.setPaletteId(it) },
                        onSelectCustom = { settings.setPaletteId(CUSTOM_PALETTE_ID) },
                    )

                    if (!dynamicColor && paletteId == CUSTOM_PALETTE_ID) {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Cor base",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        HueSlider(
                            seed = Color(customSeed),
                            onSeedChange = { settings.setCustomSeed(it) },
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Estilo da paleta",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        StylePicker(
                            selectedOrdinal = customStyle,
                            onSelect = { settings.setCustomStyle(it) },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(
                    icon = Icons.Rounded.Contrast,
                    shape = OrgShape.PEBBLE,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Preto puro (AMOLED)",
                    subtitle = "Fundo 100% preto no modo escuro — economiza bateria em telas OLED",
                    trailing = {
                        Switch(
                            checked = pureBlack,
                            onCheckedChange = { settings.setPureBlack(it) },
                            thumbContent = if (pureBlack) {
                                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    },
                )
            }

            // ── Fundo de tela ──
            SectionHeader("Fundo de tela")
            SettingsCard {
                SettingRow(
                    icon = Icons.Rounded.Image,
                    shape = OrgShape.FLOWER,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Imagem de fundo",
                    subtitle = if (bgEnabled && settings.hasBackgroundFile()) "Toque para trocar a imagem"
                               else "Use uma foto sua como plano de fundo",
                    trailing = {
                        Switch(
                            checked = bgEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (settings.hasBackgroundFile()) settings.setBackgroundEnabled(true)
                                    else photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    settings.setBackgroundEnabled(false)
                                }
                            },
                            thumbContent = if (bgEnabled) {
                                { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    },
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )

                if (bgEnabled && settings.hasBackgroundFile()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Intensidade da foto",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(bgOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = bgOpacity,
                            onValueChange = { settings.setBackgroundOpacity(it) },
                            valueRange = 0f..1f,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { settings.clearBackground() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remover imagem")
                        }
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

private val RainbowSpectrum = listOf(
    Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00),
    Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF3D5AFE),
    Color(0xFFD500F9), Color(0xFFFF1744),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PalettePicker(
    selectedId: String,
    customSeed: Color,
    enabled: Boolean,
    onSelectPreset: (String) -> Unit,
    onSelectCustom: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemePresets.forEach { preset ->
            Swatch(
                brush = Brush.linearGradient(listOf(preset.swatch, preset.swatch)),
                label = preset.label,
                selected = enabled && selectedId == preset.id,
                enabled = enabled,
                onClick = { onSelectPreset(preset.id) },
            )
        }
        Swatch(
            brush = if (selectedId == CUSTOM_PALETTE_ID)
                Brush.linearGradient(listOf(customSeed, customSeed))
            else Brush.sweepGradient(RainbowSpectrum),
            label = "Você",
            selected = enabled && selectedId == CUSTOM_PALETTE_ID,
            enabled = enabled,
            onClick = onSelectCustom,
        )
    }
}

@Composable
private fun Swatch(
    brush: Brush,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(brush)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
                .alpha(if (enabled) 1f else 0.4f),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun HueSlider(seed: Color, onSeedChange: (Int) -> Unit) {
    val hsv = remember(seed) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(seed.toArgb(), it) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Brush.horizontalGradient(RainbowSpectrum)),
    )
    Slider(
        value = hsv[0],
        onValueChange = { hue ->
            onSeedChange(Color.hsv(hue, 0.65f, 0.85f).toArgb())
        },
        valueRange = 0f..360f,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StylePicker(selectedOrdinal: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppPaletteStyle.entries.forEachIndexed { i, style ->
            FilterChip(
                selected = selectedOrdinal == i,
                onClick = { onSelect(i) },
                label = { Text(style.label) },
            )
        }
    }
}
