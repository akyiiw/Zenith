package br.com.zenith.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Exercicio
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.components.profile.activityType
import br.com.zenith.ui.components.profile.ChallengeActivitySummary
import br.com.zenith.ui.components.profile.challengeModeLabel
import br.com.zenith.ui.components.profile.formattedActivityValue
import br.com.zenith.ui.components.profile.formattedDuration
import br.com.zenith.ui.components.profile.mentionLabel
import br.com.zenith.ui.components.profile.tempoRelativo
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithDateTimeField
import br.com.zenith.ui.theme.items.ZenithDurationField
import br.com.zenith.ui.theme.items.ZenithOptionField
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.activity.ActivityViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.ceil

@Composable
fun ActivityDetailScreen(navController: NavController, atividadeId: String?) {
    val viewModel: ActivityViewModel = viewModel()
    val context = LocalContext.current
    val atividades by viewModel.atividades.collectAsState()
    val desafios by viewModel.desafios.collectAsState()
    val exercicios by viewModel.exercicios.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(atividadeId) {
        if (atividadeId != null) {
            viewModel.fetchAtividadePorId(atividadeId, context)
        } else if (atividades.isEmpty()) {
            viewModel.fetchAtividades(context)
        }
    }

    val atividade = detailState.activity ?: atividades.find { it.id == atividadeId }

    if (atividade == null) {
        if (detailState.errorMessage != null || atividadeId == null) {
            ActivityDetailError(
                message = detailState.errorMessage ?: "Atividade inválida.",
                onBack = { navController.popBackStack() },
                onRetry = atividadeId?.let { id ->
                    { viewModel.fetchAtividadePorId(id, context) }
                }
            )
        } else {
            CenteredZenithLoading()
        }
        return
    }

    var editando by remember(atividade.id) { mutableStateOf(false) }
    var confirmarDelete by remember(atividade.id) { mutableStateOf(false) }
    val desafio = atividade.desafioId?.let { desafios[it] }
    val currentUserId = SupabaseConfig.getClient().auth.currentUserOrNull()?.id
    val ownsActivity = atividade.userId == currentUserId
    val canEditMetrics = ownsActivity &&
        !atividade.verificada &&
        atividade.desafioId == null
    val canEditActivity = ownsActivity

    LaunchedEffect(editando) {
        if (editando && canEditMetrics && exercicios.isEmpty()) viewModel.fetchExercicios(context)
    }

    ZenithTheme {
        if (editando && canEditActivity) {
            EditActivityScreenContent(
                atividade = atividade,
                exercicios = exercicios,
                isSaving = isSaving,
                canEditMetrics = canEditMetrics,
                onBack = { editando = false },
                onSave = { exercicioId, valor, duracaoMin, titulo, descricao, data, nota, intensidade, humor ->
                    if (canEditMetrics) {
                        viewModel.editarAtividade(
                            atividadeId = atividade.id,
                            exercicioId = exercicioId,
                            valor = valor,
                            duracaoMin = duracaoMin,
                            titulo = titulo,
                            descricao = descricao,
                            data = data,
                            nota = nota,
                            intensidade = intensidade,
                            humor = humor,
                            context = context
                        ) {
                            editando = false
                        }
                    } else {
                        viewModel.editarMetadadosAtividade(
                            atividadeId = atividade.id,
                            titulo = titulo,
                            descricao = descricao,
                            nota = nota,
                            intensidade = intensidade,
                            humor = humor,
                            context = context
                        ) {
                            editando = false
                        }
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAF8))
                    .statusBarsPadding()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = Color(0xFF238D25)
                                )
                            }
                            Text(
                                text = atividade.titulo ?: atividade.exercicio?.nome ?: "Atividade",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                            if (ownsActivity) {
                                IconButton(onClick = { editando = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = Color(0xFF238D25)
                                    )
                                }
                                IconButton(onClick = { confirmarDelete = true }, enabled = !isSaving) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Apagar",
                                        tint = Color(0xFF9B2D20)
                                    )
                                }
                            }
                        }
                        detailState.authorUsername?.takeIf { it.isNotBlank() }?.let { username ->
                            Text(
                                text = "Completada por @$username",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF536057)
                                ),
                                modifier = Modifier.padding(horizontal = 72.dp)
                            )
                        }
                    }

                    item {
                        // Card principal â€” igual ao RecentActivityCard mas expandido
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = tempoRelativo(atividade.realizadaEm),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = activityType(atividade),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF238D25),
                                    fontWeight = FontWeight.W600
                                )
                            )

                            detailState.groupNames.takeIf { it.isNotEmpty() }?.let { groups ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = groups.joinToString(prefix = "Grupo: "),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF238D25),
                                        fontWeight = FontWeight.W600
                                    )
                                )
                            }
                            detailState.acceptedMentions.takeIf { it.isNotEmpty() }?.let { mentions ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mentionLabel(mentions),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = Inter,
                                        color = Color(0xFF536057)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = atividade.titulo ?: "Atividade registrada:",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.W700
                                )
                            )

                            Text(
                                text = formattedActivityValue(atividade),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 40.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.W700,
                                    color = Color(0xFF1B820E)
                                )
                            )

                            Text(
                                text = "Duracao: ${formattedDuration(atividade.duracaoMin)}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )

                            Text(
                                text = "Nota do usuario: ${atividade.nota?.let { "$it/10" } ?: "-"}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (atividade.verificada) {
                                DetailChip(
                                    label = "Verificada",
                                    color = Color(0xFF238D25)
                                )
                            }

                            ChallengeActivitySummary(desafio = desafio, atividade = atividade)
                        }
                    }

                    item {
                        ActivityRouteMap(rota = atividade.rota)
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // Informações detalhadas
                    item {
                        DetailSection(title = "Detalhes") {
                            atividade.intensidade?.let {
                                DetailRow(
                                    label = "Intensidade",
                                    value = it.replaceFirstChar { c -> c.uppercase() }
                                )
                            }
                            atividade.humor?.let {
                                DetailRow(
                                    label = "Humor",
                                    value = when (it) {
                                        "otimo" -> "Ótimo"
                                        "ok" -> "Ok"
                                        "cansado" -> "Cansado"
                                        else -> it
                                    }
                                )
                            }
                            atividade.clima?.let {
                                DetailRow(
                                    label = "Clima",
                                    value = when (it) {
                                        "ensolarado" -> "Ensolarado"
                                        "nublado" -> "Nublado"
                                        "chuvoso" -> "Chuvoso"
                                        "frio" -> "Frio"
                                        else -> it
                                    }
                                )
                            }
                            atividade.realizadaEm?.let {
                                DetailRow(label = "Data", value = formatarData(it))
                            }
                            atividade.desafioId?.let { challengeId ->
                                DetailRow(
                                    label = "Desafio vinculado",
                                    value = desafio?.titulo?.takeIf { it.isNotBlank() } ?: challengeId
                                )
                                desafio?.let {
                                    DetailRow(label = "Regra do ranking", value = challengeModeLabel(it))
                                }
                            }
                        }
                    }
                }
            }
            if (confirmarDelete) {
                AlertDialog(
                    onDismissRequest = { confirmarDelete = false },
                    containerColor = Color.White,
                    title = {
                        Text(
                            "Apagar atividade?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    text = {
                        Text(
                            "Essa ação remove a atividade do perfil, feed e vínculos relacionados.",
                            fontFamily = Inter
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmarDelete = false
                                viewModel.apagarAtividade(atividade.id, context) {
                                    navController.popBackStack()
                                }
                            },
                            enabled = !isSaving
                        ) {
                            Text("Apagar", color = Color(0xFF9B2D20), fontFamily = Inter)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmarDelete = false }) {
                            Text("Cancelar", color = Color.Gray, fontFamily = Inter)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActivityRouteMap(rota: String?) {
    val points = remember(rota) { parseRoutePoints(rota) }
    if (points.isEmpty()) return

    val initialPoint = points.first()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPoint, 16f)
    }

    LaunchedEffect(points) {
        if (points.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            points.forEach { boundsBuilder.include(it) }
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80)
            )
        } else {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(initialPoint, 16f))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Rota realizada",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFFEFEFEF), RoundedCornerShape(12.dp)),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false
            )
        ) {
            if (points.size >= 2) {
                Polyline(
                    points = points,
                    color = Color(0xFF238D25),
                    width = 12f
                )
            }
        }
    }
}

