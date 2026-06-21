package br.com.zenith.ui.screens.progress

import android.graphics.Paint
import android.graphics.RectF
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.R
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.ZenithSheetDragHandle
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.SecondaryGreen
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.components.common.ScreenHeader
import br.com.zenith.ui.components.common.ZenithFilterBar
import br.com.zenith.ui.components.common.ZenithFilterOption
import br.com.zenith.viewmodels.progress.ProgressDay
import br.com.zenith.viewmodels.progress.ProgressPeriod
import br.com.zenith.viewmodels.progress.ProgressSummary
import br.com.zenith.data.models.ProgressGoal
import br.com.zenith.utils.UnitFormatters
import br.com.zenith.viewmodels.progress.ProgressViewModel
import kotlin.math.max

@Composable
fun ProgressScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: ProgressViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    var period by remember { mutableStateOf(ProgressPeriod.Weekly) }
    var showPremiumDialog by remember { mutableStateOf(false) }

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
                contentPadding = PaddingValues(top = 14.dp, bottom = BottomNavListPadding),
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
                        val goals = if (period == ProgressPeriod.Monthly) state.monthlyGoals else state.weeklyGoals
                        if (goals.isNotEmpty()) {
                            GoalsSection(
                                goals = goals,
                                onManageGoals = { navController.navigate("goals") }
                            )
                        } else {
                            EmptyGoalsCard(onManageGoals = { navController.navigate("goals") })
                        }
                    }

                    item {
                        SummaryGrid(summary = summary)
                    }

                    item {
                        ReportCharts(
                            days = days,
                            period = period,
                            onViewAll = { navController.navigate("progress_reports/${period.name}") }
                        )
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
    }
}

@Composable
private fun ProgressHeader(
    period: ProgressPeriod,
    isPremium: Boolean,
    onTogglePeriod: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenHeader(
            title = if (period == ProgressPeriod.Monthly) "Relatório Mensal" else "Relatório Semanal",
            icon = Icons.Default.Timeline
        )

        ZenithFilterBar(
            options = listOf(
                ZenithFilterOption(ProgressPeriod.Weekly, "Semanal"),
                ZenithFilterOption(ProgressPeriod.Monthly, if (isPremium) "Mensal" else "Mensal Premium")
            ),
            selectedValue = period,
            onSelected = { selected ->
                if (selected != period) onTogglePeriod()
            },
            modifier = Modifier.fillMaxWidth()
        )
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W800,
                        fontSize = 18.sp
                    )
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
private fun GoalsSection(
    goals: List<ProgressGoal>,
    onManageGoals: () -> Unit
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
                    text = "Minhas Metas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W800,
                        fontSize = 19.sp
                    )
                )
                TextButton(onClick = onManageGoals) {
                    Text(
                        text = "Gerenciar",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Inter,
                            color = Green,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            goals.take(3).forEach { goal ->
                GoalItem(goal = goal)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun GoalItem(goal: ProgressGoal) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800
                )
            )
            Text(
                text = "${formatOneDecimal(goal.currentValue)} / ${formatOneDecimal(goal.targetValue)} ${goal.unit}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Inter)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE9F8E9), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(goal.progressPercent)
                    .fillMaxHeight()
                    .background(Green, CircleShape)
            )
        }
    }
}

@Composable
private fun EmptyGoalsCard(onManageGoals: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecondaryGreen, RoundedCornerShape(8.dp))
            .clickable { onManageGoals() },
        color = Color(0xFFF8FFF8),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Crie sua primeira meta",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800
                )
            )
            Text(
                text = "Defina objetivos semanais ou mensais para acompanhar sua evolução.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF606060),
                    textAlign = TextAlign.Center
                )
            )
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
                value = UnitFormatters.steps(summary.steps)
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
                iconRes = R.drawable.firestreak,
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
    icon: ImageVector? = null,
    iconRes: Int? = null,
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
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W800,
                        fontSize = 21.sp,
                        color = Color(0xFF111111)
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
    }
}

