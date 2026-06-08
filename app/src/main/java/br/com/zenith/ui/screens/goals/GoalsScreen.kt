package br.com.zenith.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.zenith.data.models.PersonalGoal
import br.com.zenith.data.repositories.GoalsRepository
import br.com.zenith.ui.theme.*
import br.com.zenith.ui.theme.items.ZenithTextField
import kotlinx.coroutines.launch

@Composable
fun GoalsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val goalsRepository = remember { GoalsRepository() }
    var goals by remember { mutableStateOf<List<PersonalGoal>>(emptyList()) }
    var showForm by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<PersonalGoal?>(null) }

    LaunchedEffect(Unit) {
        goals = goalsRepository.getAllGoals()
    }

    fun refreshGoals() {
        scope.launch {
            goals = goalsRepository.getAllGoals()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Minhas Metas",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(goals) { goal ->
                    GoalItem(
                        goal = goal,
                        onToggleActive = { isActive ->
                            scope.launch {
                                goalsRepository.toggleGoalActive(goal.id!!, isActive)
                                refreshGoals()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                goalsRepository.deleteGoal(goal.id!!)
                                refreshGoals()
                            }
                        },
                        onEdit = {
                            editingGoal = goal
                            showForm = true
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingGoal = null
                showForm = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Green,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Meta")
        }

        if (showForm) {
            GoalFormBottomSheet(
                goal = editingGoal,
                onDismiss = {
                    showForm = false
                    editingGoal = null
                },
                onSave = { newGoal ->
                    scope.launch {
                        goalsRepository.saveGoal(newGoal)
                        refreshGoals()
                        showForm = false
                        editingGoal = null
                    }
                }
            )
        }
    }
}

@Composable
fun GoalItem(
    goal: PersonalGoal,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SecondaryGreen, RoundedCornerShape(8.dp))
            .clickable { onEdit() },
        color = if (goal.isActive) Color(0xFFF8FFF8) else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        color = if (goal.isActive) Black else Color.Gray
                    )
                )
                Text(
                    text = "${goal.targetValue} ${getUnitForMetric(goal.metric)} • ${goal.period.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = if (goal.isActive) Color(0xFF606060) else Color.Gray
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = goal.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(checkedThumbColor = Green)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = Color(0xFFB0B0B0)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalFormBottomSheet(
    goal: PersonalGoal?,
    onDismiss: () -> Unit,
    onSave: (PersonalGoal) -> Unit
) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var metric by remember { mutableStateOf(goal?.metric ?: "distance_km") }
    var period by remember { mutableStateOf(goal?.period ?: "weekly") }
    var targetValue by remember { mutableStateOf(goal?.targetValue?.toString() ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) { },
            color = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (goal == null) "Nova Meta" else "Editar Meta",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold
                    )
                )

                ZenithTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Título da Meta",
                    placeholder = "Ex: Caminhada Matinal"
                )

                Text(
                    text = "Métrica",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Inter)
                )
                GoalMetricSelector(
                    selectedMetric = metric,
                    onMetricSelected = { metric = it }
                )

                Text(
                    text = "Período",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = Inter)
                )
                GoalPeriodSelector(
                    selectedPeriod = period,
                    onPeriodSelected = { period = it }
                )

                ZenithTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it },
                    label = "Valor Alvo",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "Ex: 10.5"
                )

                Button(
                    onClick = {
                        val targetVal = targetValue.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && targetVal > 0) {
                            onSave(
                                PersonalGoal(
                                    id = goal?.id,
                                    userId = "",
                                    title = title,
                                    metric = metric,
                                    period = period,
                                    targetValue = targetVal,
                                    isActive = goal?.isActive ?: true
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text(
                        text = "Salvar Meta",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Inter,
                            color = Color.Gray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GoalMetricSelector(
    selectedMetric: String,
    onMetricSelected: (String) -> Unit
) {
    val metrics = listOf(
        "distance_km" to "Distância (km)",
        "steps" to "Passos",
        "active_minutes" to "Minutos Ativos",
        "activities" to "Atividades",
        "sleep_hours" to "Sono (horas)"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        metrics.forEach { (metric, label) ->
            FilterChip(
                selected = selectedMetric == metric,
                onClick = { onMetricSelected(metric) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Inter)) },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF5F5F5),
                    selectedContainerColor = Green
                )
            )
        }
    }
}

@Composable
fun GoalPeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = listOf("weekly" to "Semanal", "monthly" to "Mensal")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        periods.forEach { (period, label) ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = Inter)) },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF5F5F5),
                    selectedContainerColor = Green
                )
            )
        }
    }
}

fun getUnitForMetric(metric: String): String {
    return when (metric) {
        "distance_km" -> "km"
        "steps" -> "passos"
        "active_minutes" -> "min"
        "activities" -> "atividades"
        "sleep_hours" -> "horas"
        else -> ""
    }
}
