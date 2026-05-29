package br.com.zenith.ui.screens.ranking

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.R
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Desafio
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.ranking.ChallengeRankingEntry
import br.com.zenith.viewmodels.ranking.RankingUiState
import br.com.zenith.viewmodels.ranking.RankingViewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage

@Composable
fun RankingScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: RankingViewModel = viewModel()
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()
        var showCreateDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.fetchDesafios(context)
        }

        RankingContent(
            uiState = uiState,
            isLoading = isLoading,
            isSaving = isSaving,
            onCreateClick = { showCreateDialog = true },
            onOpenChallenge = viewModel::abrirDesafio,
            onBackToChallenges = viewModel::voltarParaDesafios,
            onJoinChallenge = { viewModel.participarDesafio(it, context) },
            onOpenProfile = { profile ->
                if (profile.id == uiState.currentUserId) {
                    navController.navigate("profile")
                } else {
                    navController.navigate("user_profile/${profile.id}")
                }
            }
        )

        if (showCreateDialog) {
            CriarDesafioDialog(
                isSaving = isSaving,
                onDismiss = { if (!isSaving) showCreateDialog = false },
                onCreate = { titulo, descricao, tipo, meta, dias ->
                    viewModel.criarDesafio(
                        titulo = titulo,
                        descricao = descricao,
                        tipo = tipo,
                        meta = meta,
                        dias = dias,
                        context = context
                    )
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun RankingContent(
    uiState: RankingUiState,
    isLoading: Boolean,
    isSaving: Boolean,
    onCreateClick: () -> Unit,
    onOpenChallenge: (String) -> Unit,
    onBackToChallenges: () -> Unit,
    onJoinChallenge: (Desafio) -> Unit,
    onOpenProfile: (Profile) -> Unit
) {
    Scaffold(containerColor = Color.White) { padding ->
        if (isLoading) {
            CenteredZenithLoading(
                modifier = Modifier
                    .background(Color.White)
                    .padding(padding)
                    .statusBarsPadding()
            )
            return@Scaffold
        }

        val selectedRanking = uiState.selectedRanking
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 19.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedRanking == null) {
                item { DesafiosHeader(onCreateClick = onCreateClick) }

                if (uiState.desafios.isEmpty()) {
                    item { EmptyState("Nenhum desafio cadastrado ainda") }
                } else {
                    items(uiState.desafios, key = { it.id }) { desafio ->
                        DesafioRow(
                            desafio = desafio,
                            participantes = uiState.participantes(desafio.id),
                            participando = uiState.participando(desafio.id),
                            isSaving = isSaving,
                            onOpen = { onOpenChallenge(desafio.id) },
                            onJoin = { onJoinChallenge(desafio) }
                        )
                    }
                }
            } else {
                val desafio = selectedRanking.desafio
                item {
                    ChallengeHeader(
                        desafio = desafio,
                        participantes = uiState.participantes(desafio.id),
                        participando = uiState.participando(desafio.id),
                        isSaving = isSaving,
                        onBack = onBackToChallenges,
                        onJoin = { onJoinChallenge(desafio) }
                    )
                }

                selectedRanking.meuEntry(uiState.currentUserId)?.let { entry ->
                    item {
                        MinhaPosicaoCard(
                            position = selectedRanking.minhaPosicao(uiState.currentUserId) ?: 0,
                            entry = entry,
                            desafio = desafio
                        )
                    }
                }

                item { SectionTitle("Ranking do desafio") }

                if (selectedRanking.entries.isEmpty()) {
                    item { EmptyState("Nenhuma atividade vinculada a este desafio ainda") }
                } else {
                    items(selectedRanking.entries, key = { it.profile.id }) { entry ->
                        ChallengeRankingRow(
                            position = selectedRanking.entries.indexOf(entry) + 1,
                            entry = entry,
                            desafio = desafio,
                            isCurrentUser = entry.profile.id == uiState.currentUserId,
                            onClick = { onOpenProfile(entry.profile) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesafiosHeader(onCreateClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Desafios",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Abra um desafio para ver o ranking",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF6F6C6C)
                )
            )
        }
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Criar desafio", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ChallengeHeader(
    desafio: Desafio,
    participantes: Int,
    participando: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onJoin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = desafio.titulo.ifBlank { "Desafio" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatMeta(desafio.meta)} ${desafio.unidade} - $participantes participantes",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            }
        }

        desafio.descricao?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF4E4E4E)
                )
            )
        }

        if (!participando) {
            OutlinedButton(
                onClick = onJoin,
                enabled = !isSaving && desafio.id.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Entrar no desafio", fontFamily = Inter, color = Green)
            }
        }
    }
}

