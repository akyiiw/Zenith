package br.com.zenith.ui.screens.activity

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import br.com.zenith.data.models.ActivityGroup
import br.com.zenith.data.models.Exercicio
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.components.common.ZenithSheetDragHandle
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithDateTimeField
import br.com.zenith.ui.theme.items.ZenithDurationField
import br.com.zenith.ui.theme.items.ZenithOptionField
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.utils.UnitFormatters
import br.com.zenith.viewmodels.activity.ActivityViewModel
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        val mentionFriends by viewModel.mentionFriends.collectAsState()
        val activityGroups by viewModel.activityGroups.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.fetchExercicios(context)
            viewModel.fetchMentionFriends(context)
            viewModel.fetchActivityGroups(context)
        }

        RegisterActivityContent(
            exercicios = exercicios,
            mentionFriends = mentionFriends,
            activityGroups = activityGroups,
            isLoading = isLoading,
            isSaving = isSaving,
            exercicioPreSelecionadoId = exercicioId,
            exercicioPreSelecionadoNome = exercicioNome,
            exercicioPreSelecionadoUnidade = exercicioUnidade,
            duracaoPreenchida = duracaoMin,
            verificada = verificada,
            desafioId = desafioId,
            onSave = { eid, nome, unidade, valor, dur, titulo, descricao, data, nota, intensidade, humor, fotoUri, publicarNoFeed, submitDesafioId, mentionedFriendIds, activityGroupId, newActivityGroupName ->
                viewModel.registrarAtividade(
                    exercicioId = eid,
                    valor = valor,
                    verificada = verificada,
                    duracaoMin = dur,
                    titulo = titulo,
                    descricao = descricao,
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
                    mentionedFriendIds = mentionedFriendIds,
                    activityGroupId = activityGroupId,
                    newActivityGroupName = newActivityGroupName,
                    publicarNoFeed = publicarNoFeed,
                    context = context
                ) { _ ->
                    TrackingResultHolder.result = null
                    val nomeEncoded = URLEncoder.encode(nome, "UTF-8")
                    val unidadeEncoded = URLEncoder.encode(unidade, "UTF-8")
                    val valorEncoded = URLEncoder.encode(UnitFormatters.compactNumber(valor), "UTF-8")
                    val duracao = dur ?: 0
                    navController.navigate(
                        "activity_registered/$verificada/$nomeEncoded/$valorEncoded/$unidadeEncoded/$duracao"
                    ) {
                        popUpTo("home") { inclusive = false }
                    }
                }
            },
            onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
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
    mentionFriends: List<Profile>,
    activityGroups: List<ActivityGroup>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (String, String, String, Double, Int?, String?, String?, String, Int?, String?, String?, Uri?, Boolean, String?, List<String>, String?, String?) -> Unit
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var nota by rememberSaveable { mutableStateOf<Float?>(null) }
    var intensidade by rememberSaveable { mutableStateOf<String?>(null) }
    var humor by rememberSaveable { mutableStateOf<String?>(null) }
    var mentionedFriendIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedActivityGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var newActivityGroupName by rememberSaveable { mutableStateOf("") }
    var publicarNoFeed by rememberSaveable { mutableStateOf(false) }
    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val hasChanges = titulo.isNotBlank() ||
        descricao.isNotBlank() ||
        nota != null ||
        intensidade != null ||
        humor != null ||
        mentionedFriendIds.isNotEmpty() ||
        publicarNoFeed

    val valor = remember(exercicioUnidade, trackingResult) {
        if (exercicioUnidade.lowercase().contains("pass")) {
            trackingResult.steps.toDouble()
        } else {
            trackingResult.distanceMeters / 1000.0
        }
    }
    val data = rememberSaveable(duracaoMin) {
        java.time.LocalDateTime.now()
            .minusMinutes(duracaoMin.toLong())
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
            descricao.ifBlank { null },
            data,
            nota?.toInt(),
            intensidade,
            humor,
            null,
            publicarNoFeed,
            submitDesafioId,
            mentionedFriendIds,
            selectedActivityGroupId,
            newActivityGroupName
        )
    }
    fun requestBack() {
        if (hasChanges && !isSaving) {
            showDiscardDialog = true
        } else if (!isSaving) {
            onBack()
        }
    }

    BackHandler(enabled = !isSaving) {
        requestBack()
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
                IconButton(onClick = { requestBack() }, enabled = !isSaving) {
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
                        LockedReviewRow("Distância", UnitFormatters.kilometersWithSpace(trackingResult.distanceMeters / 1000.0))
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
                    Text("Humor (opcional)", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Publicar no feed", fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Amigos poderão ver essa atividade na Home.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Inter,
                                    color = Color.Gray
                                )
                            )
                        }
                        Switch(
                            checked = publicarNoFeed,
                            onCheckedChange = { publicarNoFeed = it },
                            enabled = !isSaving,
                            colors = zenithSwitchColors()
                        )
                    }
                }

                item {
                    ActivityGroupSelector(
                        groups = activityGroups,
                        selectedGroupId = selectedActivityGroupId,
                        newGroupName = newActivityGroupName,
                        onSelectedGroupChange = {
                            selectedActivityGroupId = it
                            if (it != null) newActivityGroupName = ""
                        },
                        onNewGroupNameChange = {
                            newActivityGroupName = it
                            if (it.isNotBlank()) selectedActivityGroupId = null
                        }
                    )
                }

                item {
                    FriendMentionSelector(
                        friends = mentionFriends,
                        selectedIds = mentionedFriendIds.toSet(),
                        onToggle = { friendId ->
                            mentionedFriendIds = if (friendId in mentionedFriendIds) {
                                mentionedFriendIds - friendId
                            } else {
                                mentionedFriendIds + friendId
                            }
                        }
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
                scrimColor = Color.Transparent,
                dragHandle = { ZenithSheetDragHandle() }
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

        if (showDiscardDialog) {
            DiscardActivityChangesSheet(
                onDismiss = { showDiscardDialog = false },
                onConfirm = {
                    showDiscardDialog = false
                    onBack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscardActivityChangesSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Transparent,
        dragHandle = { ZenithSheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Descartar alterações?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    color = Color(0xFF111111)
                )
            )
            Text(
                text = "As informações adicionadas nessa revisão serão perdidas.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF667066)
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Text("Continuar", color = Color(0xFF1A1A1A), fontFamily = Inter, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B2D20))
                ) {
                    Text("Descartar", color = Color.White, fontFamily = Inter, fontWeight = FontWeight.Bold)
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
    mentionFriends: List<Profile> = emptyList(),
    activityGroups: List<ActivityGroup> = emptyList(),
    isLoading: Boolean,
    isSaving: Boolean,
    exercicioPreSelecionadoId: String = "",
    exercicioPreSelecionadoNome: String = "",
    exercicioPreSelecionadoUnidade: String = "",
    duracaoPreenchida: Int = 0,
    verificada: Boolean = false,
    desafioId: String? = null,
    onSave: (String, String, String, Double, Int?, String?, String?, String, Int?, String?, String?, Uri?, Boolean, String?, List<String>, String?, String?) -> Unit,
    onBack: () -> Unit
) {
    val temPreSelecionado = exercicioPreSelecionadoId.isNotBlank()
    val trackingResult = TrackingResultHolder.result
    val veioDeTracking = trackingResult != null && temPreSelecionado
    val challengeSubmissionLocked = veioDeTracking && !desafioId.isNullOrBlank()

    var step by rememberSaveable { mutableIntStateOf(if (temPreSelecionado) 1 else 0) }
    var exercicioSelecionadoId by rememberSaveable {
        mutableStateOf(exercicioPreSelecionadoId.takeIf { it.isNotBlank() })
    }
    val exercicioSelecionado = remember(
        exercicioSelecionadoId,
        exercicios,
        exercicioPreSelecionadoId,
        exercicioPreSelecionadoNome,
        exercicioPreSelecionadoUnidade
    ) {
        when {
            temPreSelecionado && exercicioSelecionadoId == exercicioPreSelecionadoId -> Exercicio(
                id = exercicioPreSelecionadoId,
                nome = exercicioPreSelecionadoNome,
                unidade = exercicioPreSelecionadoUnidade,
                slug = "",
                icone = ""
            )
            exercicioSelecionadoId != null -> exercicios.firstOrNull { it.id == exercicioSelecionadoId }
            else -> null
        }
    }

    var query by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var valor by rememberSaveable {
        val unidadeInicial = exercicioPreSelecionadoUnidade.lowercase()
        mutableStateOf(
            when {
                veioDeTracking && unidadeInicial.contains("pass") -> trackingResult.steps.toString()
                veioDeTracking -> UnitFormatters.compactNumber(trackingResult.distanceMeters / 1000.0).replace(",", ".")
                else -> ""
            }
        )
    }
    var passos by rememberSaveable {
        mutableStateOf(if (veioDeTracking) trackingResult.steps.toString() else "")
    }

    var duracaoHoras by rememberSaveable {
        mutableIntStateOf(if (duracaoPreenchida >= 60) duracaoPreenchida / 60 else 0)
    }
    var duracaoMinutos by rememberSaveable {
        mutableIntStateOf(if (duracaoPreenchida > 0) duracaoPreenchida % 60 else 0)
    }
    var titulo by rememberSaveable { mutableStateOf("") }
    var descricao by rememberSaveable { mutableStateOf("") }
    var data by rememberSaveable {
        mutableStateOf(
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        )
    }
    var nota by rememberSaveable { mutableStateOf<Float?>(null) }
    var intensidade by rememberSaveable { mutableStateOf<String?>(null) }
    var humor by rememberSaveable { mutableStateOf<String?>(null) }
    var mentionedFriendIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var selectedActivityGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var newActivityGroupName by rememberSaveable { mutableStateOf("") }
    var publicarNoFeed by rememberSaveable { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

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
            mentionFriends = mentionFriends,
            activityGroups = activityGroups,
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
                        exercicioSelecionadoId = null
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

        AnimatedContent(
            targetState = if (isLoading) -1 else step,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (fadeIn(animationSpec = tween(170)) +
                    slideInHorizontally(
                        animationSpec = tween(240),
                        initialOffsetX = { direction * it / 6 }
                    )) togetherWith
                    (fadeOut(animationSpec = tween(120)) +
                        slideOutHorizontally(
                            animationSpec = tween(200),
                            targetOffsetX = { -direction * it / 8 }
                        ))
            },
            label = "register_activity_step"
        ) { animatedStep ->
            when (animatedStep) {
            -1 -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CenteredZenithLoading() }

            0 -> {
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
                                exercicioSelecionadoId = exercicio.id
                                step = 1
                            })
                        }
                    }
                }
            }

            else -> {
                val ex = exercicioSelecionado
                if (ex == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CenteredZenithLoading()
                    }
                } else {
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
                            ZenithDurationField(
                                minutes = duracaoHoras * 60 + duracaoMinutos,
                                onMinutesChange = { total ->
                                    validationError = null
                                    duracaoHoras = total / 60
                                    duracaoMinutos = total % 60
                                },
                                label = "Duração",
                                modifier = Modifier.fillMaxWidth(),
                                isError = validationError != null,
                                supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                            )
                        }
                    }

                    item {
                        ZenithDateTimeField(
                            value = data,
                            onValueChange = {
                                validationError = null
                                data = it
                            },
                            label = "Data e horário *",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !challengeSubmissionLocked,
                            isError = validationError != null,
                            supportingText = if (challengeSubmissionLocked) null else validationError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            validateSelection = { selected ->
                                val duration = (duracaoHoras * 60 + duracaoMinutos).takeIf { it > 0 }
                                val now = LocalDateTime.now()
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

                    // Humor (sempre)
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

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Publicar no feed", fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Amigos poderão ver essa atividade na Home.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = Inter,
                                            color = Color.Gray
                                        )
                                    )
                                }
                                Switch(
                                    checked = publicarNoFeed,
                                    onCheckedChange = { publicarNoFeed = it },
                                    enabled = !isSaving,
                                    colors = zenithSwitchColors()
                                )
                            }
                            FriendMentionSelector(
                                friends = mentionFriends,
                                selectedIds = mentionedFriendIds.toSet(),
                                onToggle = { friendId ->
                                    mentionedFriendIds = if (friendId in mentionedFriendIds) {
                                        mentionedFriendIds - friendId
                                    } else {
                                        mentionedFriendIds + friendId
                                    }
                                }
                            )
                            ActivityGroupSelector(
                                groups = activityGroups,
                                selectedGroupId = selectedActivityGroupId,
                                newGroupName = newActivityGroupName,
                                onSelectedGroupChange = {
                                    selectedActivityGroupId = it
                                    if (it != null) newActivityGroupName = ""
                                },
                                onNewGroupNameChange = {
                                    newActivityGroupName = it
                                    if (it.isNotBlank()) selectedActivityGroupId = null
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
                }
            }
        }
        }

        // Botão salvar
        AnimatedVisibility(
            visible = step == 1 && !isLoading,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(animationSpec = tween(160)) +
                slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 2 }),
            exit = fadeOut(animationSpec = tween(120)) +
                slideOutVertically(animationSpec = tween(180), targetOffsetY = { it / 2 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            val selectedExercise = exercicioSelecionado ?: return@Button
                            val duracaoTotal = (duracaoHoras * 60 + duracaoMinutos).takeIf { it > 0 }
                            val realizedAt = parseRegisterDateTime(data)
                            val now = LocalDateTime.now()
                            validationError = when {
                                realizedAt == null -> "Escolha uma data e horário válidos."
                                realizedAt.toLocalDate().isAfter(now.toLocalDate()) -> "A data da atividade não pode ser futura."
                                realizedAt.isAfter(now) -> "O horário da atividade não pode ser futuro."
                                duracaoTotal != null && realizedAt.plusMinutes(duracaoTotal.toLong()).isAfter(now) ->
                                    "A duração informada termina depois do horário atual."
                                else -> null
                            }
                            if (validationError != null) return@Button
                            val unidadeEhDuracaoAtual = selectedExercise.unidade
                                .lowercase()
                                .let { it.contains("min") || it.contains("dura") }
                            val v = if (unidadeEhDuracaoAtual) {
                                duracaoTotal?.toDouble() ?: return@Button
                            } else {
                                valor.replace(",", ".").toDoubleOrNull() ?: return@Button
                            }
                            val saveWithChallenge: (String?) -> Unit = { submitDesafioId ->
                                onSave(
                                    selectedExercise.id,
                                    selectedExercise.nome,
                                    selectedExercise.unidade,
                                    v,
                                    duracaoTotal,
                                    titulo.ifBlank { null },
                                    descricao.ifBlank { null },
                                    data,
                                    nota?.toInt(),
                                    intensidade,
                                    humor,
                                    null,
                                    publicarNoFeed,
                                    submitDesafioId,
                                    mentionedFriendIds,
                                    selectedActivityGroupId,
                                    newActivityGroupName
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
                scrimColor = Color.Transparent,
                dragHandle = { ZenithSheetDragHandle() }
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
                                val selectedExercise = exercicioSelecionado ?: return@TextButton
                                pendingSave = null
                                onSave(
                                    selectedExercise.id,
                                    selectedExercise.nome,
                                    selectedExercise.unidade,
                                    valor.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                    (duracaoHoras * 60 + duracaoMinutos).takeIf { it > 0 },
                                    titulo.ifBlank { null },
                                    descricao.ifBlank { null },
                                    data,
                                    nota?.toInt(),
                                    intensidade,
                                    humor,
                                    null,
                                    publicarNoFeed,
                                    null,
                                    mentionedFriendIds,
                                    selectedActivityGroupId,
                                    newActivityGroupName
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
private fun ActivityGroupSelector(
    groups: List<ActivityGroup>,
    selectedGroupId: String?,
    newGroupName: String,
    onSelectedGroupChange: (String?) -> Unit,
    onNewGroupNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Grupo de atividade (opcional)",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = Color.Gray
            )
        )
        ZenithTextField(
            value = newGroupName,
            onValueChange = { value -> onNewGroupNameChange(value.take(48)) },
            label = "Criar novo grupo",
            placeholder = "Ex: Corridas de junho",
            leadingIcon = {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color(0xFF238D25))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        groups.takeIf { it.isNotEmpty() }?.forEach { group ->
            val selected = group.id == selectedGroupId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0xFFEAF3DE) else Color(0xFFF7F7F7))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color(0xFF238D25) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectedGroupChange(if (selected) null else group.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    group.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = Inter,
                                color = Color(0xFF6F6C6C)
                            )
                        )
                    }
                }
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onSelectedGroupChange(if (selected) null else group.id) },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF238D25))
                )
            }
        }
    }
}

@Composable
private fun FriendMentionSelector(
    friends: List<Profile>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    if (friends.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Convidar participantes (opcional)",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = Color.Gray
            )
        )
        Text(
            text = "O amigo precisará aceitar para a atividade aparecer no perfil dele como participação.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color(0xFF6F6C6C)
            )
        )
        friends.forEach { friend ->
            val selected = friend.id in selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0xFFEAF3DE) else Color(0xFFF7F7F7))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color(0xFF238D25) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggle(friend.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = friend.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = "@${friend.name}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Inter,
                            color = Color(0xFF6F6C6C)
                        )
                    )
                }
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggle(friend.id) },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF238D25))
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

private val registerDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun parseRegisterDateTime(value: String): LocalDateTime? {
    return runCatching { LocalDateTime.parse(value, registerDateTimeFormatter) }.getOrNull()
}
