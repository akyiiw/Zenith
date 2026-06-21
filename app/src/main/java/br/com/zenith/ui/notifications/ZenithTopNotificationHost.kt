package br.com.zenith.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import br.com.zenith.ui.theme.Inter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ZenithTopNotificationHost(
    modifier: Modifier = Modifier
) {
    var currentNotification by remember { mutableStateOf<ZenithNotification?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ZenithNotifier.notifications.collectLatest { notification ->
            currentNotification = notification
            visible = true
            delay(2600)
            visible = false
            delay(240)
            if (currentNotification == notification) {
                currentNotification = null
            }
        }
    }

    Box(
        modifier = modifier
            .zIndex(20f)
            .statusBarsPadding()
            .padding(top = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible && currentNotification != null,
            enter = slideInVertically(
                animationSpec = tween(220),
                initialOffsetY = { -it / 2 }
            ) + fadeIn(animationSpec = tween(160)) + scaleIn(
                animationSpec = tween(180),
                initialScale = 0.98f
            ),
            exit = slideOutVertically(
                animationSpec = tween(180),
                targetOffsetY = { -it / 3 }
            ) + fadeOut(animationSpec = tween(140)) + scaleOut(
                animationSpec = tween(160),
                targetScale = 0.98f
            )
        ) {
            currentNotification?.let { notification ->
                ZenithTopNotification(notification = notification)
            }
        }
    }
}

@Composable
private fun ZenithTopNotification(notification: ZenithNotification) {
    val colors = notification.type.colors()
    Surface(
        color = colors.background,
        shape = RoundedCornerShape(50),
        shadowElevation = 0.dp,
        modifier = Modifier
            .widthIn(max = 340.dp)
            .border(1.dp, colors.border, RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(colors.iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.type.icon(),
                    contentDescription = null,
                    tint = colors.icon,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = notification.message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
            )
        }
    }
}

private data class NotificationColors(
    val background: Color,
    val border: Color,
    val iconBackground: Color,
    val icon: Color,
    val text: Color
)

private fun ZenithNotificationType.colors(): NotificationColors = when (this) {
    ZenithNotificationType.Success -> NotificationColors(
        background = Color(0xFFF4FFF5),
        border = Color(0xFFB8E7BC),
        iconBackground = Color(0xFF238D25),
        icon = Color.White,
        text = Color(0xFF124D17)
    )
    ZenithNotificationType.Error -> NotificationColors(
        background = Color(0xFFFFF5F5),
        border = Color(0xFFFFC7C7),
        iconBackground = Color(0xFFD64545),
        icon = Color.White,
        text = Color(0xFF7A1F1F)
    )
    ZenithNotificationType.Warning -> NotificationColors(
        background = Color(0xFFFFFBEC),
        border = Color(0xFFF2D78A),
        iconBackground = Color(0xFFE0A500),
        icon = Color.White,
        text = Color(0xFF6B4B00)
    )
    ZenithNotificationType.Info -> NotificationColors(
        background = Color(0xFFF7FAF8),
        border = Color(0xFFD8E2DA),
        iconBackground = Color(0xFF4F6653),
        icon = Color.White,
        text = Color(0xFF223126)
    )
}

private fun ZenithNotificationType.icon(): ImageVector = when (this) {
    ZenithNotificationType.Success -> Icons.Default.CheckCircle
    ZenithNotificationType.Error -> Icons.Default.Error
    ZenithNotificationType.Warning -> Icons.Default.Warning
    ZenithNotificationType.Info -> Icons.Default.Info
}