@Composable
private fun MinhaPosicaoCard(
    position: Int,
    entry: ChallengeRankingEntry,
    desafio: Desafio
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEAF6EA), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFB7DEB8), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Green, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sua posi\u00e7\u00e3o",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Inter, color = Color(0xFF4E5D4F))
            )
            Text(
                text = "#$position com ${formatMeta(entry.progresso)} ${desafio.unidade}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun DesafioRow(
    desafio: Desafio,
    participantes: Int,
    participando: Boolean,
    isSaving: Boolean,
    onOpen: () -> Unit,
    onJoin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
            .clickable { onOpen() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Groups, contentDescription = null, tint = Green, modifier = Modifier.size(30.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = desafio.titulo.ifBlank { "Desafio" },
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            desafio.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Inter, color = Color(0xFF6F6C6C)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                AssistChip(
                    onClick = onOpen,
                    label = { Text("${formatMeta(desafio.meta)} ${desafio.unidade}", fontFamily = Inter) }
                )
                AssistChip(
                    onClick = onOpen,
                    label = { Text("$participantes participantes", fontFamily = Inter) }
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (participando) {
            AssistChip(onClick = onOpen, label = { Text("Ranking", fontFamily = Inter) })
        } else {
            OutlinedButton(
                onClick = onJoin,
                enabled = !isSaving && desafio.id.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Entrar", fontFamily = Inter, color = Green)
            }
        }
    }
}

@Composable
private fun ChallengeRankingRow(
    position: Int,
    entry: ChallengeRankingEntry,
    desafio: Desafio,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 78.dp)
            .background(if (isCurrentUser) Color(0xFFF0FAF0) else Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, if (isCurrentUser) Color(0xFFB7DEB8) else Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(rankColor(position)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = position.toString(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        RankingAvatar(entry.profile)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentUser) "${entry.profile.displayName} (voc\u00ea)" else entry.profile.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.atividades} atividades - ${entry.minutos} min - ${entry.verificadas} verificadas",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Inter, color = Color(0xFF6F6C6C)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${formatMeta(entry.progresso)} ${desafio.unidade}",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold, color = Green)
        )
    }
}

@Composable
private fun CriarDesafioDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String?, String, Double, Int) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("atividades") }
    var meta by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Criar desafio", fontFamily = Inter, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("T\u00edtulo") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = inputColors()
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descri\u00e7\u00e3o") },
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp),
                    colors = inputColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "atividades" to "Atividades",
                        "minutos" to "Minutos",
                        "distancia" to "Dist\u00e2ncia"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = tipo == value,
                            onClick = { tipo = value },
                            label = { Text(label, fontFamily = Inter) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = meta,
                        onValueChange = { meta = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                        label = { Text("Meta") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = inputColors()
                    )
                    OutlinedTextField(
                        value = dias,
                        onValueChange = { dias = it.filter(Char::isDigit) },
                        label = { Text("Dias") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = inputColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = titulo.trim()
                    val metaValue = meta.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val diasValue = dias.toIntOrNull() ?: 0
                    if (cleanTitle.isNotBlank() && metaValue > 0 && diasValue > 0) {
                        onCreate(cleanTitle, descricao.takeIf { it.isNotBlank() }, tipo, metaValue, diasValue)
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Criar", fontFamily = Inter)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar", fontFamily = Inter)
            }
        }
    )
}

@Composable
private fun RankingAvatar(profile: Profile) {
    if (profile.pictureHash != null) {
        val url = SupabaseConfig.getClient().storage.from("profiles").publicUrl(profile.pictureHash)
        AsyncImage(
            model = url,
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF2B792C), CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.profile_picture),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF2B792C), CircleShape)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
    )
}

@Composable
private fun EmptyState(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = Inter,
            color = Color(0xFF6F6C6C)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green,
    focusedLabelColor = Green,
    cursorColor = Green
)

private fun rankColor(position: Int): Color {
    return when (position) {
        1 -> Color(0xFFE0A800)
        2 -> Color(0xFF8A94A6)
        3 -> Color(0xFFB86E32)
        else -> Green
    }
}

private fun formatMeta(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}
