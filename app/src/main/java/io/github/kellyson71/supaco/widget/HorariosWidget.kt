package io.github.kellyson71.supaco.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.kellyson71.supaco.MainActivity
import io.github.kellyson71.supaco.data.local.AppDatabase
import io.github.kellyson71.supaco.data.ScheduleData
import io.github.kellyson71.supaco.ui.dashboard.AbsenceStatus
import io.github.kellyson71.supaco.ui.dashboard.calcStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HorariosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HorariosWidget()
}

internal data class AulaInfo(
    val nome: String,
    val dia: String,
    val horaInicio: String,
    val horaFim: String,
    val sala: String,
    val status: AbsenceStatus,
)

private val KEY_DAY_INDEX = intPreferencesKey("day_index")
private const val DAY_TODAY = -1 // sentinela: acompanha o dia atual

class PrevDayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        shiftDay(context, glanceId, -1)
    }
}

class NextDayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        shiftDay(context, glanceId, +1)
    }
}

class ResetDayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[KEY_DAY_INDEX] = DAY_TODAY }
        HorariosWidget().update(context, glanceId)
    }
}

private suspend fun shiftDay(context: Context, glanceId: GlanceId, delta: Int) {
    updateAppWidgetState(context, glanceId) { prefs ->
        val current = prefs[KEY_DAY_INDEX].let { if (it == null || it == DAY_TODAY) ScheduleData.todayIndex() else it }
        val size = ScheduleData.DIAS_SEMANA.size
        prefs[KEY_DAY_INDEX] = (current + delta + size) % size
    }
    HorariosWidget().update(context, glanceId)
}

class HorariosWidget : GlanceAppWidget(), KoinComponent {

    private val database: AppDatabase by inject()

    override val sizeMode = SizeMode.Single
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val boletim = database.boletimDao().getBoletim()
        val porSigla = boletim.associateBy { ScheduleData.extraiSigla(it.disciplina) }
        val horariosReais = database.horarioDao().getAll()

        // Grade real do SUAP; fallback hardcoded se o sync ainda não trouxe horários
        val aulas = if (horariosReais.isNotEmpty()) {
            horariosReais.mapNotNull { h ->
                val entity = porSigla[h.sigla] ?: return@mapNotNull null
                AulaInfo(
                    nome = ScheduleData.limpaNome(entity.disciplina),
                    dia = h.dia,
                    horaInicio = h.horaInicio,
                    horaFim = h.horaFim,
                    sala = h.sala,
                    status = calcStatus(entity.numeroFaltas, entity.cargaHoraria),
                )
            }
        } else {
            boletim.mapNotNull { entity ->
                val sched = ScheduleData.forDisciplina(entity.disciplina) ?: return@mapNotNull null
                AulaInfo(
                    nome = ScheduleData.limpaNome(entity.disciplina),
                    dia = sched.dia,
                    horaInicio = sched.horaInicio,
                    horaFim = sched.horaFim,
                    sala = sched.sala,
                    status = calcStatus(entity.numeroFaltas, entity.cargaHoraria),
                )
            }
        }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val stored = prefs[KEY_DAY_INDEX] ?: DAY_TODAY
                val dayIdx = if (stored == DAY_TODAY) ScheduleData.todayIndex() else stored
                HorariosContent(aulas = aulas, dayIdx = dayIdx, followingToday = stored == DAY_TODAY)
            }
        }
    }
}

@Composable
private fun HorariosContent(aulas: List<AulaInfo>, dayIdx: Int, followingToday: Boolean) {
    val dia = ScheduleData.DIAS_SEMANA[dayIdx]
    val ehHoje = dia == ScheduleData.currentDayName()
    val aulasDoDia = aulas.filter { it.dia == dia }.sortedBy { ScheduleData.parseMinutes(it.horaInicio) }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(14.dp),
    ) {
        // Header: ‹ Dia ›
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                modifier = GlanceModifier
                    .padding(horizontal = 10.dp)
                    .clickable(actionRunCallback<PrevDayAction>()),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
            Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                Text(
                    if (ehHoje) "$dia · hoje" else dia,
                    modifier = GlanceModifier.clickable(actionRunCallback<ResetDayAction>()),
                    style = TextStyle(
                        color = if (ehHoje) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Text(
                "›",
                modifier = GlanceModifier
                    .padding(horizontal = 10.dp)
                    .clickable(actionRunCallback<NextDayAction>()),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (aulasDoDia.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Dia livre. Aproveita. 😴",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
        } else {
            val now = ScheduleData.nowMinutes()
            val atual = if (ehHoje) aulasDoDia.firstOrNull {
                now >= ScheduleData.parseMinutes(it.horaInicio) && now < ScheduleData.parseMinutes(it.horaFim)
            } else null
            val proxima = if (ehHoje) aulasDoDia.firstOrNull {
                ScheduleData.parseMinutes(it.horaInicio) > now
            } else null

            if (atual != null) {
                // Estado "ao vivo": aula rolando agora + a próxima
                LiveAulaCard(atual, label = "AGORA · até ${atual.horaFim}")
                if (proxima != null) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    AulaRow(proxima, prefix = "Depois · ")
                }
            } else if (ehHoje && proxima != null && aulasDoDia.any { ScheduleData.parseMinutes(it.horaFim) <= now }) {
                // Intervalo entre aulas
                LiveAulaCard(proxima, label = "PRÓXIMA · ${proxima.horaInicio}")
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    "Intervalo — respira e volta.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
            } else {
                // Estado "agenda": todas as aulas do dia
                aulasDoDia.forEach { aula ->
                    AulaRow(aula)
                    Spacer(modifier = GlanceModifier.height(5.dp))
                }
                if (ehHoje && proxima == null && aulasDoDia.isNotEmpty() && now > ScheduleData.parseMinutes(aulasDoDia.last().horaFim)) {
                    Text(
                        "Acabou por hoje. 🎉",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveAulaCard(aula: AulaInfo, label: String) {
    val (lightContainer, darkContainer) = statusContainer(aula.status)
    val (lightOn, darkOn) = statusOnContainer(aula.status)
    val (lightSolid, darkSolid) = statusSolid(aula.status)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(day = lightContainer, night = darkContainer))
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            label,
            style = TextStyle(
                color = ColorProvider(day = lightSolid, night = darkSolid),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            aula.nome,
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(day = lightOn, night = darkOn),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            "${aula.horaInicio}–${aula.horaFim} · ${aula.sala}",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(day = lightOn, night = darkOn),
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
private fun AulaRow(aula: AulaInfo, prefix: String = "") {
    val (lightSolid, darkSolid) = statusSolid(aula.status)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(26.dp)
                .background(ColorProvider(day = lightSolid, night = darkSolid))
                .cornerRadius(2.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            aula.horaInicio,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            "$prefix${aula.nome}",
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
        )
    }
}
