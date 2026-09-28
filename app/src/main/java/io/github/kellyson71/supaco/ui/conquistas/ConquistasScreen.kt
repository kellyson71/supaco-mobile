package io.github.kellyson71.supaco.ui.conquistas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConquistasScreen(
    materias: List<MateriaDisplay>,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val achievements = remember(materias) { computeAchievements(materias) }
    val unlockedCount = achievements.count { it.unlocked }
    val progress = if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conquistas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card showing progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Seu Progresso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$unlockedCount de ${achievements.size} Conquistas Desbloqueadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    // Enche ao abrir; completa, ganha um brilho que passa de tempos em tempos
                    val reduce = LocalReduceMotion.current
                    val fill = remember { Animatable(if (reduce) progress else 0f) }
                    LaunchedEffect(progress) { fill.animateTo(progress, tween(900, delayMillis = 200, easing = Motion.EmphasizedDecelerate)) }
                    val complete = progress >= 1f && !reduce
                    val sheen = if (complete) {
                        rememberInfiniteTransition(label = "complete").animateFloat(
                            -0.3f, 1.3f, infiniteRepeatable(tween(1800, delayMillis = 800)), label = "complete_sheen",
                        ).value
                    } else -1f
                    val sheenColor = MaterialTheme.colorScheme.onPrimary
                    LinearProgressIndicator(
                        progress = { fill.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .drawWithContent {
                                drawContent()
                                if (sheen in 0f..1f) {
                                    val x = size.width * sheen
                                    drawRect(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, sheenColor.copy(alpha = 0.6f), Color.Transparent),
                                            startX = x - 40f,
                                            endX = x + 40f,
                                        ),
                                    )
                                }
                            },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Grid of achievements
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(achievements, key = { _, a -> a.id }) { index, ach ->
                    AchievementItemRow(ach, Modifier.enterOnce(index, stepMs = 40))
                }
            }
        }
    }
}

@Composable
private fun AchievementItemRow(ach: Achievement, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    val showContent = ach.unlocked || !ach.isSecret
    val containerColor = if (ach.unlocked) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val alpha = if (ach.unlocked) 1f else 0.38f

    OutlinedCard(
        // Tocar numa conquista desbloqueada: a forma dá um giro completo
        onClick = {
            if (ach.unlocked) {
                haptics.tick()
                scope.launch {
                    spin.snapTo(0f)
                    spin.animateTo(360f, spring(dampingRatio = 0.55f, stiffness = 120f))
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = CardDefaults.outlinedCardBorder(enabled = ach.unlocked)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShapeContainer(
                shape = if (ach.unlocked) OrgShape.FLOWER else OrgShape.PEBBLE,
                size = 48.dp,
                extraRotation = spin.value,
                morph = true,
                containerColor = if (ach.unlocked) ach.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (ach.unlocked) ach.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ) {
                Icon(
                    if (ach.unlocked) ach.icon else Icons.Rounded.Lock,
                    null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (showContent) {
                    Text(
                        ach.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    )
                } else {
                    // Segredo: letras "decodificando" sem nunca revelar o nome
                    ScrambleText(
                        length = ach.title.length,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (showContent) ach.description else ach.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
            if (ach.unlocked) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Desbloqueada",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ScrambleText(length: Int, style: androidx.compose.ui.text.TextStyle, color: Color) {
    if (LocalReduceMotion.current) {
        Text("Conquista Oculta", style = style, fontWeight = FontWeight.Bold, color = color)
        return
    }
    val glyphs = "ABCDEFGHJKLMNPQRSTUVWXYZ#%&?!<>"
    val text by produceState("Conquista Oculta") {
        while (true) {
            value = String(CharArray(length) { if (kotlin.random.Random.nextInt(6) == 0) ' ' else glyphs.random() })
            kotlinx.coroutines.delay(140)
        }
    }
    Text(
        text,
        style = style,
        fontWeight = FontWeight.Bold,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = color,
        maxLines = 1,
        modifier = Modifier.semantics { contentDescription = "Conquista oculta" },
    )
}
