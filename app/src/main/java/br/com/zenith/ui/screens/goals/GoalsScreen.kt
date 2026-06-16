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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.PersonalGoal
import br.com.zenith.data.repositories.GoalsRepository
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.notifications.ZenithNotifier
import br.com.zenith.ui.theme.*
import br.com.zenith.ui.theme.items.ZenithOptionField
import br.com.zenith.ui.theme.items.ZenithTextField
import kotlinx.coroutines.launch

@Composable
fun GoalsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val goalsRepository = remember { GoalsRepository() }
    var goals by remember { mutableStateOf<List<PersonalGoal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<PersonalGoal?>(null) }

    fun refreshGoals(showLoading: Boolean = false) {
        scope.launch {
            if (showLoading) isLoading = true
            errorMessage = null
            try {
                SupabaseConfig.init(context)
                goals = goalsRepository.getAllGoals()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Erro ao carregar metas"
                ZenithNotifier.error(errorMessage ?: "Erro ao carregar metas")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshGoals(showLoading = true)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Green
                    )
                }
                Text(
                    text = "Minhas Metas",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Green)
                    }
                }
                errorMessage != null -> {
                    GoalMessageState(
                        title = "Não foi possível carregar suas metas",
                        message = errorMessage.orEmpty(),
                        actionLabel = "Tentar novamente",
                        onAction = { refreshGoals(showLoading = true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                goals.isEmpty() -> {
                    GoalMessageState(
                        title = "Nenhuma meta criada",
                        message = "Crie uma meta semanal ou mensal para acompanhar seu progresso.",
                        actionLabel = "Criar meta",
                        onAction = {
                            editingGoal = null
                            showForm = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(goals, key = { it.id ?: it.title }) { goal ->
                            GoalItem(
                                goal = goal,
                                onToggleActive = { isActive ->
                                    val goalId = goal.id ?: return@GoalItem
                                    scope.launch {
                                        try {
                                            goalsRepository.toggleGoalActive(goalId, isActive)
                                            refreshGoals()
                                        } catch (e: Exception) {
                                            ZenithNotifier.error("Erro ao atualizar meta: ${e.localizedMessage}")
                                        }
                                    }
                                },
                                onDelete = {
                                    val goalId = goal.id ?: return@GoalItem
                                    scope.launch {
                                        try {
                                            goalsRepository.deleteGoal(goalId)
                                            ZenithNotifier.success("Meta excluída.")
                                            refreshGoals()
                                        } catch (e: Exception) {
                                            ZenithNotifier.error("Erro ao excluir meta: ${e.localizedMessage}")
                                        }
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
                        try {
                            goalsRepository.saveGoal(newGoal)
                            ZenithNotifier.success("Meta salva.")
                            refreshGoals()
                            showForm = false
                            editingGoal = null
                        } catch (e: Exception) {
                            ZenithNotifier.error("Erro ao salvar meta: ${e.localizedMessage}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun GoalMessageState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFE7F4E8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Green
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                color = Black
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = Color(0xFF606060),
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
                    colors = zenithSwitchColors()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormBottomSheet(
    goal: PersonalGoal?,
    onDismiss: () -> Unit,
    onSave: (PersonalGoal) -> Unit
) {
    val formKey = goal?.id ?: "new"
    var title by rememberSaveable(formKey) { mutableStateOf(goal?.title ?: "") }
    var metric by rememberSaveable(formKey) { mutableStateOf(goal?.metric ?: "distance_km") }
    var period by rememberSaveable(formKey) { mutableStateOf(goal?.period ?: "weekly") }
    var targetValue by rememberSaveable(formKey) { mutableStateOf(goal?.targetValue?.toString() ?: "") }
    val cleanedTitle = title.trim()
    val targetVal = targetValue.toDoubleOrNull()
    val canSave = cleanedTitle.isNotBlank() && targetVal != null && targetVal > 0.0
    val targetUnit = getUnitForMetric(metric)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (goal == null) "Nova meta" else "Editar meta",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            )

            ZenithTextField(
                value = title,
                onValueChange = { title = it },
                label = "Titulo",
                placeholder = "Ex: Caminhada matinal",
                isError = title.isNotBlank() && cleanedTitle.isBlank(),
                supportingText = {
                    if (title.isNotBlank() && cleanedTitle.isBlank()) {
                        Text(
                            "Informe um titulo valido",
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Inter
                        )
                    }
                }
            )

            ZenithOptionField(
                selectedValue = metric,
                placeholder = "Selecionar metrica",
                options = goalMetricOptions(),
                onSelected = { selected -> selected?.let { metric = it } },
                modifier = Modifier.fillMaxWidth()
            )

            ZenithOptionField(
                selectedValue = period,
                placeholder = "Selecionar periodo",
                options = goalPeriodOptions(),
                onSelected = { selected -> selected?.let { period = it } },
                modifier = Modifier.fillMaxWidth()
            )

            ZenithTextField(
                value = targetValue,
                onValueChange = { value ->
                    targetValue = value
                        .replace(',', '.')
                        .filter { it.isDigit() || it == '.' }
                },
                label = "Valor alvo" + targetUnit.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = "Ex: 10.5",
                isError = targetValue.isNotBlank() && (targetVal == null || targetVal <= 0.0),
                supportingText = {
                    if (targetValue.isNotBlank() && (targetVal == null || targetVal <= 0.0)) {
                        Text(
                            "Informe um valor maior que zero",
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = Inter
                        )
                    }
                }
            )

            Button(
                onClick = {
                    val validTarget = targetValue.toDoubleOrNull() ?: return@Button
                    if (cleanedTitle.isNotBlank() && validTarget > 0.0) {
                        onSave(
                            PersonalGoal(
                                id = goal?.id,
                                userId = "",
                                title = cleanedTitle,
                                metric = metric,
                                period = period,
                                targetValue = validTarget,
                                isActive = goal?.isActive ?: true
                            )
                        )
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    disabledContainerColor = Color(0xFFE6E6E6)
                )
            ) {
                Text(
                    text = "Salvar meta",
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
                        color = Color(0xFF6F6C6C)
                    )
                )
            }
        }
    }
}

private fun goalMetricOptions(): List<Pair<String, String>> = listOf(
    "distance_km" to "Distancia (km)",
    "steps" to "Passos",
    "active_minutes" to "Minutos ativos",
    "activities" to "Atividades",
    "sleep_hours" to "Sono (horas)"
)

private fun goalPeriodOptions(): List<Pair<String, String>> = listOf(
    "weekly" to "Semanal",
    "monthly" to "Mensal"
)

@Composable
private fun LegacyGoalFormBottomSheet(
    goal: PersonalGoal?,
    onDismiss: () -> Unit,
    onSave: (PersonalGoal) -> Unit
) {
    val formKey = goal?.id ?: "new"
    var title by rememberSaveable(formKey) { mutableStateOf(goal?.title ?: "") }
    var metric by rememberSaveable(formKey) { mutableStateOf(goal?.metric ?: "distance_km") }
    var period by rememberSaveable(formKey) { mutableStateOf(goal?.period ?: "weekly") }
    var targetValue by rememberSaveable(formKey) { mutableStateOf(goal?.targetValue?.toString() ?: "") }
    val cleanedTitle = title.trim()
    val targetVal = targetValue.toDoubleOrNull()
    val canSave = cleanedTitle.isNotBlank() && targetVal != null && targetVal > 0.0

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
                    placeholder = "Ex: Caminhada Matinal",
                    isError = title.isNotBlank() && cleanedTitle.isBlank(),
                    supportingText = {
                        if (title.isNotBlank() && cleanedTitle.isBlank()) {
                            Text("Informe um título válido")
                        }
                    }
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
                    onValueChange = { value ->
                        targetValue = value
                            .replace(',', '.')
                            .filter { it.isDigit() || it == '.' }
                    },
                    label = "Valor Alvo",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "Ex: 10.5",
                    isError = targetValue.isNotBlank() && (targetVal == null || targetVal <= 0.0),
                    supportingText = {
                        if (targetValue.isNotBlank() && (targetVal == null || targetVal <= 0.0)) {
                            Text("Informe um valor maior que zero")
                        }
                    }
                )

                Button(
                    onClick = {
                        val validTarget = targetValue.toDoubleOrNull() ?: return@Button
                        if (cleanedTitle.isNotBlank() && validTarget > 0.0) {
                            onSave(
                                PersonalGoal(
                                    id = goal?.id,
                                    userId = "",
                                    title = cleanedTitle,
                                    metric = metric,
                                    period = period,
                                    targetValue = validTarget,
                                    isActive = goal?.isActive ?: true
                                )
                            )
                        }
                    },
                    enabled = canSave,
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
