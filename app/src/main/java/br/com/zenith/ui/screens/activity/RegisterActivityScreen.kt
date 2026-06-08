package br.com.zenith.ui.screens.activity

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.TrackingResultHolder
import br.com.zenith.data.models.Exercicio
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithDateTimeField
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.activity.ActivityViewModel
import java.net.URLEncoder

@Composable
fun RegisterActivityScreen(
    navController: NavController,
    exercicioId: String = "",
    exercicioNome: String = "",
    exercicioUnidade: String = "",
    duracaoMin: Int = 0,
    verificada: Boolean = false,
    desafioId: String? = null
) {
    ZenithTheme {
        val viewModel: ActivityViewModel = viewModel()
        val context = LocalContext.current
        val exercicios by viewModel.exercicios.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()

        LaunchedEffect(Unit) { viewModel.fetchExercicios(context) }

        RegisterActivityContent(
            exercicios = exercicios,
            isLoading = isLoading,
            isSaving = isSaving,
            exercicioPreSelecionadoId = exercicioId,
            exercicioPreSelecionadoNome = exercicioNome,
            exercicioPreSelecionadoUnidade = exercicioUnidade,
            duracaoPreenchida = duracaoMin,
            verificada = verificada,
            desafioId = desafioId,
            onSave = { eid, nome, unidade, valor, dur, titulo, data, nota, intensidade, humor, fotoUri, submitDesafioId ->
                viewModel.registrarAtividade(
                    exercicioId = eid,
                    valor = valor,
                    verificada = verificada,
                    duracaoMin = dur,
                    titulo = titulo,
                    data = data,
                    nota = nota,
                    intensidade = intensidade,
                    humor = humor,
                    fotoUri = fotoUri,
                    rota = TrackingResultHolder.result?.rotaJson,
                    desafioId = submitDesafioId,
                    passos = TrackingResultHolder.result?.steps,
                    distanciaBruta = TrackingResultHolder.result?.rawDistanceMeters?.div(1000.0),
                    gpsAccuracyMedia = TrackingResultHolder.result?.averageAccuracyMeters?.toDouble(),
                    gpsPontosAceitos = TrackingResultHolder.result?.acceptedGpsPoints,
                    gpsPontosRejeitados = TrackingResultHolder.result?.rejectedGpsPoints,
                    gpsQualidade = TrackingResultHolder.result?.gpsQuality,
                    context = context
                ) {
                    TrackingResultHolder.result = null
                    val nomeEncoded = URLEncoder.encode(nome, "UTF-8")
                    val unidadeEncoded = URLEncoder.encode(unidade, "UTF-8")
                    val valorEncoded = URLEncoder.encode(valor.toString(), "UTF-8")
                    val duracao = dur ?: 0
                    navController.navigate(
                        "activity_registered/$verificada/$nomeEncoded/$valorEncoded/$unidadeEncoded/$duracao"
                    ) {
                        popUpTo("home") { inclusive = false }
                    }
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartedActivityReviewContent(
    exercicioId: String,
    exercicioNome: String,
    exercicioUnidade: String,
    trackingResult: br.com.zenith.viewmodels.activity.ActiveActivityViewModel.TrackingResult,
    duracaoMin: Int,
    desafioId: String?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (String, String, String, Double, Int?, String?, String, Int?, String?, String?, Uri?, String?) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf<Float?>(null) }
    var intensidade by remember { mutableStateOf<String?>(null) }
    var humor by remember { mutableStateOf<String?>(null) }
    var showSubmitDialog by remember { mutableStateOf(false) }

    val valor = remember(exercicioUnidade, trackingResult) {
        if (exercicioUnidade.lowercase().contains("pass")) {
            trackingResult.steps.toDouble()
        } else {
            trackingResult.distanceMeters / 1000.0
        }
    }
    val data = remember {
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }

    fun submit(submitDesafioId: String?) {
        onSave(
            exercicioId,
            exercicioNome,
            exercicioUnidade,
            valor,
            duracaoMin,
            titulo.ifBlank { null },
            data,
            nota?.toInt(),
            intensidade,
            humor,
            null,
            submitDesafioId
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !isSaving) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Text(
                    text = "Revisar atividade monitorada",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEAF3DE), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = exercicioNome,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF27500A)
                            )
                        )
                        LockedReviewRow("Distância", "%.2f km".format(trackingResult.distanceMeters / 1000f))
                        LockedReviewRow("Duração", "${duracaoMin}min")
                        LockedReviewRow("Passos", trackingResult.steps.toString())
                        LockedReviewRow("GPS", trackingResult.gpsQuality.replaceFirstChar { it.uppercase() })
                        LockedReviewRow("Data", data)
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
                    Text("Intensidade (opcional)", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
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
                    Text("Humor (opcional)", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
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
            }
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
                        if (desafioId.isNullOrBlank()) {
                            submit(null)
                        } else {
                            showSubmitDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (desafioId.isNullOrBlank()) "Salvar atividade" else "Submeter ao desafio",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W600
                        )
                    )
                }
            }
        }

        if (showSubmitDialog) {
            ModalBottomSheet(
                onDismissRequest = { showSubmitDialog = false },
                containerColor = Color.White,
                scrimColor = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Submeter ao desafio?", fontFamily = Inter, fontWeight = FontWeight.Bold)
                    Text(
                        "Os dados monitorados não poderão ser alterados e serão usados no ranking.",
                        fontFamily = Inter
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = {
                                showSubmitDialog = false
                                submit(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Salvar sem desafio", fontFamily = Inter)
                        }
                        Button(
                            onClick = {
                                showSubmitDialog = false
                                submit(desafioId)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                        ) {
                            Text("Submeter", color = Color.White, fontFamily = Inter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = Inter, color = Color(0xFF4E5D4F))
        Text(
            value,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterActivityContent(
    exercicios: List<Exercicio>,
    isLoading: Boolean,
    isSaving: Boolean,
    exercicioPreSelecionadoId: String = "",
    exercicioPreSelecionadoNome: String = "",
    exercicioPreSelecionadoUnidade: String = "",
    duracaoPreenchida: Int = 0,
    verificada: Boolean = false,
    desafioId: String? = null,
    onSave: (String, String, String, Double, Int?, String?, String, Int?, String?, String?, Uri?, String?) -> Unit,
    onBack: () -> Unit
) {
    val temPreSelecionado = exercicioPreSelecionadoId.isNotBlank()
    val trackingResult = TrackingResultHolder.result
    val veioDeTracking = trackingResult != null && temPreSelecionado
    val challengeSubmissionLocked = veioDeTracking && !desafioId.isNullOrBlank()

    var step by remember { mutableIntStateOf(if (temPreSelecionado) 1 else 0) }

    var exercicioSelecionado by remember {
        mutableStateOf(
            if (temPreSelecionado) Exercicio(
                id = exercicioPreSelecionadoId,
                nome = exercicioPreSelecionadoNome,
                unidade = exercicioPreSelecionadoUnidade,
                slug = "",
                icone = ""
            ) else null
        )
    }

    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var valor by remember {
        val unidadeInicial = exercicioPreSelecionadoUnidade.lowercase()
        mutableStateOf(
            when {
                veioDeTracking && unidadeInicial.contains("pass") -> trackingResult.steps.toString()
                veioDeTracking -> "%.2f".format(trackingResult.distanceMeters / 1000f).replace(",", ".")
                else -> ""
            }
        )
    }
    var passos by remember {
        mutableStateOf(if (veioDeTracking) trackingResult.steps.toString() else "")
    }

    var duracaoHoras by remember {
        mutableIntStateOf(if (duracaoPreenchida >= 60) duracaoPreenchida / 60 else 0)
    }
    var duracaoMinutos by remember {
        mutableIntStateOf(if (duracaoPreenchida > 0) duracaoPreenchida % 60 else 0)
    }
    var titulo by remember { mutableStateOf("") }
    var data by remember {
        mutableStateOf(
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        )
    }
    var nota by remember { mutableStateOf<Float?>(null) }
    var intensidade by remember { mutableStateOf<String?>(null) }
    var humor by remember { mutableStateOf<String?>(null) }
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    val filtrados = remember(query, exercicios) {
        if (query.isBlank()) exercicios
        else exercicios.filter { it.nome.contains(query, ignoreCase = true) }
    }

    if (veioDeTracking) {
        StartedActivityReviewContent(
            exercicioId = exercicioPreSelecionadoId,
            exercicioNome = exercicioPreSelecionadoNome,
            exercicioUnidade = exercicioPreSelecionadoUnidade,
            trackingResult = trackingResult,
            duracaoMin = duracaoPreenchida.coerceAtLeast(1),
            desafioId = desafioId,
            isSaving = isSaving,
            onBack = onBack,
            onSave = onSave
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header
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
                IconButton(onClick = {
                    if (step == 1 && !temPreSelecionado) {
                        step = 0
                        exercicioSelecionado = null
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Text(
                    text = if (step == 0) "Qual exercício?" else "Detalhes da atividade",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (!temPreSelecionado) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(2) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (i <= step) Color(0xFF238D25) else Color(0xFFDDDDDD),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CenteredZenithLoading() }

            step == 0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp)
                ) {
                    ZenithTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Buscar exercício",
                        placeholder = "Buscar exercício...",
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF238D25))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrados) { exercicio ->
                            ExercicioItem(exercicio = exercicio, onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                exercicioSelecionado = exercicio
                                step = 1
                            })
                        }
                    }
                }
            }

            step == 1 -> {
                val ex = exercicioSelecionado!!
                val unidadeEhDuracao = remember(ex.unidade) {
                    val unidade = ex.unidade.lowercase()
                    unidade.contains("min") || unidade.contains("dura")
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Card do exercício (sempre)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEAF3DE), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ex.nome,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF27500A)
                                )
                            )
                            Text(
                                text = ex.unidade,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF3B6D11)
                                )
                            )
                        }
                    }

                    // Título
                    item {
                        ZenithTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = "Título (opcional)",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = "Ex: Pedalada confortável"
                        )
                    }
                    if (!unidadeEhDuracao) {
                        item {
                            ZenithTextField(
                                value = valor,
                                onValueChange = {
                                    if (!challengeSubmissionLocked) {
                                        valor = it.filter { char -> char.isDigit() || char == '.' || char == ',' }
                                    }
                                },
                                label = "${ex.unidade} realizados *",
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                readOnly = challengeSubmissionLocked
                            )
                        }
                    }

                    if (veioDeTracking) {
                        item {
                            ZenithTextField(
                                value = passos,
                                onValueChange = { if (!challengeSubmissionLocked) passos = it.filter(Char::isDigit) },
                                label = "Passos",
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                readOnly = challengeSubmissionLocked
                            )
                        }
                    }

                    item {
                        if (challengeSubmissionLocked) {
                            ZenithTextField(
                                value = "${duracaoHoras}h ${duracaoMinutos.toString().padStart(2, '0')}min",
                                onValueChange = {},
                                label = "Duração",
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            ZenithTextField(
                                value = ((duracaoHoras * 60) + duracaoMinutos).takeIf { it > 0 }?.toString().orEmpty(),
                                onValueChange = { input ->
                                    val total = input.filter(Char::isDigit).toIntOrNull() ?: 0
                                    duracaoHoras = total / 60
                                    duracaoMinutos = total % 60
                                },
                                label = "Duração em minutos",
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        ZenithDateTimeField(
                            value = data,
                            onValueChange = { data = it },
                            label = "Data e horário *",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !challengeSubmissionLocked
                        )
                    }

                    // Nota
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

                    // Intensidade (sempre)
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

                    // Humor (sempre)
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

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Botão salvar
        if (step == 1) {
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
                            val duracaoTotal = (duracaoHoras * 60 + duracaoMinutos).takeIf { it > 0 }
                            val unidadeEhDuracaoAtual = exercicioSelecionado!!.unidade
                                .lowercase()
                                .let { it.contains("min") || it.contains("dura") }
                            val v = if (unidadeEhDuracaoAtual) {
                                duracaoTotal?.toDouble() ?: return@Button
                            } else {
                                valor.replace(",", ".").toDoubleOrNull() ?: return@Button
                            }
                            val saveWithChallenge: (String?) -> Unit = { submitDesafioId ->
                                onSave(
                                    exercicioSelecionado!!.id,
                                    exercicioSelecionado!!.nome,
                                    exercicioSelecionado!!.unidade,
                                    v,
                                    duracaoTotal,
                                    titulo.ifBlank { null },
                                    data,
                                    nota?.toInt(),
                                    intensidade,
                                    humor,
                                    null,
                                    submitDesafioId
                                )
                            }
                            if (desafioId.isNullOrBlank()) {
                                saveWithChallenge(null)
                            } else {
                                pendingSave = { saveWithChallenge(desafioId) }
                            }
                        },
                        enabled = exercicioSelecionado
                            ?.unidade
                            ?.lowercase()
                            ?.let { unidade ->
                                if (unidade.contains("min") || unidade.contains("dura")) {
                                    (duracaoHoras * 60 + duracaoMinutos) > 0
                                } else {
                                    valor.isNotBlank()
                                }
                            } ?: false,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Registrar atividade",
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

        if (pendingSave != null) {
            ModalBottomSheet(
                onDismissRequest = { pendingSave = null },
                containerColor = Color.White,
                scrimColor = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Submeter ao desafio?", fontFamily = Inter, fontWeight = FontWeight.Bold)
                    Text(
                        "Esse registro será usado na participação e no ranking do desafio.",
                        fontFamily = Inter
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = {
                                pendingSave = null
                                onSave(
                                    exercicioSelecionado!!.id,
                                    exercicioSelecionado!!.nome,
                                    exercicioSelecionado!!.unidade,
                                    valor.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    (duracaoHoras * 60 + duracaoMinutos).takeIf { it > 0 },
                                    titulo.ifBlank { null },
                                    data,
                                    nota?.toInt(),
                                    intensidade,
                                    humor,
                                    null,
                                    null
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Salvar sem desafio", fontFamily = Inter)
                        }
                        Button(
                            onClick = {
                                val action = pendingSave
                                pendingSave = null
                                action?.invoke()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                        ) {
                            Text("Submeter", color = Color.White, fontFamily = Inter)
                        }
                    }
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
fun TrackingInfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFEAF3DE), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF27500A)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color(0xFF3B6D11)
            )
        )
    }
}

@Composable
fun ExercicioItem(exercicio: Exercicio, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "exercise_item_scale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFEAF3DE) else Color(0xFFF5F5F5),
        animationSpec = tween(120),
        label = "exercise_item_background"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFF238D25) else Color(0xFFE0E0E0),
        animationSpec = tween(120),
        label = "exercise_item_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = exercicio.nome,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter)
        )
        Text(
            text = exercicio.unidade,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
        )
    }
}
