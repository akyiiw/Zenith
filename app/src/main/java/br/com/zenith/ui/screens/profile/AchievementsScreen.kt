package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.profile.AchievementCategory
import br.com.zenith.viewmodels.profile.AchievementItem
import br.com.zenith.viewmodels.profile.AchievementsViewModel

@Composable
fun AchievementsScreen(
    navController: NavController,
    userId: String
) {
    ZenithTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        val viewModel: AchievementsViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        LaunchedEffect(userId) {
            viewModel.fetchAchievements(userId, context)
        }

        Column(
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
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFF238D25),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = "Conquistas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    uiState.profile?.name?.takeIf { !uiState.isOwnProfile && it.isNotBlank() }?.let { username ->
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = Inter,
                                color = Color(0xFF6F6C6C)
                            )
                        )
                    }
                }
            }

            if (isLoading) {
                CenteredZenithLoading()
            } else {
                val grouped = uiState.achievements.groupBy { it.definition.category }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = BottomNavListPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.achievements.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhuma conquista desbloqueada ainda",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Inter,
                                    color = Color(0xFF6F6C6C)
                                ),
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                    AchievementCategory.entries.forEach { category ->
                        val items = grouped[category].orEmpty()
                        if (items.isNotEmpty()) {
                            item {
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF151515)
                                    )
                                )
                            }
                            items(items, key = { it.definition.slug }) { item ->
                                AchievementCard(item = item)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementItem) {
    val unlocked = item.unlocked
    val accent = if (unlocked) Color(0xFF238D25) else Color(0xFF9A9A9A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (unlocked) Color(0xFFEAF3DE) else Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, if (unlocked) Color(0xFFB7DEB8) else Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (unlocked) item.definition.icon else Icons.Default.Lock,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.definition.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) Color(0xFF151515) else Color(0xFF6F6C6C)
                )
            )
            Text(
                text = if (unlocked) {
                    item.unlock?.unlockedAt?.let { "Desbloqueada" } ?: "Desbloqueada"
                } else {
                    item.definition.description
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    color = Color(0xFF6F6C6C)
                )
            )
        }
    }
}
