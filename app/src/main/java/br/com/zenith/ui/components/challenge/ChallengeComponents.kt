package br.com.zenith.ui.components.challenge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.zenith.R
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Desafio as Challenge
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.components.common.ScreenHeader
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.TextFieldGreen
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.challenge.ChallengeEntry
import br.com.zenith.viewmodels.challenge.ChallengeForumItem
import br.com.zenith.viewmodels.challenge.ChallengeUiState
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChallengeContent(
    uiState: ChallengeUiState,
    isLoading: Boolean,
    isSaving: Boolean,
    onCreateClick: () -> Unit,
    onOpenChallenge: (String) -> Unit,
    onBackToChallenges: () -> Unit,
    onJoinChallenge: (Challenge) -> Unit,
    onCreateForumPost: (String, String, List<Uri>) -> Unit,
    onCreateForumComment: (String, String, String) -> Unit,
    onOpenProfile: (Profile) -> Unit
) {
    var selectedTab by remember { mutableStateOf(ChallengeTab.Ativos) }
    val selectedChallengeDetails = uiState.selectedChallengeDetails
    var selectedDetailTab by remember(selectedChallengeDetails?.challenge?.id) {
        mutableStateOf(ChallengeDetailTab.Participantes)
    }

    Scaffold(containerColor = Color.White) { padding ->
        if (isLoading) {
            CenteredZenithLoading(
                modifier = Modifier
                    .background(Color.White),
                contentPadding = padding,
                respectStatusBars = true
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 19.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedChallengeDetails == null) {
                item {
                    ScreenHeader(
                        title = "Desafios",
                        icon = Icons.Default.EmojiEvents
                    )
                }
                item { CreateChallengeAction(onCreateClick = onCreateClick) }
                item {
                    ChallengeTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                val visibleChallenges = uiState.challenges.filter { challenge ->
                    when (selectedTab) {
                        ChallengeTab.Explorar -> !uiState.isParticipating(challenge.id)
                        ChallengeTab.Ativos -> uiState.isParticipating(challenge.id)
                    }
                }

                if (visibleChallenges.isEmpty()) {
                    val emptyText = when (selectedTab) {
                        ChallengeTab.Explorar -> "Nenhum desafio disponível para explorar"
                        ChallengeTab.Ativos -> "Você ainda não entrou em nenhum desafio"
                    }
                    item { EmptyState(emptyText) }
                } else {
                    items(visibleChallenges, key = { it.id }) { challenge ->
                        ChallengeRow(
                            challenge = challenge,
                            creatorName = uiState.creatorName(challenge.criadorId),
                            participantCount = uiState.participantCount(challenge.id),
                            isParticipating = uiState.isParticipating(challenge.id),
                            isSaving = isSaving,
                            onOpen = { onOpenChallenge(challenge.id) },
                            onJoin = { onJoinChallenge(challenge) }
                        )
                    }
                }
            } else {
                val challenge = selectedChallengeDetails.challenge
                item {
                    ChallengeHeader(
                        challenge = challenge,
                        isParticipating = uiState.isParticipating(challenge.id),
                        isSaving = isSaving,
                        onBack = onBackToChallenges,
                        onJoin = { onJoinChallenge(challenge) }
                    )
                }
                item {
                    ChallengeDetailTabs(
                        selectedTab = selectedDetailTab,
                        onTabSelected = { selectedDetailTab = it }
                    )
                }

                when (selectedDetailTab) {
                    ChallengeDetailTab.Participantes -> {
                        selectedChallengeDetails.userEntry(uiState.currentUserId)?.let { entry ->
                            item {
                                MyPositionCard(
                                    position = selectedChallengeDetails.userPosition(uiState.currentUserId) ?: 0,
                                    entry = entry,
                                    challenge = challenge
                                )
                            }
                        }

                        item { RankingHeader(challenge = challenge) }

                        if (selectedChallengeDetails.entries.isEmpty()) {
                            item { EmptyState("Nenhuma atividade vinculada a este desafio ainda") }
                        } else {
                            items(selectedChallengeDetails.entries, key = { it.profile.id }) { entry ->
                                ChallengeDetailsRow(
                                    position = selectedChallengeDetails.entries.indexOf(entry) + 1,
                                    entry = entry,
                                    challenge = challenge,
                                    isCurrentUser = entry.profile.id == uiState.currentUserId,
                                    onClick = { onOpenProfile(entry.profile) }
                                )
                            }
                        }
                    }
                    ChallengeDetailTab.Informacoes -> {
                        item {
                            ChallengeInformation(
                                challenge = challenge,
                                creatorName = uiState.creatorName(challenge.criadorId),
                                participantCount = uiState.participantCount(challenge.id)
                            )
                        }
                        item { SectionTitle("Fórum") }
                        if (uiState.isParticipating(challenge.id)) {
                            item {
                                ChallengeForumComposer(
                                    isSaving = isSaving,
                                    onPost = { content, images ->
                                        onCreateForumPost(challenge.id, content, images)
                                    }
                                )
                            }
                        }
                        if (uiState.selectedForumPosts.isEmpty()) {
                            item { EmptyState("Nenhuma publicação neste desafio ainda") }
                        } else {
                            items(uiState.selectedForumPosts, key = { it.post.id }) { item ->
                                ChallengeForumPostCard(
                                    item = item,
                                    currentUserParticipating = uiState.isParticipating(challenge.id),
                                    isSaving = isSaving,
                                    onOpenProfile = { profile -> onOpenProfile(profile) },
                                    onComment = { entryId, content ->
                                        onCreateForumComment(challenge.id, entryId, content)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ChallengeTab(val label: String) {
    Ativos("Ativos"),
    Explorar("Explorar")
}

private enum class ChallengeDetailTab(val label: String) {
    Participantes("Ranking"),
    Informacoes("Informações")
}

@Composable
private fun ChallengeTabs(
    selectedTab: ChallengeTab,
    onTabSelected: (ChallengeTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = ChallengeTab.entries.indexOf(selectedTab),
        containerColor = Color.White,
        contentColor = TextFieldGreen,
        divider = {},
        // Adicione o indicator aqui
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(ChallengeTab.entries.indexOf(selectedTab)),
                color = TextFieldGreen // Coloque a cor que você deseja para o seletor aqui
            )
        }
    ) {
        ChallengeTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                selectedContentColor = TextFieldGreen,
                unselectedContentColor = Color(0xEE515151)
            )
        }
    }
}

@Composable
private fun ChallengeDetailTabs(
    selectedTab: ChallengeDetailTab,
    onTabSelected: (ChallengeDetailTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = ChallengeDetailTab.entries.indexOf(selectedTab),
        containerColor = Color.White,
        contentColor = TextFieldGreen
    ) {
        ChallengeDetailTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                selectedContentColor = TextFieldGreen,
                unselectedContentColor = Color(0xEE515151)
            )
        }
    }
}

@Composable
private fun CreateChallengeAction(onCreateClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Criar desafio",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Criar desafio",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun ChallengeHeader(
    challenge: Challenge,
    isParticipating: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onJoin: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                ChallengeActivityLabel(challenge.atividadeDesignada)
                Text(
                    text = challenge.titulo.ifBlank { "Desafio" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatChallengeDate(challenge.inicioEm)} - ${formatChallengeDate(challenge.fimEm)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6F6C6C)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ChallengeBanner(challenge = challenge)
        }

        if (!isParticipating) {
            OutlinedButton(
                onClick = onJoin,
                enabled = !isSaving && challenge.id.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Entrar no desafio", fontFamily = Inter, color = Green)
            }
        }
    }
}

@Composable
private fun RankingHeader(challenge: Challenge) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Ranking do desafio",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF151515)
            )
        )
        Text(
            text = challengeGoalLabel(challenge),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6F6C6C)
            )
        )
    }
}

