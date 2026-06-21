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
import androidx.compose.material.icons.filled.Flag
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
import br.com.zenith.ui.components.common.ZenithConfirmSheet
import br.com.zenith.ui.components.common.ZenithFilterBar
import br.com.zenith.ui.components.common.ZenithFilterOption
import br.com.zenith.ui.components.common.ZenithSheetDragHandle
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.notifications.ZenithNotifier
import br.com.zenith.ui.theme.*
import br.com.zenith.ui.theme.items.ZenithOptionField
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.utils.UnitFormatters
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
    var goalPendingDelete by remember { mutableStateOf<PersonalGoal?>(null) }
    var selectedFilter by remember { mutableStateOf(GoalFilter.All) }
    val filteredGoals = remember(goals, selectedFilter) {
        goals.filter { goal ->
            when (selectedFilter) {
                GoalFilter.All -> true
                GoalFilter.Active -> goal.isActive
                GoalFilter.Paused -> !goal.isActive
            }
        }
    }

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
            Spacer(modifier = Modifier.height(18.dp))
            GoalsHeader(
                onBack = { navController.popBackStack() },
                onCreate = {
                    editingGoal = null
                    showForm = true
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            GoalsSummaryCard(goals = goals)
            Spacer(modifier = Modifier.height(12.dp))
            GoalFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
            Spacer(modifier = Modifier.height(14.dp))

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
                filteredGoals.isEmpty() -> {
                    GoalMessageState(
                        title = "Nada por aqui",
                        message = "Não há metas nesse filtro.",
                        actionLabel = "Ver todas",
                        onAction = { selectedFilter = GoalFilter.All },
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(filteredGoals, key = { it.id ?: it.title }) { goal ->
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
                                    goalPendingDelete = goal
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

        goalPendingDelete?.let { goal ->
            ZenithConfirmSheet(
                title = "Excluir meta?",
                message = "Essa ação remove a meta e não pode ser desfeita.",
                confirmLabel = "Excluir",
                onDismiss = { goalPendingDelete = null },
                onConfirm = {
                    val goalId = goal.id
                    if (goalId != null) {
                        goalPendingDelete = null
                        scope.launch {
                            try {
                                goalsRepository.deleteGoal(goalId)
                                ZenithNotifier.success("Meta excluída.")
                                refreshGoals()
                            } catch (e: Exception) {
                                ZenithNotifier.error("Erro ao excluir meta: ${e.localizedMessage}")
                            }
                        }
                    }
                }
            )
        }
    }
}

private enum class GoalFilter(val label: String) {
    All("Todas"),
    Active("Ativas"),
    Paused("Pausadas")
}

@Composable
private fun GoalsHeader(
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = Green
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(
                text = "Metas",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    fontSize = 28.sp,
                    color = Black
                )
            )
            Text(
                text = "Organize objetivos semanais e mensais",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF667066)
                )
            )
        }
        Button(
            onClick = onCreate,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Criar", fontFamily = Inter, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoalsSummaryCard(goals: List<PersonalGoal>) {
    val active = goals.count { it.isActive }
    val paused = goals.size - active

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFFEAF3DE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = Green)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${goals.size} ${if (goals.size == 1) "meta" else "metas"}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    color = Black
                )
            )
            Text(
                text = "$active ativas • $paused pausadas",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF667066)
                )
            )
        }

    }
}

@Composable
private fun GoalFilterBar(
    selectedFilter: GoalFilter,
    onFilterSelected: (GoalFilter) -> Unit
) {
    ZenithFilterBar(
        options = GoalFilter.entries.map { ZenithFilterOption(it, it.label) },
        selectedValue = selectedFilter,
        onSelected = onFilterSelected,
        modifier = Modifier.fillMaxWidth()
    )
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
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
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
            shape = RoundedCornerShape(10.dp),
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
    val statusColor = if (goal.isActive) Green else Color(0xFF8A8A8A)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (goal.isActive) Color(0xFFBFDDBF) else Color(0xFFE0E0E0),
                RoundedCornerShape(10.dp)
            )
            .clickable { onEdit() },
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W800,
                            color = if (goal.isActive) Black else Color(0xFF6F6F6F)
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoalChip(text = goalMetricLabel(goal.metric), color = statusColor)
                        GoalChip(text = goalPeriodLabel(goal.period), color = Color(0xFF536057))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = Color(0xFF9B2D20).copy(alpha = 0.7f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Alvo",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF667066)
                        )
                    )
                    Text(
                        text = formatGoalTarget(goal),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W800,
                            color = if (goal.isActive) Green else Color(0xFF777777)
                        )
                    )
                }
                Switch(
                    checked = goal.isActive,
                    onCheckedChange = onToggleActive,
                    colors = zenithSwitchColors()
                )
            }
        }
    }
}

@Composable
private fun GoalChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
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
    var targetValue by rememberSaveable(formKey) { mutableStateOf(goal?.let { formatGoalInputValue(it) } ?: "") }
    val cleanedTitle = title.trim()
    val targetVal = targetValue.toDoubleOrNull()
    val canSave = cleanedTitle.isNotBlank() && targetVal != null && targetVal > 0.0
    val targetUnit = getUnitForMetric(metric)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = { ZenithSheetDragHandle() }
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
    ZenithFilterBar(
        options = metrics.map { (metric, label) -> ZenithFilterOption(metric, label) },
        selectedValue = selectedMetric,
        onSelected = onMetricSelected,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun GoalPeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = listOf("weekly" to "Semanal", "monthly" to "Mensal")
    ZenithFilterBar(
        options = periods.map { (period, label) -> ZenithFilterOption(period, label) },
        selectedValue = selectedPeriod,
        onSelected = onPeriodSelected,
        modifier = Modifier.fillMaxWidth()
    )
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

private fun goalMetricLabel(metric: String): String = when (metric) {
    "distance_km" -> "Distância"
    "steps" -> "Passos"
    "active_minutes" -> "Minutos ativos"
    "activities" -> "Atividades"
    "sleep_hours" -> "Sono"
    else -> "Meta"
}

private fun goalPeriodLabel(period: String): String = when (period) {
    "weekly" -> "Semanal"
    "monthly" -> "Mensal"
    else -> period.replaceFirstChar { it.uppercase() }
}

private fun formatGoalTarget(goal: PersonalGoal): String {
    val value = formatGoalNumber(goal.targetValue)
    val unit = getUnitForMetric(goal.metric)
    return if (unit.isBlank()) value else "$value $unit"
}

private fun formatGoalInputValue(goal: PersonalGoal): String {
    return formatGoalNumber(goal.targetValue)
}

private fun formatGoalNumber(value: Double): String {
    return UnitFormatters.compactNumber(value)
}
