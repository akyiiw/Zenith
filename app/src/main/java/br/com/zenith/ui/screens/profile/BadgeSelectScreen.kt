package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.models.Badge
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.components.profile.Badge as BadgePill
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.profile.UserViewModel

@Composable
fun BadgeSelectScreen(navController: NavController) {
    ZenithTheme {
        val userViewModel: UserViewModel = viewModel()
        val context = LocalContext.current
        val badges by userViewModel.badgesDisponiveis.collectAsState()
        val user by userViewModel.userState.collectAsState()
        val isLoading by userViewModel.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            if (user == null) userViewModel.fetchUserProfile(context)
            userViewModel.fetchBadgesDisponiveis(context)
        }

        BadgeSelectContent(
            badges = badges,
            selectedBadgeId = user?.badgeId,
            isLoading = isLoading,
            onSelect = { badge ->
                userViewModel.selecionarBadge(badge.id, context) {
                    navController.popBackStack()
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun BadgeSelectContent(
    badges: List<Badge>,
    selectedBadgeId: Int?,
    isLoading: Boolean,
    onSelect: (Badge) -> Unit,
    onBack: () -> Unit
) {
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
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color(0xFF238D25)
                )
            }
            Text(
                text = "Escolher badge",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isLoading) {
            CenteredZenithLoading()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp),
                contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (badges.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum badge disponível ainda.",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                            )
                        }
                    }
                }

                items(badges, key = { it.id }) { badge ->
                    val selected = badge.id == selectedBadgeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selected) Color(0xFFEAF3DE) else Color(0xFFF5F5F5),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (selected) 1.5.dp else 0.5.dp,
                                color = if (selected) Color(0xFF238D25) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(11.dp)
                            )
                            .clickable { onSelect(badge) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BadgePill(badge = badge)
                            Text(
                                text = descriptionForBadge(badge.title),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF238D25),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun descriptionForBadge(title: String): String = when (title.uppercase()) {
    "DEV" -> "Disponível para desenvolvedores do Zenith"
    "PREMIUM" -> "Disponível para usuários Premium"
    "TESTER" -> "Disponível para testadores beta"
    else -> "Badge disponível"
}
