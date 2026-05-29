package br.com.zenith.ui.screens.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import br.com.zenith.data.models.Exercicio
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.activity.ActivityViewModel
import java.net.URLEncoder

@Composable
fun NewActivityScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: ActivityViewModel = viewModel()
        val context = LocalContext.current
        val exercicios by viewModel.exercicios.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        LaunchedEffect(Unit) { viewModel.fetchExercicios(context) }

        NewActivityContent(
            exercicios = exercicios,
            isLoading = isLoading,
            onSleepSettings = {
                navController.navigate("sleep_settings")
            },
            onIniciar = { exercicio ->
                val nome = URLEncoder.encode(exercicio.nome, "UTF-8")
                val unidade = URLEncoder.encode(exercicio.unidade, "UTF-8")
                navController.navigate("start_activity/${exercicio.id}/$nome/$unidade")
            },
            onRegistrar = { exercicio ->
                val nome = URLEncoder.encode(exercicio.nome, "UTF-8")
                val unidade = URLEncoder.encode(exercicio.unidade, "UTF-8")
                navController.navigate("register_activity/${exercicio.id}/$nome/$unidade/0")
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
fun NewActivityContent(
    exercicios: List<Exercicio>,
    isLoading: Boolean,
    onSleepSettings: () -> Unit,
    onIniciar: (Exercicio) -> Unit,
    onRegistrar: (Exercicio) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf<ActivitySelection?>(null) }
    var sheetActivity by remember { mutableStateOf<ActivitySelection?>(null) }
    var outrasExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val filtrados = remember(query, exercicios) {
        if (query.isBlank()) exercicios
        else exercicios.filter { it.nome.contains(query, ignoreCase = true) }
    }
    val destaqueCaminhada = remember(exercicios) { exercicios.findFeaturedExercise(FeaturedActivity.Walk) }
    val destaqueCorrida = remember(exercicios) { exercicios.findFeaturedExercise(FeaturedActivity.Run) }
    val destaqueCiclismo = remember(exercicios) { exercicios.findFeaturedExercise(FeaturedActivity.Cycling) }
    val exerciciosEmDestaque = remember(destaqueCaminhada, destaqueCorrida, destaqueCiclismo) {
        listOfNotNull(destaqueCaminhada, destaqueCorrida, destaqueCiclismo)
            .map { it.id }
            .toSet()
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Text(
                    text = "Qual exercício?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar exercício...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = Color(0xFF238D25))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(11.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                CenteredZenithLoading()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (query.isBlank()) {
                        item {
                            FeaturedActivityCard(
                                title = "Sono",
                                subtitle = null,
                                icon = Icons.Default.Bedtime,
                                containerColor = Color(0xFFEAF1FF),
                                iconColor = Color(0xFF2F5FBA),
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
                                icon = Icons.Default.DirectionsWalk,
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
                                icon = Icons.Default.DirectionsRun,
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
                                icon = Icons.Default.DirectionsBike,
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
                                ExercicioItem(
                                    exercicio = exercicio,
                                    onClick = { selecionarExercicio(exercicio) }
                                )
                            }
                        }
                    } else {
                        items(filtrados) { exercicio ->
                            ExercicioItem(
                                exercicio = exercicio,
                                onClick = { selecionarExercicio(exercicio) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom sheet de ação ao selecionar exercício
        val currentSelection = selectedActivity
        LaunchedEffect(currentSelection) {
            if (currentSelection != null) {
                sheetActivity = currentSelection
            }
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
            enter = slideInVertically(
                animationSpec = tween(260),
                initialOffsetY = { it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { it }
            )
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
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                )
                selection.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (exercise != null) {
                            onIniciar(exercise)
                        } else {
                            onSleepSettings()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Text(
                        if (exercise != null) "Iniciar atividade" else "Configurações de sono",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W600
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (exercise != null) {
                            onRegistrar(exercise)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF238D25))
                ) {
                    Text(
                        "Registrar atividade",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W600
                        )
                    )
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
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) iconColor.copy(alpha = 0.22f) else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.78f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconColor else Color.Gray,
                    modifier = Modifier.padding(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151515)
                    )
                )
                subtitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            color = Color(0xFF666666)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherActivitiesCard(
    expanded: Boolean,
    count: Int,
    onClick: () -> Unit
) {
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
                Text(
                    text = "Outras",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF151515)
                    )
                )
                Text(
                    text = "$count atividades",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color.Gray
                    )
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF238D25)
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

private enum class FeaturedActivity {
    Walk,
    Run,
    Cycling
}

private fun List<Exercicio>.findFeaturedExercise(activity: FeaturedActivity): Exercicio? {
    return firstOrNull { exercicio ->
        val text = "${exercicio.slug} ${exercicio.nome} ${exercicio.grupo.orEmpty()}".lowercase()
        when (activity) {
            FeaturedActivity.Walk -> text.contains("caminhada") ||
                    text.contains("caminhar") ||
                    text.contains("walk")
            FeaturedActivity.Run -> text.contains("corrida") ||
                    text.contains("correr") ||
                    text.contains("running") ||
                    text.contains("run")
            FeaturedActivity.Cycling -> text.contains("ciclismo") ||
                    text.contains("bicicleta") ||
                    text.contains("bike") ||
                    text.contains("cycling") ||
                    text.contains("biking")
        }
    }
}

