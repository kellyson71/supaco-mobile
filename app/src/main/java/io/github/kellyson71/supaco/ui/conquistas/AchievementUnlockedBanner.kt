package io.github.kellyson71.supaco.ui.conquistas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import kotlinx.coroutines.delay

/**
 * Anúncio de conquista: desce do topo, a pedra cinza se transforma numa flor com a
 * cor da conquista, e some sozinho depois de alguns segundos.
 */
@Composable
fun AchievementUnlockedBanner(
    achievement: Achievement?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Mantém o último valor durante a animação de saída
    var shown by remember { mutableStateOf(achievement) }
    if (achievement != null) shown = achievement
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current

    AnimatedVisibility(
        visible = achievement != null,
        enter = slideInVertically(Motion.viva()) { -it } + fadeIn(Motion.calma(250)),
        exit = slideOutVertically(Motion.calma(250)) { -it } + fadeOut(Motion.calma(200)),
        modifier = modifier,
    ) {
        val ach = shown ?: return@AnimatedVisibility
        var unlocked by remember(ach.id) { mutableStateOf(reduce) }
        val colorMix = remember(ach.id) { Animatable(if (reduce) 1f else 0f) }
        LaunchedEffect(ach.id) {
            delay(350)
            unlocked = true
            haptics.alegre()
            colorMix.animateTo(1f, Motion.calma(500))
            delay(4500)
            onDismiss()
        }
        val neutral = MaterialTheme.colorScheme.surfaceVariant
        ElevatedCard(
            onClick = onOpen,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.elevatedCardElevation(6.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ShapeContainer(
                    shape = if (unlocked) OrgShape.FLOWER else OrgShape.PEBBLE,
                    size = 48.dp,
                    containerColor = lerp(neutral, ach.color.copy(alpha = 0.25f), colorMix.value),
                    contentColor = lerp(MaterialTheme.colorScheme.onSurfaceVariant, ach.color, colorMix.value),
                    morph = true,
                    breathe = unlocked,
                ) {
                    Icon(ach.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Conquista desbloqueada!",
                        style = MaterialTheme.typography.labelMedium,
                        color = ach.color,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(ach.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        ach.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dispensar")
                }
            }
        }
    }
}
