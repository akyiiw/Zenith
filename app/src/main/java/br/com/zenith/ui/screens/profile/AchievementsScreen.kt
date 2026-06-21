package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        val viewModel: AchievementsViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        val context = LocalContext.current

        LaunchedEffect(userId) {
            viewModel.fetchAchievements(userId, context)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
        ) {
            // Cabeçalho
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
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = "Conquistas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W800,
                            color = Color(0xFF111111)
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
                val unlockedCount = uiState.achievements.count { it.unlocked }
                val totalCount = uiState.achievements.size
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = BottomNavListPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.achievements.isEmpty()) {
                        item {
                            EmptyAchievementsCard()
                        }
                    } else {
                        item {
                            AchievementSummaryCard(
                                unlockedCount = unlockedCount,
                                totalCount = totalCount
                            )
                        }
                    }
                    AchievementCategory.entries.forEach { category ->
                        val items = grouped[category].orEmpty()
                        if (items.isNotEmpty()) {
                            item {
                                AchievementCategoryBlock(category = category, items = items)
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
private fun AchievementSummaryCard(unlockedCount: Int, totalCount: Int) {
    val progress = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Progresso",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.W800,
                        color = Color(0xFF111111)
                    )
                )
                Text(
                    text = "$unlockedCount de $totalCount desbloqueadas",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF667066)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF3DE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF238D25))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE4E4E4), RoundedCornerShape(99.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(Color(0xFF238D25), RoundedCornerShape(99.dp))
            )
        }
    }
}

@Composable
private fun EmptyAchievementsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Nenhuma conquista desbloqueada ainda",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                color = Color(0xFF111111)
            )
        )
        Text(
            text = "Continue registrando atividades para liberar novas marcas.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                color = Color(0xFF6F6C6C)
            )
        )
    }
}

@Composable
private fun AchievementCategoryBlock(
    category: AchievementCategory,
    items: List<AchievementItem>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    color = Color(0xFF151515)
                )
            )
            Text(
                text = "${items.count { it.unlocked }}/${items.size}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF667066)
                )
            )
        }
        items.forEach { item ->
            AchievementCard(item = item)
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementItem) {
    val unlocked = item.unlocked
    val accent = if (unlocked) Color(0xFF238D25) else Color(0xFF9A9A9A)
    val background = if (unlocked) Color(0xFFF5F5F5) else Color(0xFFFAFAFA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .border(1.dp, if (unlocked) Color(0xFFD9DED8) else Color(0xFFE5E5E5), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (unlocked) Color(0xFFEAF3DE) else Color(0xFFEDEDED)),
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
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (unlocked) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "OK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    color = Color(0xFF238D25)
                ),
                modifier = Modifier
                    .background(Color(0xFFEAF3DE), RoundedCornerShape(99.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
