package br.com.zenith.ui.screens.activity

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
        mutableStateOf(
            if (veioDeTracking)
                "%.2f".format(trackingResult.distanceMeters / 1000f).replace(",", ".")
            else ""
        )
    }

    var duracaoHoras by remember {
        mutableStateOf(if (duracaoPreenchida >= 60) (duracaoPreenchida / 60).toString() else "")
    }
    var duracaoMinutos by remember {
        mutableStateOf(
            if (duracaoPreenchida > 0) {
                (duracaoPreenchida % 60).takeIf { it > 0 }?.toString() ?: ""
            } else ""
        )
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
        focusedBorderColor = Color(0xFF238D25),
        focusedLabelColor = Color(0xFF238D25),
        unfocusedLabelColor = Color(0xFF555555),
        focusedPlaceholderColor = Color(0xFF777777),
        unfocusedPlaceholderColor = Color(0xFF777777)
    )

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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        shape = RoundedCornerShape(12.dp),
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
                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it },
                            label = { Text("Título (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            placeholder = { Text("Ex: Pedalada confortável") },
                            colors = inputColors
                        )
                    }

                    // Valor — editável apenas se não veio do tracking
                    if (!veioDeTracking) {
                        item {
                            OutlinedTextField(
                                value = valor,
                                onValueChange = { valor = it },
                                label = { Text("${ex.unidade} realizados *") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = inputColors
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = duracaoHoras,
                                    onValueChange = { duracaoHoras = it.filter(Char::isDigit) },
                                    label = { Text("Horas") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = inputColors
                                )
                                OutlinedTextField(
                                    value = duracaoMinutos,
                                    onValueChange = { input ->
                                        duracaoMinutos = input.filter(Char::isDigit).take(2)
                                    },
                                    label = { Text("Minutos") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = inputColors
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = data,
                                onValueChange = { data = it },
                                label = { Text("Data e horário *") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                placeholder = { Text("dd/MM/yyyy HH:mm") },
                                colors = inputColors
                            )
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TrackingInfoCard(
                                    label = "Distância",
                                    value = "%.2f km".format(trackingResult.distanceMeters / 1000f),
                                    modifier = Modifier.weight(1f)
                                )
                                TrackingInfoCard(
                                    label = "Passos",
                                    value = "${trackingResult.steps}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TrackingInfoCard(
                                    label = "Duração",
                                    value = "$duracaoPreenchida min",
                                    modifier = Modifier.weight(1f)
                                )
                                TrackingInfoCard(
                                    label = "Horário",
                                    value = data,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
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
                            listOf("otimo" to "Ótimo", "ok" to "Ok", "cansado" to "Cansado").forEach { (op, label) ->
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
                            val v = valor.replace(",", ".").toDoubleOrNull() ?: return@Button
                            val duracaoTotal = if (veioDeTracking) {
                                duracaoPreenchida.takeIf { it > 0 }
                            } else {
                                val horas = duracaoHoras.toIntOrNull() ?: 0
                                val minutos = duracaoMinutos.toIntOrNull() ?: 0
                                (horas * 60 + minutos).takeIf { it > 0 }
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
                        enabled = valor.isNotBlank(),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
