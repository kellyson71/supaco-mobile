package com.example.supacomobile.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
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
import com.example.supacomobile.MainActivity
import com.example.supacomobile.data.local.AppDatabase
import com.example.supacomobile.ui.dashboard.AbsenceStatus
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

class SupacoVerdictWidget : GlanceAppWidget(), KoinComponent {

    private val database: AppDatabase by inject()

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val pior = database.boletimDao().getBoletim()
            .map { it.toWidgetMateria() }
            .minByOrNull { it.restantes }

        provideContent {
            GlanceTheme {
                VerdictContent(pior)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun VerdictContent(materia: WidgetMateria?) {
    if (materia == null) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
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
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Posso faltar?",
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
            else "${materia.restantes} livre${if (materia.restantes == 1) "" else "s"} · ${materia.nome}",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(day = lightOn, night = darkOn),
                fontSize = 10.sp,
            ),
        )
    }
}
