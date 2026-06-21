package br.com.zenith.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.components.common.ScreenHeader
import br.com.zenith.ui.components.settings.AccountSettingsGroup
import br.com.zenith.ui.components.settings.SettingsActionGroup
import br.com.zenith.ui.components.settings.SettingsActionItem
import br.com.zenith.ui.components.settings.SettingsSectionTitle
import br.com.zenith.ui.theme.ZenithTheme
import io.github.jan.supabase.auth.auth

@Composable
fun SettingsScreen(navController: NavController) {
    ZenithTheme {
        val context = LocalContext.current
        var email by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            SupabaseConfig.init(context)
            email = SupabaseConfig.getClient().auth.currentUserOrNull()?.email
        }

        Scaffold(containerColor = Color.White) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = BottomNavListPadding)
            ) {
                item {
                    ScreenHeader(title = "Configurações")
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionTitle("Conta")
                        AccountSettingsGroup(
                            email = email,
                            onEmailClick = { navController.navigate("settings_email") },
                            onPasswordClick = { navController.navigate("settings_password") },
                            onSecurityClick = { navController.navigate("settings_security") }
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionTitle("Privacidade")
                        SettingsActionGroup(
                            items = listOf(
                                SettingsActionItem(
                                    icon = Icons.Default.PrivacyTip,
                                    title = "Privacidade do perfil",
                                    subtitle = "Visibilidade das suas atividades e dados",
                                    onClick = { navController.navigate("settings_privacy") }
                                )
                            )
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionTitle("Bloqueio de aplicativos")
                        SettingsActionGroup(
                            items = listOf(
                                SettingsActionItem(
                                    icon = Icons.Default.Block,
                                    title = "Configurar bloqueios",
                                    subtitle = "Apps, horários e janelas de foco",
                                    onClick = { navController.navigate("app_blocks") }
                                )
                            )
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionTitle("Sobre")
                        SettingsActionGroup(
                            items = listOf(
                                SettingsActionItem(
                                    icon = Icons.Default.Info,
                                    title = "Zenith",
                                    subtitle = "Versão, privacidade e informações do app",
                                    onClick = { navController.navigate("settings_about") }
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
