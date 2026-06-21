package br.com.zenith.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
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
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.profile.UserViewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.storage.storage
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private enum class ProfileMediaEditType {
    Picture,
    Banner
}

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
            onSave = { name, displayName, aboutMe, pictureUri, bannerUri, bannerBlurRadius, pictureFocusX, pictureFocusY, pictureZoom, bannerFocusX, bannerFocusY, bannerZoom ->
                userViewModel.atualizarPerfil(
                    name = name,
                    displayName = displayName,
                    aboutMe = aboutMe,
                    pictureUri = pictureUri,
                    bannerUri = bannerUri,
                    bannerBlurRadius = bannerBlurRadius,
                    pictureFocusX = pictureFocusX,
                    pictureFocusY = pictureFocusY,
                    pictureZoom = pictureZoom,
                    bannerFocusX = bannerFocusX,
                    bannerFocusY = bannerFocusY,
                    bannerZoom = bannerZoom,
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
    pictureFocusX: Float,
    pictureFocusY: Float,
    bannerFocusX: Float,
    bannerFocusY: Float,
    bannerBlurRadius: Int,
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
                    alignment = BiasAlignment(bannerFocusX, bannerFocusY),
                    modifier = Modifier.fillMaxSize()
                        .then(if (bannerBlurRadius > 0) Modifier.blur(bannerBlurRadius.dp) else Modifier)
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
                        alignment = BiasAlignment(pictureFocusX, pictureFocusY),
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, Color(0xFF238D25).copy(alpha = 0.62f))
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

@Composable
private fun ProfileMediaAdjustScreen(
    uri: Uri,
    type: ProfileMediaEditType,
    focusX: Float,
    focusY: Float,
    bannerBlurRadius: Int,
    onFocusXChange: (Float) -> Unit,
    onFocusYChange: (Float) -> Unit,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onBannerBlurRadiusChange: (Int) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val isBanner = type == ProfileMediaEditType.Banner
    val title = if (isBanner) "Ajustar banner" else "Ajustar foto"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF238D25)
                    )
                }
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W800,
                            color = Color(0xFF111111)
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isBanner) Modifier.height(150.dp)
                        else Modifier
                            .size(220.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    .clip(if (isBanner) RoundedCornerShape(12.dp) else CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .border(
                        1.dp,
                        Color(0xFFE0E0E0),
                        if (isBanner) RoundedCornerShape(12.dp) else CircleShape
                    )
                    .pointerInput(isBanner) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val panScale = if (isBanner) 180f else 120f
                            onFocusXChange((focusX - pan.x / panScale / zoom).coerceIn(-1f, 1f))
                            onFocusYChange((focusY - pan.y / panScale / zoom).coerceIn(-1f, 1f))
                            onZoomChange((zoom * gestureZoom).coerceIn(1f, 4f))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = if (isBanner) "Prévia do banner" else "Prévia da foto",
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(focusX, focusY),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                        }
                        .then(if (isBanner && bannerBlurRadius > 0) Modifier.blur(bannerBlurRadius.dp) else Modifier)
                )
            }

            if (isBanner) {
                MediaAdjustmentCard(title = "Blur do banner") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BannerBlurButton("0%", 0, bannerBlurRadius, onBannerBlurRadiusChange)
                        BannerBlurButton("50%", 12, bannerBlurRadius, onBannerBlurRadiusChange)
                        BannerBlurButton("100%", 24, bannerBlurRadius, onBannerBlurRadiusChange)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Text("Cancelar", color = Color(0xFF1A1A1A), fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onApply,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
            ) {
                Text("Aplicar", color = Color.White, fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MediaAdjustmentCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Inter,
                fontWeight = FontWeight.W800,
                color = Color(0xFF111111)
            )
        )
        content()
    }
}

