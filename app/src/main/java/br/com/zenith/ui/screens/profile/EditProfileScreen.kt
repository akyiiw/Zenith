package br.com.zenith.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import br.com.zenith.data.models.Profile
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.profile.UserViewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditProfileScreen(navController: NavController) {
    ZenithTheme {
        val userViewModel: UserViewModel = viewModel()
        val context = LocalContext.current
        val user by userViewModel.userState.collectAsState()
        val isLoading by userViewModel.isLoading.collectAsState()
        val isSaving by userViewModel.isSaving.collectAsState()

        LaunchedEffect(Unit) { userViewModel.fetchUserProfile(context) }

        EditProfileContent(
            user = user,
            isLoading = isLoading,
            isSaving = isSaving,
            onSave = { name, displayName, aboutMe, pictureUri, bannerUri ->
                userViewModel.atualizarPerfil(
                    name = name,
                    displayName = displayName,
                    aboutMe = aboutMe,
                    pictureUri = pictureUri,
                    bannerUri = bannerUri,
                    context = context
                ) { navController.popBackStack() }
            },
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun ProfileMediaPicker(
    user: Profile?,
    pictureUri: Uri?,
    bannerUri: Uri?,
    onPickPicture: () -> Unit,
    onPickBanner: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0))
                .clickable { onPickBanner() }
        ) {
            val bannerModel = bannerUri ?: user?.bannerHash?.let { hash ->
                SupabaseConfig.getClient().storage.from("profiles").publicUrl(hash)
            }

            if (bannerModel != null) {
                AsyncImage(
                    model = bannerModel,
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            MediaActionPill(
                text = "Trocar banner",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onPickPicture() },
                contentAlignment = Alignment.Center
            ) {
                val pictureModel = pictureUri ?: user?.pictureHash?.let { hash ->
                    SupabaseConfig.getClient().storage.from("profiles").publicUrl(hash)
                }

                if (pictureModel != null) {
                    AsyncImage(
                        model = pictureModel,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.profile_picture),
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Foto de perfil",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold
                    )
                )
                OutlinedButton(
                    onClick = onPickPicture,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color(0xFF238D25))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trocar foto", fontFamily = Inter, color = Color(0xFF238D25))
                }
            }
        }
    }
}

@Composable
private fun MediaActionPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color(0xFF238D25))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W600,
                    color = Color(0xFF238D25)
                )
            )
        }
    }
}

private data class UsernameCooldownInfo(
    val canEdit: Boolean,
    val nextEditLabel: String
)

private fun usernameCooldownInfo(nameUpdatedAt: String?): UsernameCooldownInfo {
    val lastUpdate = nameUpdatedAt
        ?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        ?: return UsernameCooldownInfo(canEdit = true, nextEditLabel = "")

    val nextEdit = lastUpdate.plusDays(UserViewModel.USERNAME_COOLDOWN_DAYS)
    val now = OffsetDateTime.now()

    if (!now.isBefore(nextEdit)) {
        return UsernameCooldownInfo(canEdit = true, nextEditLabel = "")
    }

    val remaining = Duration.between(now, nextEdit)
    val days = remaining.toDays().coerceAtLeast(0)
    val hours = remaining.minusDays(days).toHours().coerceAtLeast(0)
    val label = when {
        days > 0 -> "$days dia${if (days > 1) "s" else ""}"
        hours > 0 -> "$hours hora${if (hours > 1) "s" else ""}"
        else -> nextEdit.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }

    return UsernameCooldownInfo(canEdit = false, nextEditLabel = label)
}

@Composable
fun EditProfileContent(
    user: Profile?,
    isLoading: Boolean,
    isSaving: Boolean,
    onSave: (String, String, String?, Uri?, Uri?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember(user) { mutableStateOf(user?.name ?: "") }
    var displayName by remember(user) { mutableStateOf(user?.displayName ?: "") }
    var aboutMe by remember(user) { mutableStateOf(user?.aboutMe ?: "") }
    var pictureUri by remember(user) { mutableStateOf<Uri?>(null) }
    var bannerUri by remember(user) { mutableStateOf<Uri?>(null) }

    val pictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> pictureUri = uri }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> bannerUri = uri }

    val usernameCooldown = remember(user?.nameUpdatedAt) {
        usernameCooldownInfo(user?.nameUpdatedAt)
    }
    val usernameChanged = name != (user?.name ?: "")
    val usernameEnabled = usernameCooldown.canEdit || !usernameChanged

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color(0xFF238D25)
                )
            }
            Text(
                text = "Editar perfil",
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
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileMediaPicker(
                        user = user,
                        pictureUri = pictureUri,
                        bannerUri = bannerUri,
                        onPickPicture = { pictureLauncher.launch("image/*") },
                        onPickBanner = { bannerLauncher.launch("image/*") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp),
                        singleLine = true,
                        prefix = { Text("@") },
                        enabled = usernameCooldown.canEdit
                    )
                    if (!usernameCooldown.canEdit) {
                        Text(
                            text = "VocÃª poderÃ¡ alterar o nome de usuÃ¡rio novamente em ${usernameCooldown.nextEditLabel}.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = Inter,
                                color = Color.Gray
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Nome de exibiÃ§Ã£o") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp),
                        singleLine = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = aboutMe,
                        onValueChange = { aboutMe = it },
                        label = { Text("Sobre mim (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp),
                        minLines = 3,
                        maxLines = 5
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // BotÃ£o salvar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (isSaving) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ZenithLoading(modifier = Modifier.size(52.dp), strokeWidth = 18f)
                }
            } else {
                Button(
                    onClick = {
                        if (name.isBlank() || displayName.isBlank()) return@Button
                        onSave(name, displayName, aboutMe.ifBlank { null }, pictureUri, bannerUri)
                    },
                    enabled = name.isNotBlank() && displayName.isNotBlank() && usernameEnabled,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Text(
                        "Salvar alteraÃ§Ãµes",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W600
                        )
                    )
                }
            }
        }
    }
}

