package br.com.zenith.ui.screens.activity

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.Exercicio
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.activity.ActivityViewModel
import br.com.zenith.viewmodels.challenge.ChallengeViewModel
import java.net.URLEncoder
import java.util.Locale

@Composable
fun NewActivityScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: ActivityViewModel = viewModel()
        val challengeViewModel: ChallengeViewModel = viewModel()
        val context = LocalContext.current
        val exercicios by viewModel.exercicios.collectAsState()
        val challengeState by challengeViewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.fetchExercicios(context)
            challengeViewModel.fetchChallenges(context)
        }

        fun navigateStart(exercicio: Exercicio, desafioId: String? = null) {
            val nome = URLEncoder.encode(exercicio.nome, "UTF-8")
            val unidade = URLEncoder.encode(exercicio.unidade, "UTF-8")
            val route = if (desafioId.isNullOrBlank()) {
                "start_activity/${exercicio.id}/$nome/$unidade"
            } else {
                "start_activity/${exercicio.id}/$nome/$unidade/$desafioId"
            }
            navController.navigate(route)
        }

        fun navigateRegister(exercicio: Exercicio) {
            val nome = URLEncoder.encode(exercicio.nome, "UTF-8")
            val unidade = URLEncoder.encode(exercicio.unidade, "UTF-8")
            navController.navigate("register_activity/${exercicio.id}/$nome/$unidade/0")
        }

        NewActivityContent(
            exercicios = exercicios,
            isLoading = isLoading,
            participatingChallenges = challengeState.challenges.filter { challengeState.isParticipating(it.id) },
            onSleepSettings = { navController.navigate("sleep_settings") },
            onIniciar = ::navigateStart,
            onRegistrar = ::navigateRegister,
            onIniciarDesafio = { challenge ->
                val exercise = exercicios.findChallengeExercise(challenge)
                if (exercise == null) {
                    Toast.makeText(context, "Exercício do desafio indisponível", Toast.LENGTH_SHORT).show()
                } else {
                    navigateStart(exercise, challenge.id)
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun NewActivityContent(
    exercicios: List<Exercicio>,
    isLoading: Boolean,
    participatingChallenges: List<Desafio> = emptyList(),
    onSleepSettings: () -> Unit,
    onIniciar: (Exercicio) -> Unit,
    onRegistrar: (Exercicio) -> Unit,
    onIniciarDesafio: (Desafio) -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf<ActivityMode?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf<ActivitySelection?>(null) }
    var sheetActivity by remember { mutableStateOf<ActivitySelection?>(null) }
    var outrasExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val filtrados = remember(query, exercicios) {
        if (query.isBlank()) exercicios else exercicios.filter { it.nome.contains(query, ignoreCase = true) }
    }
    val destaqueCaminhada = remember(exercicios) { exercicios.findFeaturedExercise(FeaturedActivity.Walk) }
    val destaqueCorrida = remember(exercicios) { exercicios.findFeaturedExercise(FeaturedActivity.Run) }
    val destaqueCiclismo = remember(exercicios) { exercicios.findFeaturedExercise(FeaturedActivity.Cycling) }
    val exerciciosEmDestaque = remember(destaqueCaminhada, destaqueCorrida, destaqueCiclismo) {
        listOfNotNull(destaqueCaminhada, destaqueCorrida, destaqueCiclismo).map { it.id }.toSet()
    }
    val outrosExercicios = remember(exercicios, exerciciosEmDestaque) {
        exercicios.filterNot { it.id in exerciciosEmDestaque }
    }

    fun selecionarExercicio(exercicio: Exercicio) {
        focusManager.clearFocus()
        keyboardController?.hide()
        selectedActivity = ActivitySelection.Exercise(exercicio)
    }

    fun selecionarSono() {
        focusManager.clearFocus()
        keyboardController?.hide()
        selectedActivity = ActivitySelection.Sleep
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (mode == null) onBack() else {
                        mode = null
                        query = ""
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Text(
                    text = when (mode) {
                        ActivityMode.Start -> "Iniciar atividade"
                        ActivityMode.Register -> "Registrar atividade"
                        ActivityMode.Challenges -> "Desafios"
                        null -> "Nova atividade"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (mode == ActivityMode.Start || mode == ActivityMode.Register) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar exercício...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF238D25)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(11.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                CenteredZenithLoading()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        mode == null -> {
                            item {
                                FeaturedActivityCard(
                                    title = "Iniciar atividade",
                                    subtitle = "Monitorar distância, tempo e passos",
                                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                    containerColor = Color(0xFFEAF3DE),
                                    iconColor = Color(0xFF238D25),
                                    onClick = { mode = ActivityMode.Start }
                                )
                            }
                            item {
                                FeaturedActivityCard(
                                    title = "Registrar atividade",
                                    subtitle = "Adicionar um registro manual",
                                    icon = Icons.Default.Edit,
                                    containerColor = Color(0xFFF5F5F5),
                                    iconColor = Color(0xFF555555),
                                    onClick = { mode = ActivityMode.Register }
                                )
                            }
                            item {
                                FeaturedActivityCard(
                                    title = "Desafios",
                                    subtitle = "Iniciar atividade vinculada ao ranking",
                                    icon = Icons.Default.EmojiEvents,
                                    containerColor = Color(0xFFFFF7E6),
                                    iconColor = Color(0xFFC78911),
                                    onClick = { mode = ActivityMode.Challenges }
                                )
                            }
                        }
                        mode == ActivityMode.Challenges -> {
                            if (participatingChallenges.isEmpty()) {
                                item { Text("Você ainda não participa de desafios.", color = Color.Gray, fontFamily = Inter) }
                            } else {
                                items(participatingChallenges, key = { it.id }) { challenge ->
                                    ChallengeStartItem(challenge = challenge, onClick = { onIniciarDesafio(challenge) })
                                }
                            }
                        }
                        query.isBlank() -> {
                            item {
                                FeaturedActivityCard(
                                    title = "Sono",
                                    subtitle = null,
                                    icon = Icons.Default.Bedtime,
                                    containerColor = Color(0xFFEAF1FF),
                                    iconColor = Color(0xFF2F5FBA),
                                    enabled = mode == ActivityMode.Register,
                                    onClick = { selecionarSono() }
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .height(1.dp)
                                        .background(Color(0xFFE0E0E0))
                                )
                            }
                            item {
                                FeaturedActivityCard(
                                    title = "Caminhada",
                                    subtitle = destaqueCaminhada?.unidade ?: "Health Connect",
                                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                                    containerColor = Color(0xFFEAF3DE),
                                    iconColor = Color(0xFF238D25),
                                    enabled = destaqueCaminhada != null,
                                    onClick = { destaqueCaminhada?.let(::selecionarExercicio) }
                                )
                            }
                            item {
                                FeaturedActivityCard(
                                    title = "Corrida",
                                    subtitle = destaqueCorrida?.unidade ?: "Health Connect",
                                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                    containerColor = Color(0xFFFFEFE6),
                                    iconColor = Color(0xFFC65418),
                                    enabled = destaqueCorrida != null,
                                    onClick = { destaqueCorrida?.let(::selecionarExercicio) }
                                )
                            }
                            item {
                                FeaturedActivityCard(
                                    title = "Ciclismo",
                                    subtitle = destaqueCiclismo?.unidade ?: "Health Connect",
                                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                                    containerColor = Color(0xFFE7F6F3),
                                    iconColor = Color(0xFF08756A),
                                    enabled = destaqueCiclismo != null,
                                    onClick = { destaqueCiclismo?.let(::selecionarExercicio) }
                                )
                            }
                            item {
                                OtherActivitiesCard(
                                    expanded = outrasExpanded,
                                    count = outrosExercicios.size,
                                    onClick = { outrasExpanded = !outrasExpanded }
                                )
                            }
                            if (outrasExpanded) {
                                items(outrosExercicios) { exercicio ->
                                    ExercicioItem(exercicio = exercicio, onClick = { selecionarExercicio(exercicio) })
                                }
                            }
                        }
                        else -> {
                            items(filtrados) { exercicio ->
                                ExercicioItem(exercicio = exercicio, onClick = { selecionarExercicio(exercicio) })
                            }
                        }
                    }
                }
            }
        }

        val currentSelection = selectedActivity
        LaunchedEffect(currentSelection) {
            if (currentSelection != null) sheetActivity = currentSelection
        }

        if (currentSelection != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { selectedActivity = null }
            )
        }

        AnimatedVisibility(
            visible = currentSelection != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = tween(260), initialOffsetY = { it }),
            exit = slideOutVertically(animationSpec = tween(220), targetOffsetY = { it })
        ) {
            val selection = sheetActivity ?: return@AnimatedVisibility
            val exercise = (selection as? ActivitySelection.Exercise)?.exercicio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = selection.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = sheetSubtitle(mode, exercise, selection.subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )

                Button(
                    onClick = {
                        when {
                            exercise == null -> onSleepSettings()
                            mode == ActivityMode.Start && exercise.isMonitorable() -> onIniciar(exercise)
                            else -> onRegistrar(exercise)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Text(
                        when {
                            exercise == null -> "Configurações de sono"
                            mode == ActivityMode.Start && exercise.isMonitorable() -> "Iniciar"
                            else -> "Registrar atividade"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.W600)
                    )
                }

                if (mode == ActivityMode.Start && exercise?.isMonitorable() == true) {
                    OutlinedButton(
                        onClick = { onRegistrar(exercise) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF238D25))
                    ) {
                        Text(
                            "Registrar atividade",
                            color = Color.Black,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.W600)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FeaturedActivityCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.68f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) iconColor.copy(alpha = 0.22f) else Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.78f)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconColor else Color.Gray,
                    modifier = Modifier.padding(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold, color = Color(0xFF151515))
                )
                subtitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Inter, color = Color(0xFF666666))
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherActivitiesCard(expanded: Boolean, count: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Outras", style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold, color = Color(0xFF151515)))
                Text("$count atividades", style = MaterialTheme.typography.bodySmall.copy(fontFamily = Inter, color = Color.Gray))
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF238D25)
            )
        }
    }
}

