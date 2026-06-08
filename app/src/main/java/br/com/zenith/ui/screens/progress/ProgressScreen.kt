package br.com.zenith.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.Poppins
import br.com.zenith.ui.theme.SecondaryGreen
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.progress.ProgressDay
import br.com.zenith.viewmodels.progress.ProgressPeriod
import br.com.zenith.viewmodels.progress.ProgressSummary
import br.com.zenith.viewmodels.progress.ProgressViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: ProgressViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    var period by remember { mutableStateOf(ProgressPeriod.Weekly) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    val days = if (period == ProgressPeriod.Monthly) state.monthlyDays else state.weeklyDays
    val summary = if (period == ProgressPeriod.Monthly) state.monthlySummary else state.weeklySummary

    ZenithTheme {
        Scaffold(containerColor = Color.White) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 19.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    ProgressHeader(
                        period = period,
                        isPremium = state.isPremium,
                        onTogglePeriod = {
                            if (period == ProgressPeriod.Weekly) {
                                if (state.isPremium) period = ProgressPeriod.Monthly else showPremiumDialog = true
                            } else {
                                period = ProgressPeriod.Weekly
                            }
                        }
                    )
                }

                if (state.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Green, strokeWidth = 3.dp)
                        }
                    }
                } else if (state.error != null) {
                    item {
                        ErrorCard(
                            message = state.error.orEmpty(),
                            onRetry = { viewModel.load(context) }
                        )
                    }
                } else {
                    if (!state.isPremium) {
                        item { PremiumReportCard(onClick = { showPremiumDialog = true }) }
                    }

                    item {
                        GoalCard(
                            period = period,
                            summary = summary,
                            onAddSleep = { showSleepDialog = true }
                        )
                    }

                    item {
                        SummaryGrid(summary = summary)
                    }

                    item {
                        ReportCharts(days = days, period = period)
                    }

                    item {
                        InsightCard(
                            summary = summary,
                            hasData = days.any { it.activities > 0 || it.sleepHours > 0f },
                            onNewActivity = { navController.navigate("new_activity") }
                        )
                    }
                }
            }
        }

        if (showPremiumDialog) {
            PremiumDialog(onDismiss = { showPremiumDialog = false })
        }

        if (showSleepDialog) {
            ManualSleepDialog(
                onDismiss = { showSleepDialog = false },
                onSave = { hours, quality ->
                    showSleepDialog = false
                    viewModel.addManualSleep(hours, quality, context)
                }
            )
        }
    }
}

@Composable
private fun ProgressHeader(
    period: ProgressPeriod,
    isPremium: Boolean,
    onTogglePeriod: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (period == ProgressPeriod.Monthly) "Relatório Mensal" else "Relatório Semanal",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(34.dp)
            )
        }

        Row(
            modifier = Modifier
                .clickable { onTogglePeriod() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (period == ProgressPeriod.Monthly) "ver semanal" else "ver mensal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isPremium || period == ProgressPeriod.Monthly) {
                    Icons.Default.MilitaryTech
                } else {
                    Icons.Default.Lock
                },
                contentDescription = null,
                tint = if (isPremium) Green else Color(0xFF2F9B3A),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PremiumReportCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecondaryGreen, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color(0xFFF3FFF4),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Desbloqueie o relatório mensal",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Poppins)
                )
                Text(
                    text = "Premium mostra 30 dias, tendências completas e mais detalhes da sua evolução.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF5E5E5E)
                    )
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    period: ProgressPeriod,
    summary: ProgressSummary,
    onAddSleep: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecondaryGreen, RoundedCornerShape(8.dp)),
        color = Color(0xFFF8FFF8),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (period == ProgressPeriod.Monthly) "Metas do mês" else "Metas da semana",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = Poppins)
                )
                TextButton(onClick = onAddSleep) {
                    Text("registrar sono", color = Green, fontFamily = Inter)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .background(Color(0xFFE9F8E9), CircleShape)
                        .border(1.dp, Color(0xFFB8D9B8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column {
                    Text(
                        text = "Trilha",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Poppins)
                    )
                    Text(
                        text = "${formatDistance(summary.distanceKm)} km",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = Poppins,
                            color = Green
                        )
                    )
                    Text(
                        text = "${summary.activities} atividades registradas",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            color = Color(0xFF606060)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(summary: ProgressSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = "Passos",
                value = formatInt(summary.steps)
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bedtime,
                label = "Sono médio",
                value = "${formatOneDecimal(summary.averageSleepHours)}h"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Whatshot,
                label = "Sequência",
                value = "${summary.streak}d"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MilitaryTech,
                label = "Conquistas",
                value = summary.achievements.toString()
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier.border(1.dp, Color(0xFFE1EAE1), RoundedCornerShape(8.dp)),
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Poppins,
                        color = Color.Black
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
    }
}

@Composable
private fun ReportCharts(days: List<ProgressDay>, period: ProgressPeriod) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecondaryGreen, RoundedCornerShape(8.dp)),
        color = Color(0xFFF8FFF8),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (period == ProgressPeriod.Monthly) "Relatório de 30 dias" else "Relatório de 7 dias",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Poppins)
                )
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Inter,
                        color = Color(0xFF777777),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            LineChart(
                title = "Sono",
                values = days.map { it.sleepHours },
                labels = chartLabels(days),
                valueLabel = { "${formatOneDecimal(it.toDouble())}h" },
                lineColor = Color(0xFF438B7B)
            )

            LineChart(
                title = "Passos",
                values = days.map { it.steps.toFloat() },
                labels = chartLabels(days),
                valueLabel = { formatInt(it.toInt()) },
                lineColor = Color(0xFF197233)
            )

            LineChart(
                title = "Atividades",
                values = days.map { it.distanceKm.toFloat() },
                labels = chartLabels(days),
                valueLabel = { "${formatOneDecimal(it.toDouble())}km" },
                lineColor = Green
            )
        }
    }
}

