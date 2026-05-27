package br.com.zenith.ui.screens.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.app.AuthViewModel
import br.com.zenith.R
import java.io.File

@Composable
fun OnboardingScreen(navController: NavController) {
    ZenithTheme {
        val authViewModel: AuthViewModel = viewModel()
        val context = LocalContext.current
        val isLoading by authViewModel.isLoading.collectAsState()

        OnboardingContent(
            isLoading = isLoading,
            onFinish = { name, displayName, photoUri ->
                authViewModel.salvarPerfil(
                    name = name,
                    displayName = displayName,
                    photoUri = photoUri,
                    context = context
                ) {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }
        )
    }
}

@Composable
fun OnboardingContent(
    isLoading: Boolean = false,
    onFinish: (name: String, displayName: String, photoUri: Uri?) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            slideInHorizontally { it } togetherWith
                    slideOutHorizontally { -it }
        },
        label = "onboarding_step"
    ) { currentStep ->
        when (currentStep) {
            0 -> StepUsername(
                username = username,
                onUsernameChange = { username = it },
                onNext = { step++ }
            )
            1 -> StepDisplayName(
                displayName = displayName,
                onDisplayNameChange = { displayName = it },
                onNext = { step++ }
            )
            2 -> StepPhoto(
                photoUri = photoUri,
                onPhotoSelected = { photoUri = it },
                onNext = { step++ }
            )
            3 -> StepPremium(
                onFinish = { onFinish(username, displayName, photoUri) }
            )
        }
    }
}

@Composable
fun StepUsername(
    username: String,
    onUsernameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingScaffold(
        step = 1,
        title = "Escolha seu\nusuário",
        subtitle = "Esse será seu identificador único no Zenith.",
        onNext = onNext,
        nextEnabled = username.isNotBlank()
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { onUsernameChange(it.lowercase().replace(" ", "")) },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(11.dp),
            prefix = { Text("@") }
        )
    }
}

@Composable
fun StepDisplayName(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OnboardingScaffold(
        step = 2,
        title = "Como quer\nser chamado?",
        subtitle = "Esse é o nome que os outros verão no seu perfil.",
        onNext = onNext,
        nextEnabled = displayName.isNotBlank()
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text("Nome de exibição") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            singleLine = true
        )
    }
}

@Composable
fun StepPhoto(
    photoUri: Uri?,
    onPhotoSelected: (Uri) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Copia pro cache do app — nunca expira
            try {
                val cacheFile = File(context.cacheDir, "avatar_temp.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                onPhotoSelected(Uri.fromFile(cacheFile))
            } catch (e: Exception) {
                // Se falhar a cópia, usa o URI original
                onPhotoSelected(it)
            }
        }
    }

    OnboardingScaffold(
        step = 3,
        title = "Adicione uma\nfoto de perfil",
        subtitle = "Uma foto ajuda as pessoas a te reconhecerem.",
        onNext = onNext,
        nextEnabled = true,
        nextLabel = if (photoUri == null) "Pular" else "Continuar"
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0))
                .border(2.dp, Color(0xFF238D25), CircleShape)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF238D25),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Toque para\nadicionar",
                        color = Color(0xFF238D25),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StepPremium(onFinish: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.badge_premium),
                contentDescription = "zenithpremium"
            )
            Text(
                text = "Zenith Premium",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF238D25)
                )
            )
            Text(
                text = "Desbloqueie recursos exclusivos, sem anúncios e com muito mais poder nas suas mãos.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            listOf(
                "✦  Maior customização do perfil",
                "✦  Sem anúncios",
                "✦  Recursos exclusivos",
                "✦  Muito mais em breve..."
            ).forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF094000)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
        ) {
            Text(
                text = "Ok!",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W600,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Composable
fun OnboardingScaffold(
    step: Int,
    title: String,
    subtitle: String,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String = "Continuar",
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (i == step - 1) 24.dp else 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i < step) Color(0xFF238D25) else Color(0xFFDDDDDD)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 40.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Inter,
                    color = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }

        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
        ) {
            Text(
                text = nextLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.W600,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview() {
    ZenithTheme {
        OnboardingContent(onFinish = { _, _, _ -> })
    }
}
