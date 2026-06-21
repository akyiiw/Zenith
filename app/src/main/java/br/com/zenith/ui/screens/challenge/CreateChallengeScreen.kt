package br.com.zenith.ui.screens.challenge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.theme.Black
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.White
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithDateField
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.challenge.ChallengeDraft
import br.com.zenith.viewmodels.challenge.ChallengeViewModel
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TOTAL_STEPS = 9

@Composable
fun CreateChallengeScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: ChallengeViewModel = viewModel()
        val context = LocalContext.current
        val isSaving by viewModel.isSaving.collectAsState()

        CreateChallengeContent(
            isSaving = isSaving,
            onBack = { navController.popBackStack() },
            onFinish = { draft ->
                viewModel.createChallenge(draft, context) {
                    navController.navigate("challenge") {
                        popUpTo("challenge") { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
fun CreateChallengeContent(
    isSaving: Boolean = false,
    onBack: () -> Unit = {},
    onFinish: (ChallengeDraft) -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var exercise by rememberSaveable { mutableStateOf("corrida") }
    var goalMode by rememberSaveable { mutableStateOf("fixa") }
    var rankingType by rememberSaveable { mutableStateOf("menor_tempo") }
    var goal by rememberSaveable { mutableStateOf("") }
    var visibility by rememberSaveable { mutableStateOf("publico") }
    var premiumOnly by rememberSaveable { mutableStateOf(false) }
    var maxParticipants by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().format(dateFormatter)) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(7).format(dateFormatter)) }
    var bannerUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var allowManualEntries by rememberSaveable { mutableStateOf(false) }

    val parsedGoal = goal.replace(',', '.').toDoubleOrNull()
    val parsedStart = parseDate(startDate)
    val parsedEnd = parseDate(endDate)
    val parsedMax = maxParticipants.toIntOrNull()

    fun finish() {
        val start = parsedStart ?: return
        val end = parsedEnd ?: return
        onFinish(
            ChallengeDraft(
                title = title,
                description = description.takeIf { it.isNotBlank() },
                designatedActivity = exercise,
                goalMode = goalMode,
                rankingType = rankingType,
                objectiveMetric = objectiveMetricFor(rankingType),
                objectiveValue = parsedGoal.takeIf { goalMode == "fixa" },
                visibility = visibility,
                premiumOnly = premiumOnly,
                maxParticipants = parsedMax.takeIf { visibility != "convite" },
                startDate = start.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC),
                endDate = end.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC),
                allowManualEntries = allowManualEntries,
                bannerUri = bannerUri
            )
        )
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        },
        label = "challenge_onboarding_step"
    ) { currentStep ->
        when (currentStep) {
            1 -> StepChallengeName(title, { title = it }, { step++ }, onBack)
            2 -> StepChallengeDescription(description, { description = it }, { step++ }, { step-- })
            3 -> StepChallengeExercise(exercise, { exercise = it }, { step++ }, { step-- })
            4 -> StepChallengeGoal(
                goalMode = goalMode,
                onGoalModeChange = {
                    goalMode = it
                    rankingType = if (it == "fixa") "menor_tempo" else "distancia_total"
                    goal = ""
                },
                rankingType = rankingType,
                onRankingTypeChange = { rankingType = it },
                goal = goal,
                onGoalChange = { goal = it },
                onNext = { step++ },
                onBack = { step-- }
            )
            5 -> StepChallengeVisibility(visibility, { visibility = it }, { step++ }, { step-- })
            6 -> StepChallengePremium(premiumOnly, { premiumOnly = it }, { step++ }, { step-- })
            7 -> StepChallengeParticipants(visibility, maxParticipants, { maxParticipants = it }, { step++ }, { step-- })
            8 -> StepChallengeDates(
                startDate = startDate,
                onStartDateChange = { startDate = it },
                endDate = endDate,
                onEndDateChange = { endDate = it },
                minDate = LocalDate.now(),
                onNext = { step++ },
                onBack = { step-- }
            )
            9 -> StepChallengeBannerAndRules(
                bannerUri = bannerUri,
                onPhotoSelected = { bannerUri = it },
                allowManualEntries = allowManualEntries,
                onAllowManualEntriesChange = { allowManualEntries = it },
                isSaving = isSaving,
                canFinish = title.isNotBlank() &&
                    parsedStart != null &&
                    parsedEnd != null &&
                    !parsedEnd.isBefore(parsedStart) &&
                    (goalMode == "livre" || (parsedGoal ?: 0.0) > 0.0),
                onFinish = ::finish,
                onBack = { step-- }
            )
        }
    }
}

