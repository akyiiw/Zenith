package br.com.zenith.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.zenith.R
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Atividade
import br.com.zenith.data.models.ProgressGoal
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.components.profile.formattedActivityValue
import br.com.zenith.ui.components.profile.formattedDuration
import br.com.zenith.ui.components.profile.tempoRelativo
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.SecondaryGreen
import br.com.zenith.viewmodels.home.HomeFeedItem
import br.com.zenith.viewmodels.home.HomeNotificationItem
import br.com.zenith.viewmodels.home.HomeNotificationType
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage
import java.util.Locale

data class HomeGoalItem(
    val periodLabel: String,
    val goal: ProgressGoal
)

@Composable
fun Header(
    pictureHash: String?,
    notificationCount: Int = 0,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(41.dp)
                .clickable { onNotificationsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificações",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notificationCount.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
        Image(
            painter = painterResource(id = R.drawable.zenith),
            contentDescription = "Logo",
            modifier = Modifier
                .height(34.dp)
                .width(44.dp),
            contentScale = ContentScale.Crop
        )
        if (pictureHash != null) {
            val url = SupabaseConfig.getClient().storage.from("profiles").publicUrl(pictureHash)
            AsyncImage(
                model = url,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF2B792C), CircleShape)
                    .clickable { onProfileClick() }
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.profile_picture),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF2B792C), CircleShape)
                    .clickable { onProfileClick() }
            )
        }
    }
}

@Composable
fun NotificationsDrawer(
    notifications: List<HomeNotificationItem>,
    isLoading: Boolean,
    onNotificationClick: (HomeNotificationItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(318.dp),
        color = Color.White,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, top = 28.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Notificações",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Text(
                text = "Atualizações do Zenith",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    color = Color(0xFF667067)
                )
            )

            if (isLoading) {
                NotificationSkeleton()
            } else if (notifications.isEmpty()) {
                EmptyNotifications()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    notifications.forEach { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = { onNotificationClick(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF1F4F1))
            )
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xFFF0F5F1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Nada novo por aqui",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        )
    }
}

@Composable
private fun NotificationRow(
    notification: HomeNotificationItem,
    onClick: () -> Unit
) {
    val color = notification.type.color()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAF8))
            .border(1.dp, Color(0xFFE3EAE3), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notification.type.icon(),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    color = Color(0xFF536057)
                )
            )
            notification.actionLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                )
            }
        }
    }
}

private fun HomeNotificationType.color(): Color = when (this) {
    HomeNotificationType.FriendRequest -> Color(0xFF238D25)
    HomeNotificationType.Mention -> Color(0xFF5A6EE8)
    HomeNotificationType.Ranking -> Color(0xFFE0A500)
    HomeNotificationType.Achievement -> Color(0xFF8B5CF6)
}

private fun HomeNotificationType.icon() = when (this) {
    HomeNotificationType.FriendRequest -> Icons.Default.Groups
    HomeNotificationType.Mention -> Icons.Default.Tag
    HomeNotificationType.Ranking -> Icons.Default.MilitaryTech
    HomeNotificationType.Achievement -> Icons.Default.EmojiEvents
}

@Composable
fun WelcomeCard(user: Profile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color = Color(0x2981D981))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Boa noite, ${user?.displayName ?: "Usuário"}",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W600,
                color = Color.Black
            )
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.firestreak),
                contentDescription = "Streak",
                tint = Color(0xFFF26500),
            )
            Text(
                text = user?.streak.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF26500)
            )
        }
    }
}