@Composable
private fun ChallengeStartItem(challenge: Desafio, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F8F8),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = challenge.titulo.ifBlank { "Desafio" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${challengeActivityName(challenge.atividadeDesignada)} · ${challengeModeLabel(challenge)}",
                color = Color.Gray,
                fontFamily = Inter
            )
        }
    }
}

private sealed class ActivitySelection {
    data class Exercise(val exercicio: Exercicio) : ActivitySelection()
    data object Sleep : ActivitySelection()

    val title: String
        get() = when (this) {
            is Exercise -> exercicio.nome
            Sleep -> "Sono"
        }

    val subtitle: String?
        get() = when (this) {
            is Exercise -> exercicio.unidade
            Sleep -> null
        }
}

private enum class ActivityMode {
    Start,
    Register,
    Challenges
}

private enum class FeaturedActivity {
    Walk,
    Run,
    Cycling
}

private fun List<Exercicio>.findFeaturedExercise(activity: FeaturedActivity): Exercicio? {
    val exactAliases = when (activity) {
        FeaturedActivity.Walk -> setOf("caminhada", "caminhar", "walk", "walking")
        FeaturedActivity.Run -> setOf("corrida", "correr", "run", "running")
        FeaturedActivity.Cycling -> setOf("ciclismo", "bicicleta", "bike", "cycling", "biking")
    }
    val broadMatcher: (String) -> Boolean = when (activity) {
        FeaturedActivity.Walk -> { text -> text.contains("caminhada") || text.contains("caminhar") || text.contains("walk") }
        FeaturedActivity.Run -> { text -> text.contains("corrida") || text.contains("correr") || text.contains("running") || text.contains("run") }
        FeaturedActivity.Cycling -> { text -> text.contains("ciclismo") || text.contains("bicicleta") || text.contains("bike") || text.contains("cycling") || text.contains("biking") }
    }

    return firstOrNull { it.slug.normalizedExerciseText() in exactAliases }
        ?: firstOrNull { it.nome.normalizedExerciseText() in exactAliases }
        ?: firstOrNull { broadMatcher("${it.slug} ${it.nome} ${it.grupo.orEmpty()}".lowercase(Locale.ROOT)) }
}