@Composable
private fun MyPositionCard(
    position: Int,
    entry: ChallengeEntry,
    challenge: Challenge
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
                text = "#$position com ${challengeEntryValue(entry, challenge)}",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun ChallengeRow(
    challenge: Challenge,
    creatorName: String,
    participantCount: Int,
    isParticipating: Boolean,
    isSaving: Boolean,
    onOpen: () -> Unit,
    onJoin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xAA515151), RoundedCornerShape(8.dp))
            .clickable { onOpen() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChallengeActivityLabel(challenge.atividadeDesignada)
            Text(
                text = challenge.titulo.ifBlank { "Desafio" },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xEE515151)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            ChallengeInfoBlock(
                challenge = challenge,
                creatorName = creatorName,
                participantCount = participantCount
            )

            if (!isParticipating) {
                OutlinedButton(
                    onClick = onJoin,
                    enabled = !isSaving && challenge.id.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text("Entrar", fontFamily = Inter, color = TextFieldGreen)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        ChallengeBanner(challenge = challenge)
    }
}

@Composable
private fun ChallengeActivityLabel(activity: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = challengeActivityIcon(activity),
            contentDescription = null,
            tint = TextFieldGreen,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = challengeActivityName(activity),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                color = TextFieldGreen
            )
        )
    }
}