@Composable
private fun StepChallengeName(
    challengeName: String,
    onChallengeNameChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    CreateChallengeScaffold(
        step = 1,
        title = "Escolha um título para o seu desafio",
        subtitle = "Esse será o nome que outros participantes verão.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = challengeName.isNotBlank()
    ) {
        ZenithTextField(
            value = challengeName,
            onValueChange = onChallengeNameChange,
            label = "Nome do desafio"
        )
    }
}

@Composable
private fun StepChallengeDescription(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    CreateChallengeScaffold(
        step = 2,
        title = "Descreva o desafio",
        subtitle = "Explique a proposta, regras e motivação.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true
    ) {
        ZenithTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Descrição",
            minLines = 4,
            maxLines = 6
        )
    }
}

enum class ChallengeExercise(val value: String, val label: String) {
    CAMINHADA("caminhada", "Caminhada"),
    CORRIDA("corrida", "Corrida"),
    CICLISMO("ciclismo", "Ciclismo")
}

@Composable
private fun StepChallengeExercise(
    selectedExercise: String,
    onChallengeExerciseChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    CreateChallengeScaffold(
        step = 3,
        title = "Exercício do desafio",
        subtitle = "Designa o exercício permitido para atividades do desafio.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = selectedExercise.isNotBlank()
    ) {
        ChallengeExercise.entries.forEach { exercise ->
            SelectButton(
                label = exercise.label,
                selected = selectedExercise == exercise.value,
                onClick = { onChallengeExerciseChange(exercise.value) }
            )
        }
    }
}