@Composable
private fun LineChart(
    title: String,
    values: List<Float>,
    labels: List<String>,
    valueLabel: (Float) -> String,
    lineColor: Color
) {
    val maxValue = max(values.maxOrNull() ?: 0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W500
            )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val left = 42f
            val right = size.width - 4f
            val top = 12f
            val bottom = size.height - 32f
            val chartHeight = bottom - top
            val chartWidth = right - left
            val gridColor = Color(0xFFD7DDD7)

            repeat(4) { index ->
                val y = top + chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.5f
                )
            }

            if (values.isNotEmpty()) {
                val points = values.mapIndexed { index, value ->
                    val x = if (values.size == 1) {
                        left
                    } else {
                        left + chartWidth * index / (values.size - 1)
                    }
                    val y = bottom - chartHeight * (value / maxValue)
                    Offset(x, y)
                }

                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                points.forEach {
                    drawCircle(color = lineColor, radius = 4f, center = it)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Inter,
                        color = Color.Black
                    )
                )
            }
        }

        Text(
            text = "pico ${valueLabel(values.maxOrNull() ?: 0f)}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color(0xFF777777)
            )
        )
    }
}

@Composable
private fun InsightCard(
    summary: ProgressSummary,
    hasData: Boolean,
    onNewActivity: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE1EAE1), RoundedCornerShape(8.dp)),
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Destaque do período",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Poppins)
                )
            }
            Text(
                text = if (hasData) {
                    "Você acumulou ${formatDistance(summary.distanceKm)} km, ${formatInt(summary.steps)} passos e ${summary.durationMin} minutos ativos."
                } else {
                    "Registre sua primeira atividade para o Zenith montar seus gráficos de evolução."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF5F5F5F)
                )
            )
            if (!hasData) {
                Button(
                    onClick = onNewActivity,
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("Registrar atividade")
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF6F6),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.ifBlank { "Erro ao carregar progresso" },
                modifier = Modifier.weight(1f),
                color = Color(0xFF8A1F1F),
                fontFamily = Inter
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Recarregar",
                tint = Color(0xFF8A1F1F),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onRetry() }
            )
        }
    }
}

@Composable
private fun PremiumDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Green)
        },
        title = { Text("Relatório mensal Premium") },
        text = {
            Text("Assine o Premium para comparar 30 dias, ver tendências completas e acompanhar sua evolução com mais contexto.")
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Entendi")
            }
        }
    )
}

@Composable
private fun ManualSleepDialog(
    onDismiss: () -> Unit,
    onSave: (Float, Int?) -> Unit
) {
    var hours by remember { mutableFloatStateOf(8f) }
    var quality by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar sono") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Duração: ${formatOneDecimal(hours.toDouble())}h",
                    fontFamily = Inter
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6f, 7f, 8f, 9f).forEach { option ->
                        OutlinedButton(onClick = { hours = option }) {
                            Text("${option.toInt()}h")
                        }
                    }
                }
                Text("Qualidade", fontFamily = Inter)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { option ->
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { quality = option },
                            color = if (quality == option) Green else Color(0xFFEDEDED),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = option.toString(),
                                    color = if (quality == option) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(hours, quality) },
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun chartLabels(days: List<ProgressDay>): List<String> {
    if (days.isEmpty()) return emptyList()
    val indexes = if (days.size <= 7) {
        days.indices.toList()
    } else {
        listOf(0, 5, 10, 15, 20, 25, days.lastIndex).filter { it in days.indices }
    }
    return indexes.map { days[it].label }
}

private fun formatDistance(value: Double): String {
    return if (value >= 10) value.roundToInt().toString() else formatOneDecimal(value)
}

private fun formatOneDecimal(value: Double): String {
    return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", value)
}

private fun formatInt(value: Int): String {
    return "%,d".format(java.util.Locale.forLanguageTag("pt-BR"), value)
}
