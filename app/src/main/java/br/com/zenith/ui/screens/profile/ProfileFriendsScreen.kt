package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.R
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.social.ProfileFriendItem
import br.com.zenith.viewmodels.social.PublicProfileViewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileFriendsScreen(
    navController: NavController,
    userId: String
) {
    ZenithTheme {
        val viewModel: PublicProfileViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        val context = LocalContext.current

        LaunchedEffect(userId) {
            viewModel.fetchProfile(userId, context)
        }

        Scaffold(containerColor = Color.White) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = BottomNavListPadding)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color(0xFF238D25)
                            )
                        }
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = Color(0xFF238D25),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Column {
                            Text(
                                text = "Amigos",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                            Text(
                                text = uiState.profile?.displayName ?: "Perfil",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF667066)
                                )
                            )
                        }
                    }
                }

                if (isLoading) {
                    item { CenteredZenithLoading() }
                } else if (uiState.friends.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum amigo para mostrar.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF667066)
                            ),
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                } else {
                    items(uiState.friends, key = { it.profile.id }) { friend ->
                        ProfileFriendRow(
                            friend = friend,
                            onClick = { navController.navigate("user_profile/${friend.profile.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFriendRow(
    friend: ProfileFriendItem,
    onClick: () -> Unit
) {
    val profile = friend.profile
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F8F8), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE1E7DD), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pictureModel = profile.pictureHash?.let { hash ->
            SupabaseConfig.getClient().storage.from("profiles").publicUrl(hash)
        }
        if (pictureModel != null) {
            AsyncImage(
                model = pictureModel,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.profile_picture),
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W800,
                    color = Color(0xFF111111)
                )
            )
            Text(
                text = "@${profile.name}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF667066)
                )
            )
            Text(
                text = friend.acceptedAt?.let { "Amigos desde ${formatFriendSince(it)}" } ?: "Amigos",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF238D25)
                )
            )
        }
    }
}

private fun formatFriendSince(value: String): String {
    return runCatching {
        val date = OffsetDateTime.parse(value)
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(date)
    }.getOrDefault(value.take(10))
}