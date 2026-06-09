package br.com.zenith.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.models.ActivityMention
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.ScreenHeader
import br.com.zenith.ui.components.social.SocialEmptyState
import br.com.zenith.ui.components.social.SocialSectionTitle
import br.com.zenith.ui.components.social.SocialUserRow
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.social.SocialUiState
import br.com.zenith.viewmodels.social.SocialViewModel

@Composable
fun SocialScreen(navController: NavController) {
    ZenithTheme {
        val viewModel: SocialViewModel = viewModel()
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isSaving by viewModel.isSaving.collectAsState()
        var query by remember { mutableStateOf("") }

        LaunchedEffect(Unit) { viewModel.fetchSocial(context) }

        SocialContent(
            uiState = uiState,
            isLoading = isLoading,
            isSaving = isSaving,
            query = query,
            onQueryChange = { query = it },
            onSendInvite = { viewModel.enviarConvite(it.id, context) },
            onAccept = { viewModel.aceitarConvite(it.id, context) },
            onReject = { viewModel.recusarConvite(it.id, context) },
            onRemove = { viewModel.removerAmizade(it.id, context) },
            onAcceptMention = { mention, showOnProfile ->
                viewModel.aceitarMencao(mention.id, showOnProfile, context)
            },
            onRejectMention = { viewModel.recusarMencao(it.id, context) },
            onOpenProfile = { navController.navigate("user_profile/${it.id}") },
            navController = navController
        )
    }
}

@Composable
private fun SocialContent(
    uiState: SocialUiState,
    isLoading: Boolean,
    isSaving: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSendInvite: (Profile) -> Unit,
    onAccept: (Profile) -> Unit,
    onReject: (Profile) -> Unit,
    onRemove: (Profile) -> Unit,
    onAcceptMention: (ActivityMention, Boolean) -> Unit,
    onRejectMention: (ActivityMention) -> Unit,
    onOpenProfile: (Profile) -> Unit,
    navController: NavController
) {
    val friends = uiState.friends()
    val receivedRequests = uiState.receivedRequests()
    val sentRequests = uiState.sentRequests()
    val searchResults = remember(query, uiState) {
        val term = query.trim()
        if (term.isBlank()) {
            emptyList()
        } else {
            uiState.profiles.filter {
                it.name.contains(term, ignoreCase = true) ||
                    it.displayName.contains(term, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
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
            item {
                ScreenHeader(
                    title = "Social",
                    icon = Icons.Default.Groups
                )
            }

            item {
                ZenithTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = "Buscar por nome ou @usuario",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF238D25)
                        )
                    }
                )
            }

            if (query.isNotBlank()) {
                item { SocialSectionTitle("Encontrar pessoas") }
                if (searchResults.isEmpty()) {
                    item { SocialEmptyState("Nenhum perfil encontrado") }
                } else {
                    items(searchResults, key = { "search-${it.id}" }) { profile ->
                        val friendship = uiState.friendshipWith(profile.id)
                        SocialUserRow(
                            profile = profile,
                            friendship = friendship,
                            currentUserId = uiState.currentUserId,
                            isSaving = isSaving,
                            onProfileClick = { onOpenProfile(profile) },
                            onSendInvite = { onSendInvite(profile) },
                            onAccept = { onAccept(profile) },
                            onReject = { onReject(profile) },
                            onRemove = { onRemove(profile) }
                        )
                    }
                }
            }

            if (uiState.pendingMentions.isNotEmpty()) {
                item { SocialSectionTitle("Menções em atividades") }
                items(uiState.pendingMentions, key = { "mention-${it.id}" }) { mention ->
                    ActivityMentionRequestCard(
                        mention = mention,
                        isSaving = isSaving,
                        onAcceptVisible = { onAcceptMention(mention, true) },
                        onAcceptHidden = { onAcceptMention(mention, false) },
                        onReject = { onRejectMention(mention) }
                    )
                }
            }

            item { SocialSectionTitle("Convites recebidos") }
            if (receivedRequests.isEmpty()) {
                item { SocialEmptyState("Nenhum convite pendente") }
            } else {
                items(receivedRequests, key = { "received-${it.id}" }) { profile ->
                    SocialUserRow(
                        profile = profile,
                        friendship = uiState.friendshipWith(profile.id),
                        currentUserId = uiState.currentUserId,
                        isSaving = isSaving,
                        onProfileClick = { onOpenProfile(profile) },
                        onSendInvite = { onSendInvite(profile) },
                        onAccept = { onAccept(profile) },
                        onReject = { onReject(profile) },
                        onRemove = { onRemove(profile) }
                    )
                }
            }

            item { SocialSectionTitle("Amigos") }
            if (friends.isEmpty()) {
                item { SocialEmptyState("Você ainda não adicionou amigos") }
            } else {
                items(friends, key = { "friend-${it.id}" }) { profile ->
                    SocialUserRow(
                        profile = profile,
                        friendship = uiState.friendshipWith(profile.id),
                        currentUserId = uiState.currentUserId,
                        isSaving = isSaving,
                        onProfileClick = { onOpenProfile(profile) },
                        onSendInvite = { onSendInvite(profile) },
                        onAccept = { onAccept(profile) },
                        onReject = { onReject(profile) },
                        onRemove = { onRemove(profile) }
                    )
                }
            }

            if (sentRequests.isNotEmpty()) {
                item { SocialSectionTitle("Convites enviados") }
                items(sentRequests, key = { "sent-${it.id}" }) { profile ->
                    SocialUserRow(
                        profile = profile,
                        friendship = uiState.friendshipWith(profile.id),
                        currentUserId = uiState.currentUserId,
                        isSaving = isSaving,
                        onProfileClick = { onOpenProfile(profile) },
                        onSendInvite = { onSendInvite(profile) },
                        onAccept = { onAccept(profile) },
                        onReject = { onReject(profile) },
                        onRemove = { onRemove(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityMentionRequestCard(
    mention: ActivityMention,
    isSaving: Boolean,
    onAcceptVisible: () -> Unit,
    onAcceptHidden: () -> Unit,
    onReject: () -> Unit
) {
    val publisherName = mention.publisher?.displayName?.takeIf { it.isNotBlank() }
        ?: mention.publisher?.name
        ?: "Um amigo"
    val activityName = mention.activity?.titulo?.takeIf { it.isNotBlank() }
        ?: mention.activity?.exercicio?.nome
        ?: "uma atividade"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAF8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE3EAE3), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$publisherName mencionou você",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        Text(
            text = "Atividade: $activityName",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Inter,
                color = Color(0xFF536057)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAcceptVisible,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text("Mostrar", color = Color.White, fontFamily = Inter)
            }
            TextButton(
                onClick = onAcceptHidden,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Ocultar", color = Color(0xFF238D25), fontFamily = Inter)
            }
            TextButton(
                onClick = onReject,
                enabled = !isSaving
            ) {
                Text("Recusar", color = Color(0xFFD32F2F), fontFamily = Inter)
            }
        }
    }
}
