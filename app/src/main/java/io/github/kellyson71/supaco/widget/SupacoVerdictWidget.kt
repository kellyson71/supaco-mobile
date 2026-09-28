package io.github.kellyson71.supaco.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.kellyson71.supaco.MainActivity
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.ScheduleData
import io.github.kellyson71.supaco.ui.dashboard.statusParaFaltar
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SupacoVerdictWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SupacoVerdictWidget()
}

private fun verdictWord(status: AbsenceStatus) = when (status) {
    AbsenceStatus.GO -> "BORA"
    AbsenceStatus.WARN -> "CALMA"
    AbsenceStatus.LAST -> "ÚLTIMA"
    AbsenceStatus.NO -> "NEM PENSE"
    AbsenceStatus.REPROVADO -> "JÁ ERA"
}

internal data class VereditoHoje(val materia: WidgetMateria?, val temAulaHoje: Boolean)

/**
 * Mesma regra do app: com aula hoje, a pior matéria do dia contando todas as aulas;
 * sem aula hoje, a matéria com menos folga. Usado pelo widget e pelo tile.
 */
internal suspend fun carregarVereditoHoje(database: AppDatabase): VereditoHoje {
    val hoje = ScheduleData.currentDayName()
    val aulasHojePorSigla = database.horarioDao().getAll()
        .filter { it.dia == hoje }
        .groupBy { it.sigla }
        .mapValues { (_, list) -> list.sumOf { it.aulas } }
    val materias = database.boletimDao().getBoletim().map { entity ->
        entity.toWidgetMateria() to (aulasHojePorSigla[ScheduleData.extraiSigla(entity.disciplina)] ?: 0)
    }
    val deHoje = materias.filter { it.second > 0 }.map { (m, aulas) ->
        m.copy(status = statusParaFaltar(m.restantes, aulas), restantes = m.restantes - aulas)
    }
    return if (deHoje.isNotEmpty()) {
        VereditoHoje(deHoje.maxWithOrNull(compareBy<WidgetMateria> { it.status.ordinal }.thenByDescending { it.restantes }), true)
    } else {
        VereditoHoje(materias.map { it.first }.minByOrNull { it.restantes }, false)
    }
}

internal fun verdictWordFor(status: AbsenceStatus) = verdictWord(status)

class SupacoVerdictWidget : GlanceAppWidget(), KoinComponent {

    private val database: AppDatabase by inject()

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (escolhida, temAulaHoje) = carregarVereditoHoje(database)
        // Tocar no widget abre direto no veredito, não só no app
        val abrirVeredito = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_DEST, "verdict")

        provideContent {
            GlanceTheme {
                VerdictContent(escolhida, temAulaHoje, actionStartActivity(abrirVeredito))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun VerdictContent(materia: WidgetMateria?, temAulaHoje: Boolean, onClick: Action) {
    if (materia == null) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Abra o Supaco",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
        return
    }

    val (lightContainer, darkContainer) = statusContainer(materia.status)
    val (lightOn, darkOn) = statusOnContainer(materia.status)
    val (lightSolid, darkSolid) = statusSolid(materia.status)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(day = lightContainer, night = darkContainer))
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (temAulaHoje) "Posso faltar hoje?" else "Sem aula hoje",
            style = TextStyle(
                color = ColorProvider(day = lightSolid, night = darkSolid),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            verdictWord(materia.status),
            style = TextStyle(
                color = ColorProvider(day = lightOn, night = darkOn),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            if (materia.restantes <= 0) materia.nome
            else "${materia.restantes} livre${if (materia.restantes == 1) "" else "s"}${if (temAulaHoje) " depois" else ""} · ${materia.nome}",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(day = lightOn, night = darkOn),
                fontSize = 10.sp,
            ),
        )
    }
}
