package io.github.kellyson71.supaco.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.kellyson71.supaco.MainActivity
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.local.entity.BoletimEntity
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.calcStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.floor

// Paleta fixa do widget (light/dark) — Glance não tem acesso ao tema Compose
internal fun statusSolid(status: AbsenceStatus) = when (status) {
    AbsenceStatus.GO -> Color(0xFF1E7A47) to Color(0xFF6FE39B)
    AbsenceStatus.WARN -> Color(0xFF8A6A00) to Color(0xFFF2C84B)
    AbsenceStatus.LAST -> Color(0xFFA24A12) to Color(0xFFFFA968)
    AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Color(0xFFBA1A1A) to Color(0xFFFFB4AB)
}

internal fun statusContainer(status: AbsenceStatus) = when (status) {
    AbsenceStatus.GO -> Color(0xFFB9F0CC) to Color(0xFF0F4427)
    AbsenceStatus.WARN -> Color(0xFFFFE08A) to Color(0xFF473400)
    AbsenceStatus.LAST -> Color(0xFFFFD9BF) to Color(0xFF572400)
    AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Color(0xFFFFDAD6) to Color(0xFF5C1413)
}

internal fun statusOnContainer(status: AbsenceStatus) = when (status) {
    AbsenceStatus.GO -> Color(0xFF06371F) to Color(0xFF9EF4BE)
    AbsenceStatus.WARN -> Color(0xFF4A3500) to Color(0xFFFFE08A)
    AbsenceStatus.LAST -> Color(0xFF5A2600) to Color(0xFFFFD9BF)
    AbsenceStatus.NO, AbsenceStatus.REPROVADO -> Color(0xFF410002) to Color(0xFFFFDAD6)
}

internal data class WidgetMateria(
    val nome: String,
    val faltas: Int,
    val limite: Int,
    val restantes: Int,
    val status: AbsenceStatus,
)

internal fun BoletimEntity.toWidgetMateria(): WidgetMateria {
    val limite = floor(cargaHoraria * 0.25).toInt()
    return WidgetMateria(
        nome = disciplina.substringAfter(" - ").ifBlank { disciplina }.trim(),
        faltas = numeroFaltas,
        limite = limite,
        restantes = limite - numeroFaltas,
        status = calcStatus(numeroFaltas, cargaHoraria),
    )
}

class SupacoWidget : GlanceAppWidget(), KoinComponent {

    private val database: AppDatabase by inject()

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Lê tudo do Room antes de compor — sem I/O durante a renderização
        val materias = database.boletimDao().getBoletim()
            .map { it.toWidgetMateria() }
            .sortedBy { it.restantes }
        val hasProfile = database.profileDao().getProfile() != null

        provideContent {
            GlanceTheme {
                WidgetContent(materias = materias, loggedIn = hasProfile)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(materias: List<WidgetMateria>, loggedIn: Boolean) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (!loggedIn || materias.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (!loggedIn) "Faça login no Supaco" else "Sem dados ainda. Sincronize no app.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            val totalLivres = materias.sumOf { maxOf(0, it.restantes) }
            val pior = materias.first()

            // Header
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Supaco",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    "$totalLivres faltas livres",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Matérias (até 3, da mais crítica para a mais folgada)
            materias.take(3).forEach { m ->
                MateriaRow(m)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                "Pior cenário: ${pior.nome}",
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun MateriaRow(m: WidgetMateria) {
    val (lightSolid, darkSolid) = statusSolid(m.status)
    val (lightContainer, darkContainer) = statusContainer(m.status)
    val (lightOn, darkOn) = statusOnContainer(m.status)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(day = lightContainer, night = darkContainer))
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(ColorProvider(day = lightSolid, night = darkSolid))
                .cornerRadius(4.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            m.nome,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = ColorProvider(day = lightOn, night = darkOn),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            if (m.restantes <= 0) "já era" else "${m.restantes} livre${if (m.restantes == 1) "" else "s"}",
            style = TextStyle(
                color = ColorProvider(day = lightSolid, night = darkSolid),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