@Composable
private fun StepChallengeGoal(
    goalMode: String,
    onGoalModeChange: (String) -> Unit,
    rankingType: String,
    onRankingTypeChange: (String) -> Unit,
    goal: String,
    onGoalChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val goalValue = goal.replace(',', '.').toDoubleOrNull()
    CreateChallengeScaffold(
        step = 4,
        title = "Meta do desafio",
        subtitle = "Livre soma todas as atividades. Fixa ranqueia o melhor registro.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = goalMode == "livre" || (goalValue ?: 0.0) > 0.0
    ) {
        SelectButton("Meta fixa", goalMode == "fixa") { onGoalModeChange("fixa") }
        SelectButton("Meta livre", goalMode == "livre") { onGoalModeChange("livre") }
        Spacer(modifier = Modifier.height(10.dp))
        if (goalMode == "fixa") {
            SelectOptionCard(
                title = "Tempo",
                subtitle = "Defina uma distância alvo. Vence quem completar em menor tempo.",
                selected = rankingType == "menor_tempo"
            ) { onRankingTypeChange("menor_tempo") }
            SelectOptionCard(
                title = "Distância",
                subtitle = "Defina um tempo alvo. Vence quem fizer a maior distância.",
                selected = rankingType == "maior_distancia"
            ) { onRankingTypeChange("maior_distancia") }
            SelectOptionCard(
                title = "Pace",
                subtitle = "Defina uma distância alvo. Vence quem tiver o menor pace.",
                selected = rankingType == "menor_pace"
            ) { onRankingTypeChange("menor_pace") }
        } else {
            SelectOptionCard(
                title = "Tempo total",
                subtitle = "Soma a duração de todas as atividades do participante.",
                selected = rankingType == "tempo_total"
            ) { onRankingTypeChange("tempo_total") }
            SelectOptionCard(
                title = "Distância total",
                subtitle = "Soma a distância de todas as atividades do participante.",
                selected = rankingType == "distancia_total"
            ) { onRankingTypeChange("distancia_total") }
        }
        if (goalMode == "fixa") {
            ZenithTextField(
                value = goal,
                onValueChange = {
                    onGoalChange(
                        if (rankingType == "maior_distancia") {
                            it.filter(Char::isDigit)
                        } else {
                            it.filter { char -> char.isDigit() || char == '.' || char == ',' }
                        }
                    )
                },
                label = goalInputLabel(rankingType),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (rankingType == "maior_distancia") KeyboardType.Number else KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepChallengeVisibility(
    visibility: String,
    onVisibilityChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    CreateChallengeScaffold(
        step = 5,
        title = "Quem pode participar?",
        subtitle = "Convite cria um desafio privado para convites futuros.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true
    ) {
        SelectButton("Público geral", visibility == "publico") { onVisibilityChange("publico") }
        SelectButton("Apenas amigos", visibility == "amigos") { onVisibilityChange("amigos") }
        SelectButton("Por convite", visibility == "convite") { onVisibilityChange("convite") }
    }
}

@Composable
private fun StepChallengePremium(
    premiumOnly: Boolean,
    onPremiumOnlyChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    CreateChallengeScaffold(
        step = 6,
        title = "Tipo de usuário",
        subtitle = "Defina se usuários casuais também poderão entrar.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true
    ) {
        SelectButton("Casual e Premium", !premiumOnly) { onPremiumOnlyChange(false) }
        SelectButton("Apenas Premium", premiumOnly) { onPremiumOnlyChange(true) }
    }
}

@Composable
private fun StepChallengeParticipants(
    visibility: String,
    maxParticipants: String,
    onMaxParticipantsChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    CreateChallengeScaffold(
        step = 7,
        title = "Participantes",
        subtitle = if (visibility == "convite") "Desafios por convite não usam limite público nesta etapa." else "Defina um limite ou deixe em branco.",
        onBack = onBack,
        onNext = onNext,
        nextEnabled = true
    ) {
        if (visibility == "convite") {
            Text("O desafio será privado até que convites sejam enviados.", color = Color.Gray, fontFamily = Inter)
        } else {
            ZenithTextField(
                value = maxParticipants,
                onValueChange = { onMaxParticipantsChange(it.filter(Char::isDigit)) },
                label = "Máximo de participantes (opcional)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepChallengeDates(
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    minDate: LocalDate,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }
    val start = parseDate(startDate)
    val end = parseDate(endDate)
    fun validateStartSelection(selected: LocalDate): String? = when {
        selected.isBefore(minDate) -> "O início não pode ficar no passado."
        end != null && end.isBefore(selected) -> "O início não pode ser posterior à conclusão."
        else -> null
    }
    fun validateEndSelection(selected: LocalDate): String? = when {
        selected.isBefore(minDate) -> "A conclusão não pode ficar no passado."
        start != null && selected.isBefore(start) -> "A conclusão não pode ser anterior ao início."
        else -> null
    }
    fun validateAndNext() {
        dateError = when {
            start == null || end == null -> "Escolha as duas datas."
            start.isBefore(minDate) || end.isBefore(minDate) -> "O período do desafio não pode ficar no passado."
            end.isBefore(start) -> "A conclusão não pode ser anterior ao início."
            else -> null
        }
        if (dateError == null) onNext()
    }
    CreateChallengeScaffold(
        step = 8,
        title = "Período",
        subtitle = "Use o formato dd/MM/yyyy.",
        onBack = onBack,
        onNext = { validateAndNext() },
        nextEnabled = start != null && end != null
    ) {
        ZenithDateField(
            value = startDate,
            onValueChange = {
                dateError = null
                onStartDateChange(it)
            },
            label = "Início",
            modifier = Modifier.fillMaxWidth(),
            isError = dateError != null,
            supportingText = dateError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            validateSelection = ::validateStartSelection
        )
        ZenithDateField(
            value = endDate,
            onValueChange = {
                dateError = null
                onEndDateChange(it)
            },
            label = "Conclusão",
            modifier = Modifier.fillMaxWidth(),
            isError = dateError != null,
            supportingText = dateError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            validateSelection = ::validateEndSelection
        )
    }
}

@Composable
private fun StepChallengeBannerAndRules(
    bannerUri: Uri?,
    onPhotoSelected: (Uri) -> Unit,
    allowManualEntries: Boolean,
    onAllowManualEntriesChange: (Boolean) -> Unit,
    isSaving: Boolean,
    canFinish: Boolean,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onPhotoSelected) }

    CreateChallengeScaffold(
        step = 9,
        title = "Banner e segurança",
        subtitle = "Finalize a apresentação e as regras de submissão.",
        onBack = onBack,
        onNext = onFinish,
        nextEnabled = canFinish && !isSaving,
        nextLabel = "Criar desafio",
        bottomContent = {
            if (isSaving) {
                ZenithLoading(modifier = Modifier.size(52.dp), strokeWidth = 18f)
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE8EFE8))
                .border(1.dp, Color(0xAA515151), RoundedCornerShape(8.dp))
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (bannerUri != null) {
                AsyncImage(
                    model = bannerUri,
                    contentDescription = "Banner do desafio",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color(0xFF238D25))
                    Text("Adicionar banner", color = Color(0xFF238D25), fontFamily = Inter)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Aceitar registros manuais", fontFamily = Inter, fontWeight = FontWeight.Bold)
                Text(
                    "Recomendado deixar desabilitado pela segurança da competição.",
                    color = Color.Gray,
                    fontFamily = Inter,
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = allowManualEntries,
                onCheckedChange = onAllowManualEntriesChange,
                colors = zenithSwitchColors()
            )
        }
    }
}

@Composable
private fun SelectButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFEAF3DE) else Color(0xFFF5F5F5),
            contentColor = if (selected) Color(0xFF238D25) else Color(0xFF1A1A1A)
        ),
        border = BorderStroke(1.dp, if (selected) Color(0xFF238D25) else Color(0xFFE0E0E0)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = label,
            fontFamily = Inter,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) Color(0xFFEAF3DE) else Color(0xFFF5F5F5),
                RoundedCornerShape(10.dp)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Color(0xFF238D25) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                color = if (selected) Color(0xFF238D25) else Color(0xFF1A1A1A)
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF667066)
            )
        )
    }
}