@Composable
private fun ChallengeInfoBlock(
    challenge: Challenge,
    creatorName: String,
    participantCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = creatorName,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color = Color(0xEE515151)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ChallengeMetadataIcon(
            icon = Icons.Default.Groups,
            value = participantCount.toString()
        )
        ChallengeMetadataIcon(
            icon = Icons.Default.Warning,
            value = if (challenge.apenasPremium) "Apenas Premium" else "Casual e Premium"
        )
        ChallengeMetadataIcon(
            icon = Icons.Default.Groups,
            value = challengeVisibilityLabel(challenge)
        )
        ChallengeMetadataIcon(
            icon = Icons.Default.CalendarMonth,
            value = "${formatChallengeDate(challenge.inicioEm)} - ${formatChallengeDate(challenge.fimEm)}"
        )
    }
}

@Composable
private fun ChallengeMetadataIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextFieldGreen,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color(0xEE515151)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChallengeInformation(
    challenge: Challenge,
    creatorName: String,
    participantCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ChallengeBannerLarge(challenge = challenge)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xAA515151), RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChallengeActivityLabel(challenge.atividadeDesignada)
            Text(
                text = challenge.titulo.ifBlank { "Desafio" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xEE1F1F1F)
                )
            )
            ChallengeInfoBlock(
                challenge = challenge,
                creatorName = creatorName,
                participantCount = participantCount
            )
            ChallengeMetadataIcon(
                icon = Icons.Default.EmojiEvents,
                value = challengeGoalLabel(challenge)
            )
            ChallengeMetadataIcon(
                icon = Icons.Default.Warning,
                value = if (challenge.aceitaRegistroManual) "Registro manual permitido" else "Somente atividade monitorada"
            )
            challenge.descricao?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF4E4E4E)
                    )
                )
            }
        }
    }
}

@Composable
private fun ChallengeBannerLarge(challenge: Challenge) {
    val bannerModel = challenge.bannerHash?.let { hash ->
        SupabaseConfig.getClient().storage.from("challenge-banners").publicUrl(hash)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8EFE8))
            .border(1.dp, Color(0xAA515151), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bannerModel != null) {
            AsyncImage(
                model = bannerModel,
                contentDescription = "Banner do desafio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = TextFieldGreen,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun ChallengeForumComposer(
    isSaving: Boolean,
    onPost: (String, List<Uri>) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> imageUris = uris.take(4) }
    val canPost = !isSaving && (content.isNotBlank() || imageUris.isNotEmpty())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ZenithTextField(
            value = content,
            onValueChange = { content = it },
            label = "Publicar no desafio",
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        if (imageUris.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(imageUris, key = { it.toString() }) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Foto selecionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { imageLauncher.launch("image/*") },
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fotos", fontFamily = Inter, color = TextFieldGreen)
            }
            Button(
                onClick = {
                    onPost(content, imageUris)
                    content = ""
                    imageUris = emptyList()
                },
                enabled = canPost,
                colors = ButtonDefaults.buttonColors(containerColor = TextFieldGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Publicar", fontFamily = Inter, color = Color.White)
            }
        }
    }
}

@Composable
private fun ChallengeForumPostCard(
    item: ChallengeForumItem,
    currentUserParticipating: Boolean,
    isSaving: Boolean,
    onOpenProfile: (Profile) -> Unit,
    onComment: (String, String) -> Unit
) {
    var commentText by remember(item.post.id) { mutableStateOf("") }
    val authorName = item.author?.displayName?.takeIf { it.isNotBlank() }
        ?: item.author?.name?.takeIf { it.isNotBlank() }
        ?: "Usuário"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            item.author?.let { profile ->
                Box(modifier = Modifier.clickable { onOpenProfile(profile) }) {
                    ChallengeAvatar(profile)
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = formatForumDate(item.post.createdAt),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            }
        }
        item.post.content?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF3E3E3E)
                )
            )
        }
        if (item.media.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(item.media, key = { it.id }) { media ->
                    val mediaUrl = SupabaseConfig.getClient().storage
                        .from("post-media")
                        .publicUrl(media.storagePath)
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "Foto da publicação",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(156.dp)
                            .height(112.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8EFE8))
                    )
                }
            }
        }
        item.comments.forEach { commentItem ->
            val commentAuthor = commentItem.author?.displayName?.takeIf { it.isNotBlank() }
                ?: commentItem.author?.name?.takeIf { it.isNotBlank() }
                ?: "Usuário"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = commentAuthor,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3E3E3E)
                    )
                )
                Text(
                    text = commentItem.comment.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF4E4E4E)
                    )
                )
            }
        }
        val entryId = item.entry?.id
        if (currentUserParticipating && entryId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenithTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = "Comentar",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onComment(entryId, commentText)
                        commentText = ""
                    },
                    enabled = !isSaving && commentText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TextFieldGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enviar", fontFamily = Inter, color = Color.White)
                }
            }
        }
    }
}

