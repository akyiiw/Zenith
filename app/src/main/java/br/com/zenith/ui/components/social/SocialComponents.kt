package br.com.zenith.ui.components.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.zenith.R
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.data.models.Amizade
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.theme.Inter
import br.com.zenith.viewmodels.social.SocialUiState
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage

@Composable
fun SocialSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        ),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun SocialEmptyState(text: String) {
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

@Composable
fun SocialUserRow(
    profile: Profile,
    friendship: Amizade?,
    currentUserId: String,
    isSaving: Boolean,
    onProfileClick: () -> Unit,
    onSendInvite: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 74.dp)
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(8.dp))
            .clickable { onProfileClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialAvatar(profile.pictureHash)
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${profile.name}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF6F6C6C)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        SocialFriendshipAction(
            friendship = friendship,
            currentUserId = currentUserId,
            isSaving = isSaving,
            onSendInvite = onSendInvite,
            onAccept = onAccept,
            onReject = onReject,
            onRemove = onRemove
        )
    }
}

@Composable
private fun SocialAvatar(pictureHash: String?) {
    if (pictureHash != null) {
        val url = SupabaseConfig.getClient().storage.from("profiles").publicUrl(pictureHash)
        AsyncImage(
            model = url,
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF2B792C), CircleShape)
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.profile_picture),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF2B792C), CircleShape)
        )
    }
}

@Composable
private fun SocialFriendshipAction(
    friendship: Amizade?,
    currentUserId: String,
    isSaving: Boolean,
    onSendInvite: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    when {
        friendship == null -> {
            Button(
                onClick = onSendInvite,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        friendship.status == SocialUiState.STATUS_PENDENTE && friendship.friendId == currentUserId -> {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onAccept, enabled = !isSaving) {
                    Icon(Icons.Default.Check, contentDescription = "Aceitar", tint = Color(0xFF238D25))
                }
                IconButton(onClick = onReject, enabled = !isSaving) {
                    Icon(Icons.Default.Close, contentDescription = "Recusar", tint = Color(0xFFD32F2F))
                }
            }
        }

        friendship.status == SocialUiState.STATUS_PENDENTE -> {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("Pendente", fontFamily = Inter) }
            )
        }

        else -> {
            OutlinedButton(
                onClick = onRemove,
                enabled = !isSaving,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Amigos", fontFamily = Inter, color = Color(0xFF238D25))
            }
        }
    }
}
