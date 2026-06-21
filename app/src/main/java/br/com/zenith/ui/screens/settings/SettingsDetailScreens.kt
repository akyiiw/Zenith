package br.com.zenith.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.components.common.zenithSwitchColors
import br.com.zenith.ui.components.settings.LogoutButton
import br.com.zenith.ui.notifications.ZenithNotifier
import br.com.zenith.ui.theme.Black
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.app.AuthViewModel
import br.com.zenith.viewmodels.profile.UserViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun EmailSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentEmail by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SupabaseConfig.init(context)
        currentEmail = SupabaseConfig.getClient().auth.currentUserOrNull()?.email.orEmpty()
        email = currentEmail
    }

    SettingsDetailScaffold(
        navController = navController,
        title = "E-mail",
        subtitle = "Gerencie o e-mail usado para entrar na conta"
    ) {
        SettingsCard {
            SettingsInfoRow(
                icon = Icons.Default.Mail,
                title = "E-mail atual",
                value = currentEmail.ifBlank { "Não encontrado" }
            )
            ZenithTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = "Novo e-mail",
                singleLine = true
            )
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            SupabaseConfig.getClient().auth.updateUser {
                                this.email = email
                            }
                            currentEmail = email
                            ZenithNotifier.success("Verifique seu novo e-mail para confirmar a alteração.")
                        } catch (e: Exception) {
                            ZenithNotifier.error("Erro ao alterar e-mail: ${e.localizedMessage}")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && email.contains("@") && email != currentEmail,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Salvar e-mail", fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PasswordSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val canSave = newPassword.length >= 6 && newPassword == confirmPassword && !isSaving

    LaunchedEffect(Unit) {
        SupabaseConfig.init(context)
    }

    SettingsDetailScaffold(
        navController = navController,
        title = "Senha",
        subtitle = "Atualize a senha usada para acessar sua conta"
    ) {
        SettingsCard {
            ZenithTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Senha atual",
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            ZenithTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "Nova senha",
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            ZenithTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmar nova senha",
                singleLine = true,
                isError = confirmPassword.isNotBlank() && confirmPassword != newPassword,
                supportingText = {
                    if (confirmPassword.isNotBlank() && confirmPassword != newPassword) {
                        Text("As senhas não coincidem", color = MaterialTheme.colorScheme.error)
                    }
                },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            SupabaseConfig.getClient().auth.updateUser {
                                password = newPassword
                                currentPassword.takeIf { it.isNotBlank() }?.let {
                                    this.currentPassword = it
                                }
                            }
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            ZenithNotifier.success("Senha alterada.")
                        } catch (e: Exception) {
                            ZenithNotifier.error("Erro ao alterar senha: ${e.localizedMessage}")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Salvar senha", fontFamily = Inter, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SecuritySettingsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val isLoading by authViewModel.isLoading.collectAsState()
    var email by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        SupabaseConfig.init(context)
        SupabaseConfig.getClient().auth.currentUserOrNull()?.let {
            email = it.email.orEmpty()
            userId = it.id
        }
    }

    SettingsDetailScaffold(
        navController = navController,
        title = "Segurança",
        subtitle = "Sessão, identificador e acesso da conta"
    ) {
        SettingsCard {
            SettingsInfoRow(Icons.Default.Security, "Sessão", "Conectado")
            SettingsInfoRow(Icons.Default.Mail, "E-mail", email.ifBlank { "Não encontrado" })
            SettingsInfoRow(Icons.Default.Key, "ID da conta", userId.ifBlank { "Não encontrado" })
        }
        LogoutButton(
            isLoading = isLoading,
            onClick = {
                authViewModel.sairDaConta(context) {
                    navController.navigate("welcome") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}

@Composable
fun PrivacySettingsScreen(navController: NavController) {
    val userViewModel: UserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val user by userViewModel.userState.collectAsState()
    val isSaving by userViewModel.isSaving.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.fetchUserProfile(context)
    }

    SettingsDetailScaffold(
        navController = navController,
        title = "Privacidade",
        subtitle = "Controle quem pode ver suas atividades"
    ) {
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Perfil privado",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Inter,
                            fontWeight = FontWeight.W800,
                            color = Black
                        )
                    )
                    Text(
                        text = "Apenas amigos veem atividades publicadas no seu perfil.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Inter,
                            color = Color(0xFF667066)
                        )
                    )
                }
                Switch(
                    checked = user?.profileVisibility == "privado",
                    onCheckedChange = { userViewModel.atualizarVisibilidadePerfil(it, context) },
                    enabled = !isSaving,
                    colors = zenithSwitchColors()
                )
            }
        }
        SettingsCard {
            SettingsInfoRow(Icons.Default.PrivacyTip, "Desafios e ranking", "Respeitam as regras do próprio desafio")
            SettingsInfoRow(Icons.Default.Lock, "Dados sensíveis", "Sono e bloqueios ficam restritos ao seu dispositivo/conta")
        }
    }
}

@Composable
fun AboutSettingsScreen(navController: NavController) {
    SettingsDetailScaffold(
        navController = navController,
        title = "Sobre",
        subtitle = "Informações do aplicativo"
    ) {
        SettingsCard {
            SettingsInfoRow(Icons.Default.Info, "Zenith", "Versão 1.0")
            SettingsInfoRow(Icons.Default.CheckCircle, "Build", "Release inicial")
            SettingsInfoRow(Icons.Default.PrivacyTip, "Privacidade", "Seus dados de saúde são usados para alimentar sua experiência no app")
        }
    }
}

@Composable
private fun SettingsDetailScaffold(
    navController: NavController,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ZenithTheme {
        Scaffold(containerColor = Color.White) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Green
                            )
                        }
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.W800,
                                    color = Black
                                )
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF667066)
                                )
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEAF3DE), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Green, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Inter,
                    color = Color(0xFF667066)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