private fun challengeActivityIcon(activity: String) = when (activity.lowercase(Locale.ROOT)) {
    "corrida" -> Icons.AutoMirrored.Filled.DirectionsRun
    "ciclismo" -> Icons.AutoMirrored.Filled.DirectionsBike
    else -> Icons.AutoMirrored.Filled.DirectionsWalk
}

private fun challengeActivityName(activity: String) = when (activity.lowercase(Locale.ROOT)) {
    "corrida" -> "Corrida"
    "ciclismo" -> "Ciclismo"
    else -> "Caminhada"
}

private fun challengeVisibilityLabel(challenge: Challenge): String {
    val visibility = when (challenge.visibilidade) {
        "amigos" -> "Apenas amigos"
        "convite" -> "Por convite"
        else -> "Público"
    }
    val limit = challenge.maxParticipantes?.let { " · até $it" }.orEmpty()
    return visibility + limit
}

private fun challengeGoalLabel(challenge: Challenge): String {
    val objective = challenge.objetivoValor ?: challenge.meta
    return when (challenge.rankingTipo) {
        "menor_tempo" -> "Distância alvo: ${formatGoal(objective)} km · vence menor tempo"
        "maior_distancia" -> "Tempo alvo: ${formatGoal(objective)} min · vence maior distância"
        "menor_pace" -> "Distância alvo: ${formatGoal(objective)} km · vence menor pace"
        "tempo_total" -> "Meta livre · soma tempo"
        else -> "Meta livre · soma distância"
    }
}

private fun challengeEntryValue(entry: ChallengeEntry, challenge: Challenge): String {
    val usesSteps = challenge.metrica == "passos" || challenge.unidade.contains("pass", ignoreCase = true)
    if (usesSteps && challenge.rankingTipo != "menor_tempo" && challenge.rankingTipo != "menor_pace") {
        return "${formatGoal(entry.progress)} passos"
    }
    return when (challenge.rankingTipo) {
        "menor_tempo" -> "${entry.minutes} min"
        "menor_pace" -> {
            val distance = (challenge.objetivoValor ?: challenge.meta).coerceAtLeast(0.01)
            "${formatGoal(entry.minutes / distance)} min/km"
        }
        "maior_distancia", "distancia_total" -> "${formatGoal(entry.progress)} km"
        "tempo_total" -> "${formatGoal(entry.progress)} min"
        else -> "${formatGoal(entry.progress)} ${challenge.unidade}"
    }
}

private fun formatChallengeDate(value: String?): String {
    if (value.isNullOrBlank()) return "Data não definida"
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return runCatching {
        OffsetDateTime.parse(value).format(formatter)
    }.getOrDefault("Data não definida")
}

private fun formatForumDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return runCatching {
        OffsetDateTime.parse(value).format(formatter)
    }.getOrDefault("")
}

@Composable
private fun ChallengeBanner(challenge: Challenge) {
    val bannerModel = challenge.bannerHash?.let { hash ->
        SupabaseConfig.getClient().storage.from("challenge-banners").publicUrl(hash)
    }

    Box(
        modifier = Modifier
            .width(104.dp)
            .aspectRatio(1.35f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8EFE8))
            .border(1.dp, Color(0xAA515151), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bannerModel != null) {
            AsyncImage(
                model = bannerModel,
                contentDescription = "Banner do desafio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = TextFieldGreen,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun ChallengeDetailsRow(
    position: Int,
    entry: ChallengeEntry,
    challenge: Challenge,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .background(if (isCurrentUser) Color(0xFFFFFBED) else Color.White, RoundedCornerShape(5.dp))
            .border(1.dp, if (isCurrentUser) Color(0xFF9A894C) else Color(0xFF3E3E3E), RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$position",
            color = rankColor(position),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(14.dp))
        ChallengeAvatar(entry.profile)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentUser) "${entry.profile.displayName} (voc\u00ea)" else entry.profile.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.activities} atividades - ${entry.minutes} min - ${entry.verifiedActivities} verificadas",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Inter, color = Color(0xFF6F6C6C)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = challengeEntryValue(entry, challenge),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold, color = Color(0xFF777777))
        )
    }
}

