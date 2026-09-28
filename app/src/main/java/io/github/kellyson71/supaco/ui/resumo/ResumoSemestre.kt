package io.github.kellyson71.supaco.ui.resumo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import io.github.kellyson71.supaco.theme.verdictColors
import io.github.kellyson71.supaco.ui.components.OrgShape
import io.github.kellyson71.supaco.ui.components.ShapeContainer
import io.github.kellyson71.supaco.ui.components.enterOnce
import io.github.kellyson71.supaco.ui.dashboard.LocalModoSerio
import io.github.kellyson71.supaco.ui.dashboard.MateriaDisplay
import io.github.kellyson71.supaco.ui.dashboard.rankDe
import io.github.kellyson71.supaco.ui.motion.CountUpText
import io.github.kellyson71.supaco.ui.motion.LocalReduceMotion
import io.github.kellyson71.supaco.ui.motion.Motion
import io.github.kellyson71.supaco.ui.motion.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Uma tela do resumo: forma, cor e conteúdo. */
private data class Pagina(
    val shape: OrgShape,
    val cor: Color,
    val onCor: Color,
    val content: @Composable () -> Unit,
)

/**
 * Resumo do semestre em telas sequenciais (estilo "Wrapped"). A forma do topo se
 * transforma a cada tela, os números contam e a última tela vira uma imagem
 * para compartilhar — sem dados sensíveis (só nome curto, frequência e rank).
 */