private fun List<Exercicio>.findChallengeExercise(challenge: Desafio): Exercicio? {
    val activity = when (challenge.atividadeDesignada.lowercase(Locale.ROOT)) {
        "corrida" -> FeaturedActivity.Run
        "ciclismo" -> FeaturedActivity.Cycling
        else -> FeaturedActivity.Walk
    }
    return findFeaturedExercise(activity)
}

private fun Exercicio.isMonitorable(): Boolean {
    val text = "$slug $nome ${grupo.orEmpty()}".lowercase(Locale.ROOT)
    return text.contains("caminhada") ||
        text.contains("walk") ||
        text.contains("corrida") ||
        text.contains("run") ||
        text.contains("ciclismo") ||
        text.contains("bike") ||
        text.contains("cycling")
}

private fun sheetSubtitle(mode: ActivityMode?, exercise: Exercicio?, fallback: String?): String {
    if (exercise == null) return fallback.orEmpty()
    return when {
        mode == ActivityMode.Start && exercise.isMonitorable() -> "Essa atividade pode ser monitorada."
        mode == ActivityMode.Start -> "Essa atividade não possui suporte ao monitoramento."
        else -> fallback.orEmpty()
    }
}

private fun challengeActivityName(activity: String): String = when (activity.lowercase(Locale.ROOT)) {
    "corrida" -> "Corrida"
    "ciclismo" -> "Ciclismo"
    else -> "Caminhada"
}

private fun String.normalizedExerciseText(): String {
    return trim()
        .lowercase(Locale.ROOT)
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")
}

private fun challengeModeLabel(challenge: Desafio): String {
    val objective = challenge.objetivoValor ?: challenge.meta
    return when (challenge.rankingTipo) {
        "menor_tempo" -> "${objective.cleanNumber()} km · menor tempo"
        "maior_distancia" -> "${objective.cleanNumber()} min · maior distância"
        "menor_pace" -> "${objective.cleanNumber()} km · menor pace"
        "tempo_total" -> "livre · tempo total"
        else -> "livre · distância total"
    }
}

private fun Double.cleanNumber(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
}
