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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
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
import br.com.zenith.ui.theme.SecondaryGreen
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage

@Composable
fun Header(pictureHash: String?, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notificações",
            tint = Color.Black,
            modifier = Modifier
                .size(41.dp)
        )
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