@Composable
private fun CreateChallengeScaffold(
    step: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String = "Continuar",
    bottomContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(TOTAL_STEPS) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (i == step - 1) 20.dp else 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i < step) Color(0xFF238D25) else Color(0xFFDDDDDD))
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 34.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, color = Color.Gray),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .weight(0.36f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Black
                ),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Text(
                    "Voltar",
                    color = Black,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier
                    .weight(0.64f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF238D25),
                    disabledContainerColor = Color(0xFFC7D8C7)
                ),
                border = BorderStroke(1.dp, Color(0xFF238D25))
            ) {
                bottomContent?.invoke() ?: Text(
                    text = nextLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W600
                    )
                )
            }
        }
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseDate(value: String): LocalDate? {
    return runCatching { LocalDate.parse(value, dateFormatter) }.getOrNull()
}

private fun objectiveMetricFor(rankingType: String): String? = when (rankingType) {
    "menor_tempo", "menor_pace" -> "distancia"
    "maior_distancia" -> "tempo"
    else -> null
}

private fun goalInputLabel(rankingType: String): String = when (rankingType) {
    "maior_distancia" -> "Tempo alvo em minutos"
    else -> "Distância alvo em km"
}

@Preview(showBackground = true)
@Composable
fun CreateChallengePreview() {
    ZenithTheme {
        CreateChallengeContent(onFinish = {})
    }
}
