package br.com.zenith.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.components.common.ScreenHeader
import br.com.zenith.ui.components.settings.AccountSettingsGroup
import br.com.zenith.ui.components.settings.LogoutButton
import br.com.zenith.ui.components.settings.SettingsSectionTitle
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.app.AuthViewModel
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    ZenithTheme {
        val authViewModel: AuthViewModel = viewModel()
        val context = LocalContext.current
        val isLoading by authViewModel.isLoading.collectAsState()
        var showLogoutDialog by remember { mutableStateOf(false) }
        var email by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            SupabaseConfig.init(context)
            email = SupabaseConfig.getClient().auth.currentUserOrNull()?.email
        }

        Scaffold(
            containerColor = Color.White
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 19.dp, bottom = 104.dp)
            ) {
                item {
                    ScreenHeader(
                        title = "Configurações",
                        icon = Icons.Default.Settings
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionTitle("Conta")
                        AccountSettingsGroup(
                            email = email,
                            onEmailClick = {},
                            onPasswordClick = {},
                            onProfileClick = { navController.navigate("edit_profile") },
                            onSecurityClick = {}
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionTitle("Sessão")
                        LogoutButton(
                            isLoading = isLoading,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }
            }
        }

        if (showLogoutDialog) {
            ModalBottomSheet(
                onDismissRequest = { showLogoutDialog = false },
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
                    Text("Sair da conta?")
                    Text("Você precisará entrar novamente para acessar seus dados.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                authViewModel.sairDaConta(context) {
                                    navController.navigate("welcome") {
                                        popUpTo("home") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Sair")
                        }
                    }
                }
            }
        }
    }
}
