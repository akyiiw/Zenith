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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.MaterialTheme
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
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.SecondaryGreen
import br.com.zenith.viewmodels.home.HomeNotificationItem
import br.com.zenith.viewmodels.home.HomeNotificationType
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage

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
fun ActivityCard(title: String, activityName: String, progress: String, goal: String) {
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
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    color = Color.Black
                )
                Icon(
                    painter = painterResource(R.drawable.greendot),
                    contentDescription = "Ativo",
                    tint = Color(0xFF15FD00)
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
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = "Activity Icon",
                        tint = Green,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = activityName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Row {
                        Text(
                            text = "$progress / ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Text(
                            text = goal,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun ActivitySection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        HorizontalPager(
            state = pagerState,
            pageSpacing = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (page == 0) {
                ActivityCard(
                    title = "Atividades de Hoje",
                    activityName = "Corrida matinal",
                    progress = "2,5km",
                    goal = "5km"
                )
            } else {
                ActivityCard(
                    title = "Atividades de Hoje",
                    activityName = "Caminhada leve",
                    progress = "1,2km",
                    goal = "3km"
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row (
            modifier = Modifier
                .padding(0.dp)
                .width(23.dp)
                .height(9.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(2) { index ->
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
