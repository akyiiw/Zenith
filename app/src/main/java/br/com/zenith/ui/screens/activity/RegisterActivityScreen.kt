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
import br.com.zenith.viewmodels.activity.ActivityViewModel
import java.net.URLEncoder

@Composable
fun RegisterActivityScreen(
    navController: NavController,
    exercicioId: String = "",
    exercicioNome: String = "",
    exercicioUnidade: String = "",
    duracaoMin: Int = 0,
    verificada: Boolean = false
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
            onSave = { eid, nome, unidade, valor, dur, titulo, data, nota, intensidade, humor, fotoUri ->
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
    onSave: (String, String, String, Double, Int?, String?, String, Int?, String?, String?, Uri?) -> Unit,
    onBack: () -> Unit
) {
    val temPreSelecionado = exercicioPreSelecionadoId.isNotBlank()
    val trackingResult = TrackingResultHolder.result
    val veioDeTracking = trackingResult != null && temPreSelecionado

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

    val filtrados = remember(query, exercicios) {
        if (query.isBlank()) exercicios
        else exercicios.filter { it.nome.contains(query, ignoreCase = true) }
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1C1B1F),
        unfocusedTextColor = Color(0xFF1C1B1F),
        cursorColor = Color(0xFF238D25),
        focusedBorderColor = Color(0xFF238D25),
        focusedLabelColor = Color(0xFF238D25),
        unfocusedLabelColor = Color(0xFF555555),
        focusedPlaceholderColor = Color(0xFF777777),
        unfocusedPlaceholderColor = Color(0xFF777777)
    )
    val inputShape = RoundedCornerShape(11.dp)

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
                    text = if (step == 0) "Qual exercÃ­cio?" else "Detalhes da atividade",
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
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar exercÃ­cio...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF238D25))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = inputShape,
                        singleLine = true,
                        colors = inputColors
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

                    // Card do exercÃ­cio (sempre)
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

                    // TÃ­tulo
                    item {
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("TÃ­tulo (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = inputShape,
                            singleLine = true,
                            placeholder = { Text("Ex: Pedalada confortÃ¡vel") },
                            colors = inputColors
                        )
                    }
                    if (!unidadeEhDuracao) {
                        item {
                            OutlinedTextField(
                                value = valor,
                                onValueChange = { valor = it },
                                label = { Text("${ex.unidade} realizados *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = inputShape,
                                singleLine = true,
                                colors = inputColors
                            )
                        }
                    }

                    if (veioDeTracking) {
                        item {
                            OutlinedTextField(
                                value = passos,
                                onValueChange = { passos = it.filter(Char::isDigit) },
                                label = { Text("Passos") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = inputShape,
                                singleLine = true,
                                colors = inputColors
                            )
                        }
                    }

                    item {
                        DurationPicker(
                            hours = duracaoHoras,
                            minutes = duracaoMinutos,
                            inputShape = inputShape,
                            inputColors = inputColors,
                            onHoursChange = { duracaoHoras = it },
                            onMinutesChange = { duracaoMinutos = it }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = data,
                            onValueChange = { data = it },
                            label = { Text("Data e horÃ¡rio *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = inputShape,
                            singleLine = true,
                            placeholder = { Text("dd/MM/yyyy HH:mm") },
                            colors = inputColors
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("leve", "moderada", "intensa").forEach { op ->
                                FilterChip(
                                    selected = intensidade == op,
                                    onClick = { intensidade = if (intensidade == op) null else op },
                                    label = { Text(op.replaceFirstChar { it.uppercase() }) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF238D25),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Humor (sempre)
                    item {
                        Text(
                            text = "Humor (opcional)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("otimo" to "Ã“timo", "ok" to "Ok", "cansado" to "Cansado").forEach { (op, label) ->
                                FilterChip(
                                    selected = humor == op,
                                    onClick = { humor = if (humor == op) null else op },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF238D25),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // BotÃ£o salvar
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
                                null
                            )
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
    }
}



@Composable
fun DurationPicker(
    hours: Int,
    minutes: Int,
    inputShape: RoundedCornerShape,
    inputColors: TextFieldColors,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val value = when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}min"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}min"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { pickerOpen = true }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text("Duração") },
            placeholder = { Text("00h 00min") },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            shape = inputShape,
            singleLine = true,
            colors = inputColors
        )
    }

    if (pickerOpen) {
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            title = {
                Text(
                    text = "Duração",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerAxis(
                        label = "Horas",
                        value = hours,
                        range = 0..23,
                        onValueChange = onHoursChange,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerAxis(
                        label = "Minutos",
                        value = minutes,
                        range = 0..59,
                        onValueChange = onMinutesChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text("Ok", color = Color(0xFF238D25), fontFamily = Inter)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onHoursChange(0)
                        onMinutesChange(0)
                    }
                ) {
                    Text("Limpar", color = Color.Gray, fontFamily = Inter)
                }
            }
        )
    }
}

@Composable
private fun TimePickerAxis(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF7F7F7), RoundedCornerShape(11.dp))
            .border(0.8.dp, Color(0xFFE0E0E0), RoundedCornerShape(11.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextButton(onClick = {
            onValueChange(if (value >= range.last) range.first else value + 1)
        }) {
            Text("+", color = Color(0xFF238D25), fontWeight = FontWeight.Bold)
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color(0xFF555555)
            )
        )
        TextButton(onClick = {
            onValueChange(if (value <= range.first) range.last else value - 1)
        }) {
            Text("-", color = Color(0xFF238D25), fontWeight = FontWeight.Bold)
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

