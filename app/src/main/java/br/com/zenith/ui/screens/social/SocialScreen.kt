package br.com.zenith.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.app.CustomBottomNavigationBar
import br.com.zenith.ui.components.social.SocialEmptyState
import br.com.zenith.ui.components.social.SocialSectionTitle
import br.com.zenith.ui.components.social.SocialUserRow
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
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
        containerColor = Color.White,
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(3.dp))
                CustomBottomNavigationBar(navController = navController)
            }
        }
    ) { padding ->
        if (isLoading) {
            CenteredZenithLoading(
                modifier = Modifier
                    .background(Color.White)
                    .padding(padding)
                    .statusBarsPadding()
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
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Social",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Buscar por nome ou @usuario") },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF238D25)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillParentMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF238D25),
                        focusedLabelColor = Color(0xFF238D25),
                        cursorColor = Color(0xFF238D25)
                    )
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