@Composable
fun ResumoSemestre(
    nome: String,
    periodo: String,
    materias: List<MateriaDisplay>,
    streakDays: Int,
    onClose: () -> Unit,
) {
    val serio = LocalModoSerio.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val vc = MaterialTheme.verdictColors

    val totalFaltas = materias.sumOf { it.faltas }
    val carga = materias.sumOf { it.total }
    val freq = if (carga > 0) ((1f - totalFaltas.toFloat() / carga) * 100).toInt().coerceIn(0, 100) else 100
    val maisFaltada = materias.maxByOrNull { it.faltas }
    val maisTranquila = materias.maxByOrNull { it.restantes }
    val rank = rankDe(totalFaltas, serio)
    val primeiroNome = nome.split(" ").first()

    val paginas = listOf(
        Pagina(OrgShape.FLOWER, cs.primaryContainer, cs.onPrimaryContainer) {
            Titulo(if (serio) "Resumo do semestre" else "Seu semestre, $primeiroNome")
            Subtitulo("$periodo · ${materias.size} matérias")
            Spacer(Modifier.height(24.dp))
            Subtitulo(if (serio) "Veja como foi sua frequência." else "Bora ver o estrago?")
        },
        Pagina(OrgShape.COOKIE, vc.goContainer, vc.onGoContainer) {
            Subtitulo("Você foi a")
            CountUpText(
                value = freq, format = { "$it%" }, durationMs = 1200, delayMs = 250,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp),
                fontWeight = FontWeight.Black, color = vc.onGoContainer,
            )
            Subtitulo("das aulas")
            Spacer(Modifier.height(16.dp))
            Subtitulo(
                when {
                    serio -> "$totalFaltas faltas no total."
                    freq >= 95 -> "Praticamente mora no campus."
                    freq >= 85 -> "Presença respeitável."
                    else -> "O sofá agradece a companhia."
                }
            )
        },
        Pagina(OrgShape.CLOVER, vc.lastContainer, vc.onLastContainer) {
            Subtitulo(if (serio) "Matéria com mais faltas" else "Sua matéria mais faltada")
            Spacer(Modifier.height(12.dp))
            Titulo(maisFaltada?.nome ?: "—")
            Spacer(Modifier.height(12.dp))
            CountUpText(
                value = maisFaltada?.faltas ?: 0, format = { "$it faltas" }, delayMs = 300,
                style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = vc.onLastContainer,
            )
        },
        Pagina(OrgShape.PEBBLE, cs.tertiaryContainer, cs.onTertiaryContainer) {
            Subtitulo(if (serio) "Matéria com mais folga" else "Sua matéria mais tranquila")
            Spacer(Modifier.height(12.dp))
            Titulo(maisTranquila?.nome ?: "—")
            Spacer(Modifier.height(12.dp))
            CountUpText(
                value = (maisTranquila?.restantes ?: 0).coerceAtLeast(0), format = { "$it faltas livres" }, delayMs = 300,
                style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = cs.onTertiaryContainer,
            )
        },
        Pagina(OrgShape.COOKIE, Color(0xFFFFE0B2), Color(0xFF4E2600)) {
            Subtitulo(if (serio) "Sequência atual sem faltas novas" else "Sua sequência sem falta nova")
            CountUpText(
                value = streakDays, format = { if (it == 1) "1 dia" else "$it dias" }, durationMs = 1000, delayMs = 250,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                fontWeight = FontWeight.Black, color = Color(0xFF4E2600),
            )
            Subtitulo(if (streakDays >= 7 || serio) "Continue assim." else "Dá pra melhorar, hein.")
        },
        Pagina(OrgShape.FLOWER, cs.secondaryContainer, cs.onSecondaryContainer) {
            Subtitulo(if (serio) "Classificação" else "Seu rank final")
            Spacer(Modifier.height(8.dp))
            Titulo(rank)
            Spacer(Modifier.height(8.dp))
            Subtitulo("$freq% de presença · $periodo")
        },
    )

    val pager = rememberPagerState { paginas.size }
    LaunchedEffect(pager.currentPage) { haptics.tick() }
    val pagina = paginas[pager.currentPage]
    val fundo by animateColorAsState(pagina.cor, Motion.calma(450), label = "resumo_bg")
    val texto by animateColorAsState(pagina.onCor, Motion.calma(450), label = "resumo_fg")

    // Captura da última tela para compartilhar
    val captura = rememberGraphicsLayer()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(fundo),
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                // Segmentos de progresso, estilo stories
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    paginas.indices.forEach { i ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(texto.copy(alpha = if (i <= pager.currentPage) 0.9f else 0.25f), CircleShape),
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fechar resumo", tint = texto)
                    }
                }

                // Forma que se transforma a cada tela
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    ShapeContainer(
                        shape = pagina.shape,
                        size = 120.dp,
                        containerColor = texto.copy(alpha = 0.15f),
                        contentColor = texto,
                        morph = true,
                        breathe = true,
                        spin = true,
                    )
                }

                HorizontalPager(
                    state = pager,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            // Toque na metade direita avança; na esquerda, volta
                            detectTapGestures { offset ->
                                scope.launch {
                                    val alvo = if (offset.x > size.width / 2) pager.currentPage + 1 else pager.currentPage - 1
                                    if (alvo in paginas.indices) {
                                        if (reduce) pager.scrollToPage(alvo) else pager.animateScrollToPage(alvo)
                                    }
                                }
                            }
                        },
                ) { index ->
                    val isLast = index == paginas.lastIndex
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalContentColor provides paginas[index].onCor,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(32.dp)
                                    .widthIn(max = 480.dp)
                                    .then(
                                        if (isLast) Modifier.drawWithContent {
                                            captura.record { this@drawWithContent.drawContent() }
                                            drawLayer(captura)
                                        } else Modifier
                                    )
                                    .background(if (isLast) paginas[index].cor else Color.Transparent)
                                    .padding(if (isLast) 16.dp else 0.dp)
                                    .enterOnce(),
                            ) {
                                paginas[index].content()
                                if (isLast) {
                                    Spacer(Modifier.height(20.dp))
                                    Text(
                                        "Supaco",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = paginas[index].onCor.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }

                if (pager.currentPage == paginas.lastIndex) {
                    Button(
                        onClick = {
                            haptics.confirmar()
                            scope.launch {
                                val bitmap = captura.toImageBitmap().asAndroidBitmap()
                                compartilharImagem(context, bitmap)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = texto, contentColor = fundo),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 24.dp)
                            .enterOnce(),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Compartilhar")
                    }
                } else {
                    Text(
                        "toque para avançar",
                        style = MaterialTheme.typography.labelMedium,
                        color = texto.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Titulo(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Subtitulo(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer { alpha = 0.85f },
    )
}

private suspend fun compartilharImagem(context: Context, bitmap: Bitmap) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "compartilhar").apply { mkdirs() }
        val file = File(dir, "resumo-supaco.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.arquivos", file)
    }
    val intent = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Compartilhar resumo"))
}
