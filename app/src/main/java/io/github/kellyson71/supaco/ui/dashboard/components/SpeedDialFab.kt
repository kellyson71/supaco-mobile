package io.github.kellyson71.supaco.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer

data class SpeedDialAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun SpeedDialFab(
    onSync: () -> Unit,
    syncing: Boolean,
    actions: List<SpeedDialAction>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val optionBounds = remember { mutableStateMapOf<Int, Rect>() }
    var fabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var accumulatedDrag by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
    ) {
        // Mini action FABs
        actions.forEachIndexed { index, action ->
            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically(
                    initialOffsetY = { it * 2 },
                    animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f - index * 30f)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it * 2 },
                ) + fadeOut(),
            ) {
                val isHovered = hoveredIndex == index
                val scale by animateFloatAsState(if (isHovered) 1.15f else 1.0f, label = "scale_$index")
                val fabContainerColor = if (isHovered) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.secondaryContainer
                val fabContentColor = if (isHovered) MaterialTheme.colorScheme.onPrimaryContainer 
                                      else MaterialTheme.colorScheme.onSecondaryContainer

                Row(
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            optionBounds[index] = coords.boundsInRoot()
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = if (isHovered) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.inverseSurface,
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 2.dp,
                    ) {
                        Text(
                            action.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isHovered) MaterialTheme.colorScheme.onPrimary 
                                    else MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            expanded = false
                            action.onClick()
                        },
                        containerColor = fabContainerColor,
                        contentColor = fabContentColor,
                    ) {
                        Icon(action.icon, contentDescription = action.label)
                    }
                }
            }
        }

        // Main FAB — tap = sync, long press = expand
        ExtendedFloatingActionButton(
            onClick = {
                if (expanded) {
                    expanded = false
                } else {
                    onSync()
                }
            },
            modifier = Modifier
                .onGloballyPositioned { fabCoordinates = it }
                .pointerInput(actions) {
                    detectTapGestures(
                        onTap = {
                            if (expanded) expanded = false
                            else onSync()
                        }
                    )
                }
                .pointerInput(actions) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            isDragging = true
                            dragStartOffset = offset
                            accumulatedDrag = Offset.Zero
                            expanded = true
                            hoveredIndex = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                            val rootStart = fabCoordinates?.localToRoot(dragStartOffset) ?: Offset.Zero
                            val currentRoot = rootStart + accumulatedDrag
                            var found: Int? = null
                            for ((idx, rect) in optionBounds) {
                                if (rect.contains(currentRoot)) {
                                    found = idx
                                    break
                                }
                            }
                            hoveredIndex = found
                        },
                        onDragEnd = {
                            isDragging = false
                            hoveredIndex?.let { idx ->
                                actions.getOrNull(idx)?.onClick?.invoke()
                            }
                            expanded = false
                            hoveredIndex = null
                        },
                        onDragCancel = {
                            isDragging = false
                            expanded = false
                            hoveredIndex = null
                        }
                    )
                },
            containerColor = if (expanded) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (expanded) MaterialTheme.colorScheme.onErrorContainer
                           else MaterialTheme.colorScheme.onPrimaryContainer,
            expanded = !syncing,
            icon = {
                val rotation by animateFloatAsState(
                    targetValue = if (syncing || expanded) 360f else 0f,
                    animationSpec = spring(),
                    label = "fab_rotation"
                )
                Icon(
                    if (expanded) Icons.Rounded.Close else Icons.Rounded.Sync,
                    contentDescription = if (expanded) "Fechar" else "Sincronizar",
                    modifier = Modifier.rotate(rotation),
                )
            },
            text = {
                Text(if (expanded) "Fechar" else "Sincronizar")
            },
        )
    }
}