@Composable
private fun ReportCharts(
    days: List<ProgressDay>,
    period: ProgressPeriod,
    onViewAll: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (period == ProgressPeriod.Monthly) "Relatório de 30 dias" else "Relatório de 7 dias",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W800,
                        fontSize = 19.sp
                    )
                )
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Inter,
                        color = Color(0xFF777777),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onViewAll() }
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
                valueLabel = { UnitFormatters.steps(it.toInt()) },
                lineColor = Color(0xFF197233)
            )

            LineChart(
                title = "Atividades",
                values = days.map { it.distanceKm.toFloat() },
                labels = chartLabels(days),
                valueLabel = { UnitFormatters.kilometers(it.toDouble()) },
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
    val rawMaxValue = values.maxOrNull() ?: 0f
    val maxValue = max(rawMaxValue, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                fontSize = 18.sp,
                color = Color(0xFF1B1B1B)
            )
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val left = 44.dp.toPx()
            val right = size.width - 4.dp.toPx()
            val top = 18.dp.toPx()
            val bottom = size.height - 32.dp.toPx()
            val chartHeight = bottom - top
            val chartWidth = right - left
            val gridColor = Color(0xFFD7DDD7)
            val axisPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = Color(0xFF556255).toArgb()
                textAlign = Paint.Align.RIGHT
            }
            val pointLabelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = Color(0xFF263026).toArgb()
                textAlign = Paint.Align.CENTER
            }
            val pointLabelBackgroundPaint = Paint().apply {
                isAntiAlias = true
                color = Color(0xFFF8FFF8).toArgb()
            }

            repeat(4) { index ->
                val y = top + chartHeight * index / 3f
                val axisValue = maxValue * (3 - index) / 3f
                drawContext.canvas.nativeCanvas.drawText(
                    valueLabel(axisValue),
                    left - 7.dp.toPx(),
                    y + 4.dp.toPx(),
                    axisPaint
                )
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

                val peak = rawMaxValue
                val shouldLabelAll = values.size <= 10
                points.forEachIndexed { index, point ->
                    val value = values[index]
                    if (value > 0f && value != peak && (shouldLabelAll || index % 3 == 0)) {
                        val label = valueLabel(value)
                        val labelWidth = pointLabelPaint.measureText(label)
                        val labelX = point.x.coerceIn(
                            left + labelWidth / 2f + 4.dp.toPx(),
                            right - labelWidth / 2f - 4.dp.toPx()
                        )
                        val labelY = (point.y - 8.dp.toPx()).coerceAtLeast(10.dp.toPx())
                        val rect = RectF(
                            labelX - labelWidth / 2f - 4.dp.toPx(),
                            labelY - 11.dp.toPx(),
                            labelX + labelWidth / 2f + 4.dp.toPx(),
                            labelY + 4.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawRoundRect(
                            rect,
                            5.dp.toPx(),
                            5.dp.toPx(),
                            pointLabelBackgroundPaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            labelX,
                            labelY,
                            pointLabelPaint
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3F473F)
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W800,
                        fontSize = 18.sp
                    )
                )
            }
            Text(
                text = if (hasData) {
                    "Você acumulou ${UnitFormatters.kilometersWithSpace(summary.distanceKm)}, ${UnitFormatters.steps(summary.steps)} passos e ${UnitFormatters.minutes(summary.durationMin)} ativos."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumDialog(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Transparent,
        dragHandle = { ZenithSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = Green)
            Text(
                text = "Relatório mensal Premium",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Assine o Premium para comparar 30 dias, ver tendências completas e acompanhar sua evolução com mais contexto.",
                fontFamily = Inter
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Entendi")
            }
        }
    }
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

private fun formatOneDecimal(value: Double): String {
    return UnitFormatters.compactNumber(value)
}

@Composable
fun ProgressReportsScreen(
    navController: NavController,
    initialPeriod: ProgressPeriod
) {
    val context = LocalContext.current
    val viewModel: ProgressViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    val days = if (initialPeriod == ProgressPeriod.Monthly) state.monthlyDays else state.weeklyDays
    ZenithTheme {
        Scaffold(containerColor = Color.White) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = BottomNavListPadding)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Green
                            )
                        }
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = "Relatórios",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                            Text(
                                text = if (initialPeriod == ProgressPeriod.Monthly) "Últimos 30 dias" else "Últimos 7 dias",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF667066)
                                )
                            )
                        }
                    }
                }

                if (state.isLoading) {
                    item { CenteredZenithLoading() }
                } else {
                    items(days, key = { it.date.toString() }) { day ->
                        ProgressDayRow(
                            day = day,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressDayRow(day: ProgressDay, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE1E7DD), RoundedCornerShape(10.dp)),
        color = Color(0xFFF8F8F8),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = day.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    color = Color(0xFF111111)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportMiniValue("Sono", "${formatOneDecimal(day.sleepHours.toDouble())}h")
                ReportMiniValue("Passos", UnitFormatters.steps(day.steps))
                ReportMiniValue("Distância", UnitFormatters.kilometersWithSpace(day.distanceKm))
                ReportMiniValue("Tempo", UnitFormatters.minutes(day.durationMin))
            }
        }
    }
}

@Composable
private fun ReportMiniValue(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                color = Color(0xFF238D25)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF667066)
            )
        )
    }
}