private fun parseRoutePoints(rota: String?): List<LatLng> {
    if (rota.isNullOrBlank()) return emptyList()
    return runCatching {
        val points = Json.parseToJsonElement(rota).jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
            val lng = obj["lng"]?.jsonPrimitive?.doubleOrNull
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }
        if (points.size <= MAX_ROUTE_POINTS) {
            points
        } else {
            val step = ceil(points.size.toDouble() / MAX_ROUTE_POINTS.toDouble()).toInt()
            points.filterIndexed { index, _ -> index % step == 0 }
        }
    }.getOrDefault(emptyList())
}

private const val MAX_ROUTE_POINTS = 400

@Composable
private fun ActivityDetailError(
    message: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)?
) {
    ZenithTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            onRetry?.let {
                Button(onClick = it) {
                    Text("Tentar novamente")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(onClick = onBack) {
                Text("Voltar")
            }
        }
    }
}

@Composable
fun EditActivityScreenContent(
    atividade: Atividade,
    exercicios: List<Exercicio>,
    isSaving: Boolean,
    canEditMetrics: Boolean,
    onBack: () -> Unit,
    onSave: (String, Double, Int?, String?, String?, String, Int?, String?, String?) -> Unit
) {
    val exercicioInicial = atividade.exercicio
    var exercicioId by remember(atividade.id) { mutableStateOf(atividade.exercicioId) }
    var exercicioNome by remember(atividade.id) { mutableStateOf(exercicioInicial?.nome ?: "Exercício") }
    var exercicioUnidade by remember(atividade.id) { mutableStateOf(exercicioInicial?.unidade ?: "") }
    var menuExerciciosAberto by remember { mutableStateOf(false) }

    var titulo by remember(atividade.id) { mutableStateOf(atividade.titulo ?: "") }
    var descricao by remember(atividade.id) { mutableStateOf(atividade.descricao ?: "") }
    var valor by remember(atividade.id) { mutableStateOf(atividade.valor.toString()) }
    var duracao by remember(atividade.id) { mutableIntStateOf(atividade.duracaoMin ?: 0) }
    var data by remember(atividade.id) { mutableStateOf(formatarDataInput(atividade.realizadaEm)) }
    var nota by remember(atividade.id) { mutableStateOf(atividade.nota?.toFloat()) }
    var intensidade by remember(atividade.id) { mutableStateOf(atividade.intensidade) }
    var humor by remember(atividade.id) { mutableStateOf(atividade.humor) }
    var validationError by remember(atividade.id) { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
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
                    text = "Editar atividade",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 76.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (canEditMetrics) item {
                Box {
                    TextButton(onClick = { menuExerciciosAberto = true }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Exercício", color = Color.Gray, fontFamily = Inter)
                            Text(
                                "$exercicioNome $exercicioUnidade",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF238D25)
                                )
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExerciciosAberto,
                        onDismissRequest = { menuExerciciosAberto = false }
                    ) {
                        exercicios.forEach { exercicio ->
                            DropdownMenuItem(
                                text = { Text("${exercicio.nome} ${exercicio.unidade}") },
                                onClick = {
                                    exercicioId = exercicio.id
                                    exercicioNome = exercicio.nome
                                    exercicioUnidade = exercicio.unidade
                                    menuExerciciosAberto = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                ZenithTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = "Título (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                ZenithTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = "Descrição (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    singleLine = false
                )
            }

            if (canEditMetrics) item {
                ZenithTextField(
                    value = valor,
                    onValueChange = { valor = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                    label = "$exercicioUnidade realizados *",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            if (canEditMetrics) item {
                ZenithDurationField(
                    minutes = duracao,
                    onMinutesChange = {
                        validationError = null
                        duracao = it
                    },
                    label = "Duração (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    optional = true,
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }

            if (canEditMetrics) item {
                ZenithDateTimeField(
                    value = data,
                    onValueChange = {
                        validationError = null
                        data = it
                    },
                    label = "Data e horário *",
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    validateSelection = { selected ->
                        val duration = duracao.takeIf { it > 0 }
                        val now = java.time.LocalDateTime.now()
                        when {
                            selected.toLocalDate().isAfter(now.toLocalDate()) -> "A data da atividade não pode ser futura."
                            selected.isAfter(now) -> "O horário da atividade não pode ser futuro."
                            duration != null && selected.plusMinutes(duration.toLong()).isAfter(now) ->
                                "A duração informada termina depois do horário atual."
                            else -> null
                        }
                    }
                )
            }

            item {
                val notaAtual = nota ?: 5f
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nota?.let { "Nota: ${it.toInt()}/10" } ?: "Nota (opcional)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = Inter,
                                color = Color.Gray
                            )
                        )
                        if (nota != null) {
                            TextButton(onClick = { nota = null }) {
                                Text("Remover", color = Color.Gray, fontFamily = Inter)
                            }
                        }
                    }
                    Slider(
                        value = notaAtual,
                        onValueChange = { nota = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF238D25),
                            activeTrackColor = Color(0xFF238D25)
                        )
                    )
                }
            }

            item {
                Text(
                    text = "Intensidade (opcional)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ZenithOptionField(
                    selectedValue = intensidade,
                    placeholder = "Selecionar intensidade",
                    options = listOf(
                        "leve" to "Leve",
                        "moderada" to "Moderada",
                        "intensa" to "Intensa"
                    ),
                    onSelected = { intensidade = it }
                )
            }

            item {
                Text(
                    text = "Humor (opcional)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ZenithOptionField(
                    selectedValue = humor,
                    placeholder = "Selecionar humor",
                    options = listOf(
                        "otimo" to "Ótimo",
                        "ok" to "Ok",
                        "cansado" to "Cansado"
                    ),
                    onSelected = { humor = it }
                )
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
                if (isSaving) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ZenithLoading(modifier = Modifier.size(52.dp), strokeWidth = 18f)
                    }
                } else {
                Button(
                    onClick = {
                        val valorDouble = valor.replace(",", ".").toDoubleOrNull() ?: return@Button
                        val selectedDate = parseActivityEditDateTime(data)
                        val now = java.time.LocalDateTime.now()
                        validationError = when {
                            selectedDate == null -> "Escolha uma data e horário válidos."
                            selectedDate.toLocalDate().isAfter(now.toLocalDate()) -> "A data da atividade não pode ser futura."
                            selectedDate.isAfter(now) -> "O horário da atividade não pode ser futuro."
                            duracao > 0 && selectedDate.plusMinutes(duracao.toLong()).isAfter(now) ->
                                "A duração informada termina depois do horário atual."
                            else -> null
                        }
                        if (validationError != null) return@Button
                        onSave(
                            exercicioId,
                            valorDouble,
                            duracao.takeIf { it > 0 },
                            titulo.ifBlank { null },
                            descricao.ifBlank { null },
                            data,
                            nota?.toInt(),
                            intensidade,
                            humor
                        )
                    },
                    enabled = valor.isNotBlank() && data.isNotBlank() && exercicioId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Text(
                        "Salvar alterações",
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
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600)
        )
    }
}

@Composable
fun DetailChip(label: String, color: Color = Color(0xFF238D25)) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = color)
        )
    }
}

fun formatarData(isoDate: String): String {
    return try {
        val input = java.time.OffsetDateTime.parse(isoDate)
        "%02d/%02d/%d às %02d:%02d".format(
            input.dayOfMonth, input.monthValue, input.year,
            input.hour, input.minute
        )
    } catch (_: Exception) {
        isoDate
    }
}

fun formatarDataInput(isoDate: String?): String {
    return try {
        val input = java.time.OffsetDateTime.parse(isoDate)
        "%02d/%02d/%d %02d:%02d".format(
            input.dayOfMonth, input.monthValue, input.year,
            input.hour, input.minute
        )
    } catch (_: Exception) {
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }
}

private fun parseActivityEditDateTime(value: String): java.time.LocalDateTime? {
    return runCatching {
        java.time.LocalDateTime.parse(
            value,
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        )
    }.getOrNull()
}