@Composable
fun GoalCard(item: HomeGoalItem, onManageGoals: () -> Unit) {
    val goal = item.goal
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Green, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.periodLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    color = Color.Black
                )
                Text(
                    text = "Gerenciar",
                    modifier = Modifier.clickable { onManageGoals() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Green
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, SecondaryGreen, CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Meta",
                        tint = Green,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Row {
                        Text(
                            text = "${formatGoalValue(goal.currentValue)} / ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Text(
                            text = "${formatGoalValue(goal.targetValue)} ${goal.unit}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFFE9F8E9), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(goal.progressPercent)
                                .fillMaxHeight()
                                .background(if (goal.isComplete) Color(0xFF15A000) else Green, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGoalsCard(onManageGoals: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Green, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .clickable { onManageGoals() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Crie sua primeira meta",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Acompanhe objetivos semanais e mensais na Home.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ActivitySection(
    goals: List<HomeGoalItem>,
    isLoading: Boolean,
    onManageGoals: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .border(1.dp, Green, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Green, strokeWidth = 3.dp)
            }
            return@Column
        }

        if (goals.isEmpty()) {
            EmptyGoalsCard(onManageGoals = onManageGoals)
            return@Column
        }

        val pagerState = rememberPagerState(pageCount = { goals.size })
        HorizontalPager(
            state = pagerState,
            pageSpacing = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            GoalCard(
                item = goals[page],
                onManageGoals = onManageGoals
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .padding(0.dp)
                .width((goals.size * 14).dp)
                .height(9.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(goals.size) { index ->
                Box(
                    modifier = Modifier
                        .width(9.dp)
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) Color.Black else Color.LightGray)
                )
            }
        }
    }
}

private fun formatGoalValue(value: Double): String {
    val locale = Locale.forLanguageTag("pt-BR")
    return if (value >= 10) {
        "%,.0f".format(locale, value)
    } else {
        "%.1f".format(locale, value)
    }
}

@Composable
fun FeedSection(
    items: List<HomeFeedItem>,
    isLoading: Boolean,
    onPublishClick: () -> Unit,
    onOpenProfile: (Profile) -> Unit,
    onOpenActivity: (Atividade) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Feed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Button(
                onClick = onPublishClick,
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Publicar", color = Color.White, fontFamily = Inter)
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Green, strokeWidth = 3.dp)
                }
            }

            items.isEmpty() -> {
                Text(
                    text = "Nenhum amigo publicou atividades ainda.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            }

            else -> {
                items.forEach { item ->
                    FeedActivityCard(
                        item = item,
                        onOpenProfile = { onOpenProfile(item.author) },
                        onOpenActivity = { onOpenActivity(item.activity) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedActivityCard(
    item: HomeFeedItem,
    onOpenProfile: () -> Unit,
    onOpenActivity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
            .clickable { onOpenActivity() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FeedAvatar(item.author.pictureHash, Modifier.clickable { onOpenProfile() })
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.author.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    text = tempoRelativo(item.activity.realizadaEm ?: item.activity.criadaEm),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            }
        }

        Text(
            text = item.activity.titulo ?: item.activity.exercicio?.nome ?: "Atividade registrada",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        )
        Text(
            text = formattedActivityValue(item.activity),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color = Green
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Duracao: ${formattedDuration(item.activity.duracaoMin)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Inter, color = Color(0xFF6F6C6C))
            )
            Text(
                text = if (item.activity.verificada) "Verificada" else "Manual",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Inter, color = Color(0xFF6F6C6C))
            )
        }
    }
}

@Composable
private fun FeedAvatar(pictureHash: String?, modifier: Modifier = Modifier) {
    val baseModifier = modifier
        .size(42.dp)
        .clip(CircleShape)
        .border(2.dp, Color(0xFF2B792C), CircleShape)

    if (pictureHash != null) {
        val url = SupabaseConfig.getClient().storage.from("profiles").publicUrl(pictureHash)
        AsyncImage(
            model = url,
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = baseModifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.profile_picture),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = baseModifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishActivitySheet(
    activities: List<Atividade>,
    isPublishing: Boolean,
    onDismiss: () -> Unit,
    onPublish: (Atividade) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Publicar atividade",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                )
            )

            if (activities.isEmpty()) {
                Text(
                    text = "Nenhuma atividade disponivel para publicar.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        color = Color(0xFF6F6C6C)
                    )
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activities, key = { it.id }) { activity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
                                .clickable(enabled = !isPublishing) { onPublish(activity) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activity.titulo ?: activity.exercicio?.nome ?: "Atividade",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formattedActivityValue(activity),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Inter,
                                        color = Green
                                    )
                                )
                            }
                            Text(
                                text = "Publicar",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                color = Green
                            )
                        }
                    }
                }
            }
        }
    }
}
