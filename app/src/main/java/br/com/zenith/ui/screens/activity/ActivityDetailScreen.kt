package br.com.zenith.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.Exercicio
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.components.profile.activityGroup
import br.com.zenith.ui.components.profile.activityType
import br.com.zenith.ui.components.profile.ChallengeActivitySummary
import br.com.zenith.ui.components.profile.challengeModeLabel
import br.com.zenith.ui.components.profile.formattedActivityValue
import br.com.zenith.ui.components.profile.formattedDuration
import br.com.zenith.ui.components.profile.tempoRelativo
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithDateTimeField
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull

@Composable
fun ActivityDetailScreen(navController: NavController, atividadeId: String?) {
    val viewModel: ActivityViewModel = viewModel()
    val context = LocalContext.current
    val atividades by viewModel.atividades.collectAsState()
    val desafios by viewModel.desafios.collectAsState()
    val exercicios by viewModel.exercicios.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    LaunchedEffect(Unit) {
        if (atividades.isEmpty()) viewModel.fetchAtividades(context)
    }

    val atividade = atividades.find { it.id == atividadeId }

    if (atividade == null) {
        CenteredZenithLoading()
        return
    }

    var editando by remember(atividade.id) { mutableStateOf(false) }
    val desafio = atividade.desafioId?.let { desafios[it] }

    LaunchedEffect(editando) {
        if (editando && exercicios.isEmpty()) viewModel.fetchExercicios(context)
    }

    ZenithTheme {
        if (editando && !atividade.verificada && atividade.desafioId == null) {
            EditActivityScreenContent(
                atividade = atividade,
                exercicios = exercicios,
                isSaving = isSaving,
                onBack = { editando = false },
                onSave = { exercicioId, valor, duracaoMin, titulo, data, nota, intensidade, humor ->
                    viewModel.editarAtividade(
                        atividadeId = atividade.id,
                        exercicioId = exercicioId,
                        valor = valor,
                        duracaoMin = duracaoMin,
                        titulo = titulo,
                        data = data,
                        nota = nota,
                        intensidade = intensidade,
                        humor = humor,
                        context = context
                    ) {
                        editando = false
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
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
                            if (!atividade.verificada && atividade.desafioId == null) {
                                IconButton(onClick = { editando = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = Color(0xFF238D25)
                                    )
                                }
                            }
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

                            Text(
                                text = activityGroup(atividade),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )

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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DetailChip(
                                    label = if (atividade.verificada) "Verificada" else "Manual",
                                    color = if (atividade.verificada) Color(0xFF238D25) else Color.Gray
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
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80)
            )
        } else {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(initialPoint, 16f))
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
        Json.parseToJsonElement(rota).jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
            val lng = obj["lng"]?.jsonPrimitive?.doubleOrNull
            if (lat != null && lng != null) LatLng(lat, lng) else null
        }
    }.getOrDefault(emptyList())
}

@Composable
fun EditActivityScreenContent(
    atividade: Atividade,
    exercicios: List<Exercicio>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (String, Double, Int?, String?, String, Int?, String?, String?) -> Unit
) {
    val exercicioInicial = atividade.exercicio
    var exercicioId by remember(atividade.id) { mutableStateOf(atividade.exercicioId) }
    var exercicioNome by remember(atividade.id) { mutableStateOf(exercicioInicial?.nome ?: "Exercício") }
    var exercicioUnidade by remember(atividade.id) { mutableStateOf(exercicioInicial?.unidade ?: "") }
    var menuExerciciosAberto by remember { mutableStateOf(false) }

    var titulo by remember(atividade.id) { mutableStateOf(atividade.titulo ?: "") }
    var valor by remember(atividade.id) { mutableStateOf(atividade.valor.toString()) }
    var duracao by remember(atividade.id) { mutableStateOf(atividade.duracaoMin?.toString() ?: "") }
    var data by remember(atividade.id) { mutableStateOf(formatarDataInput(atividade.realizadaEm)) }
    var nota by remember(atividade.id) { mutableStateOf(atividade.nota?.toFloat()) }
    var intensidade by remember(atividade.id) { mutableStateOf(atividade.intensidade) }
    var humor by remember(atividade.id) { mutableStateOf(atividade.humor) }

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
            item {
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
                    value = valor,
                    onValueChange = { valor = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                    label = "$exercicioUnidade realizados *",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            item {
                ZenithTextField(
                    value = duracao,
                    onValueChange = { duracao = it.filter(Char::isDigit) },
                    label = "Duração em minutos (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            item {
                ZenithDateTimeField(
                    value = data,
                    onValueChange = { data = it },
                    label = "Data e horário *",
                    modifier = Modifier.fillMaxWidth()
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
                OptionalSelectionDropdown(
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
                OptionalSelectionDropdown(
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
                        onSave(
                            exercicioId,
                            valorDouble,
                            duracao.toIntOrNull(),
                            titulo.ifBlank { null },
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
private fun OptionalSelectionDropdown(
    selectedValue: String?,
    placeholder: String,
    options: List<Pair<String, String>>,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: placeholder

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(selectedLabel, fontFamily = Inter, color = Color(0xFF238D25))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Não definido", fontFamily = Inter) },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontFamily = Inter) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
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