@Composable
fun CreateChallengeContent(
    isSaving: Boolean,
    onBack: () -> Unit,
    onCreate: (String, String?, String, String, Boolean, Double, Int, Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("atividades") }
    var designatedActivity by remember { mutableStateOf("caminhada") }
    var premiumOnly by remember { mutableStateOf(false) }
    var goal by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> bannerUri = uri }

    val goalValue = goal.replace(',', '.').toDoubleOrNull() ?: 0.0
    val daysValue = days.toIntOrNull() ?: 0
    val canCreate = title.isNotBlank() && goalValue > 0 && daysValue > 0 && !isSaving

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isSaving) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TextFieldGreen
                )
            }
            Text(
                text = "Criar desafio",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ChallengeBannerPicker(
                    bannerUri = bannerUri,
                    onPickBanner = { bannerLauncher.launch("image/*") }
                )
            }
            item {
                ZenithTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Título"
                )
            }
            item {
                ZenithTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Descrição",
                    minLines = 3,
                    maxLines = 5
                )
            }
            item {
                ChallengeSelectionDropdown(
                    selectedValue = type,
                    label = "Tipo de meta",
                    options = listOf(
                        "atividades" to "Atividades",
                        "minutos" to "Minutos",
                        "distancia" to "Distância"
                    ),
                    onSelected = { type = it }
                )
            }
            item {
                ChallengeSelectionDropdown(
                    selectedValue = designatedActivity,
                    label = "Atividade designada",
                    options = listOf(
                        "caminhada" to "Caminhada",
                        "corrida" to "Corrida",
                        "ciclismo" to "Ciclismo"
                    ),
                    onSelected = { designatedActivity = it }
                )
            }
            item {
                ChallengeBooleanDropdown(
                    selectedValue = premiumOnly,
                    label = "Acesso",
                    options = listOf(
                        false to "Livre",
                        true to "Apenas Premium"
                    ),
                    onSelected = { premiumOnly = it }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZenithTextField(
                        value = goal,
                        onValueChange = { goal = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                        label = "Meta",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    ZenithTextField(
                        value = days,
                        onValueChange = { days = it.filter(Char::isDigit) },
                        label = "Dias",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(92.dp)) }
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
                        onCreate(
                            title.trim(),
                            description.trim().takeIf { it.isNotBlank() },
                            type,
                            designatedActivity,
                            premiumOnly,
                            goalValue,
                            daysValue,
                            bannerUri
                        )
                    },
                    enabled = canCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextFieldGreen)
                ) {
                    Text(
                        "Criar desafio",
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
private fun ChallengeSelectionDropdown(
    selectedValue: String,
    label: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: label

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("$label: $selectedLabel", fontFamily = Inter, color = TextFieldGreen)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel, fontFamily = Inter) },
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
private fun ChallengeBooleanDropdown(
    selectedValue: Boolean,
    label: String,
    options: List<Pair<Boolean, String>>,
    onSelected: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: label

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("$label: $selectedLabel", fontFamily = Inter, color = TextFieldGreen)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel, fontFamily = Inter) },
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
private fun ChallengeBannerPicker(
    bannerUri: Uri?,
    onPickBanner: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8EFE8))
            .border(1.dp, Color(0xAA515151), RoundedCornerShape(8.dp))
            .clickable { onPickBanner() },
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = TextFieldGreen,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Adicionar banner",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xEE515151)
                    )
                )
            }
        }
    }
}

@Composable
private fun ChallengeAvatar(profile: Profile) {
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

private fun rankColor(position: Int): Color {
    return when (position) {
        1 -> Color(0xFFE0A800)
        2 -> Color(0xFF8A94A6)
        3 -> Color(0xFFB86E32)
        else -> Green
    }
}

private fun formatGoal(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}