@Composable
private fun RowScope.BannerBlurButton(
    label: String,
    value: Int,
    selectedValue: Int,
    onSelected: (Int) -> Unit
) {
    val selected = value == selectedValue
    Button(
        onClick = { onSelected(value) },
        modifier = Modifier
            .weight(1f)
            .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFEAF3DE) else Color.White,
            contentColor = if (selected) Color(0xFF238D25) else Color(0xFF1A1A1A)
        ),
        border = BorderStroke(1.dp, if (selected) Color(0xFF238D25) else Color(0xFFE0E0E0))
    ) {
        Text(label, fontFamily = Inter, fontWeight = FontWeight.Bold)
    }
}

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
    onSave: (String, String, String?, Uri?, Uri?, Int, Float, Float, Float, Float, Float, Float) -> Unit,
    onBack: () -> Unit
) {
    var initializedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var aboutMe by rememberSaveable { mutableStateOf("") }
    var pictureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var bannerUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pictureFocusX by rememberSaveable { mutableFloatStateOf(0f) }
    var pictureFocusY by rememberSaveable { mutableFloatStateOf(0f) }
    var pictureZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var bannerFocusX by rememberSaveable { mutableFloatStateOf(0f) }
    var bannerFocusY by rememberSaveable { mutableFloatStateOf(0f) }
    var bannerZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var bannerBlurRadius by rememberSaveable { mutableIntStateOf(10) }
    var pendingMediaUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingMediaType by rememberSaveable { mutableStateOf<ProfileMediaEditType?>(null) }
    var pendingFocusX by rememberSaveable { mutableFloatStateOf(0f) }
    var pendingFocusY by rememberSaveable { mutableFloatStateOf(0f) }
    var pendingZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var pendingBannerBlurRadius by rememberSaveable { mutableIntStateOf(10) }

    LaunchedEffect(user?.id) {
        val loadedUser = user ?: return@LaunchedEffect
        if (initializedUserId != loadedUser.id) {
            initializedUserId = loadedUser.id
            name = loadedUser.name
            displayName = loadedUser.displayName
            aboutMe = loadedUser.aboutMe.orEmpty()
            pictureUri = null
            bannerUri = null
            pictureFocusX = 0f
            pictureFocusY = 0f
            pictureZoom = 1f
            bannerFocusX = 0f
            bannerFocusY = 0f
            bannerZoom = 1f
            bannerBlurRadius = loadedUser.bannerBlurRadius
        }
    }

    val pictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingMediaUri = uri
            pendingMediaType = ProfileMediaEditType.Picture
            pendingFocusX = pictureFocusX
            pendingFocusY = pictureFocusY
            pendingZoom = pictureZoom
            pendingBannerBlurRadius = bannerBlurRadius
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingMediaUri = uri
            pendingMediaType = ProfileMediaEditType.Banner
            pendingFocusX = bannerFocusX
            pendingFocusY = bannerFocusY
            pendingZoom = bannerZoom
            pendingBannerBlurRadius = bannerBlurRadius
        }
    }

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
        val mediaUri = pendingMediaUri
        val mediaType = pendingMediaType
        if (mediaUri != null && mediaType != null) {
            ProfileMediaAdjustScreen(
                uri = mediaUri,
                type = mediaType,
                focusX = pendingFocusX,
                focusY = pendingFocusY,
                zoom = pendingZoom,
                bannerBlurRadius = pendingBannerBlurRadius,
                onFocusXChange = { pendingFocusX = it },
                onFocusYChange = { pendingFocusY = it },
                onZoomChange = { pendingZoom = it },
                onBannerBlurRadiusChange = { pendingBannerBlurRadius = it },
                onCancel = {
                    pendingMediaUri = null
                    pendingMediaType = null
                },
                onApply = {
                    when (mediaType) {
                        ProfileMediaEditType.Picture -> {
                            pictureUri = mediaUri
                            pictureFocusX = pendingFocusX
                            pictureFocusY = pendingFocusY
                            pictureZoom = pendingZoom
                        }
                        ProfileMediaEditType.Banner -> {
                            bannerUri = mediaUri
                            bannerFocusX = pendingFocusX
                            bannerFocusY = pendingFocusY
                            bannerZoom = pendingZoom
                            bannerBlurRadius = pendingBannerBlurRadius
                        }
                    }
                    pendingMediaUri = null
                    pendingMediaType = null
                }
            )
            return@Box
        }

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
                contentPadding = PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = BottomNavListPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileMediaPicker(
                        user = user,
                        pictureUri = pictureUri,
                        bannerUri = bannerUri,
                        pictureFocusX = pictureFocusX,
                        pictureFocusY = pictureFocusY,
                        bannerFocusX = bannerFocusX,
                        bannerFocusY = bannerFocusY,
                        bannerBlurRadius = bannerBlurRadius,
                        onPickPicture = { pictureLauncher.launch("image/*") },
                        onPickBanner = { bannerLauncher.launch("image/*") }
                    )
                }
                item {
                    ZenithTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nome de usuário",
                        prefix = "@",
                        enabled = usernameCooldown.canEdit
                    )
                    if (!usernameCooldown.canEdit) {
                        Text(
                            text = "Você poderá alterar o nome de usuário novamente em ${usernameCooldown.nextEditLabel}.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = Inter,
                                color = Color.Gray
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                item {
                    ZenithTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Nome de exibição"
                    )
                }
                item {
                    ZenithTextField(
                        value = aboutMe,
                        onValueChange = { aboutMe = it },
                        label = "Sobre mim",
                        minLines = 3,
                        maxLines = 5
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Botão salvar
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
                        onSave(
                            name,
                            displayName,
                            aboutMe.ifBlank { null },
                            pictureUri,
                            bannerUri,
                            bannerBlurRadius,
                            pictureFocusX,
                            pictureFocusY,
                            pictureZoom,
                            bannerFocusX,
                            bannerFocusY,
                            bannerZoom
                        )
                    },
                    enabled = name.isNotBlank() && displayName.isNotBlank() && usernameEnabled,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                ) {
                    Text(
                        "Salvar alterações",
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

