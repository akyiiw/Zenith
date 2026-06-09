package br.com.zenith.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.ui.components.profile.AboutSection
import br.com.zenith.ui.components.profile.ProfileHeader
import br.com.zenith.ui.components.profile.RecentActivitySection
import br.com.zenith.ui.components.profile.RecentHeader
import br.com.zenith.ui.theme.items.ZenithTextField
import br.com.zenith.viewmodels.activity.ActivityViewModel
import br.com.zenith.viewmodels.profile.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel()
    val activityViewModel: ActivityViewModel = viewModel()
    val user by userViewModel.userState.collectAsState()
    val stats by userViewModel.statsState.collectAsState()
    val badge by userViewModel.badgeState.collectAsState(initial = null)
    val atividades by activityViewModel.atividades.collectAsState()
    val desafios by activityViewModel.desafios.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var showStatusDialog by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }

    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route == "profile") {
            userViewModel.fetchUserProfile(context)
            activityViewModel.fetchAtividades(context)
        }
    }
    ZenithTheme {
        Scaffold(
            containerColor = Color.White
        ) { padding ->
            if (isLoading) {
                CenteredZenithLoading(contentPadding = padding)
                return@Scaffold
            }

            var contentVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                contentVisible = true
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = slideInVertically(
                    animationSpec = tween(280),
                    initialOffsetY = { it / 8 }
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = padding),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp)
                ) {
                    item {
                        ProfileHeader(
                            user = user,
                            stats = stats,
                            badge = badge,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate("edit_profile") },
                            onTitleClick = { navController.navigate("title_select") },
                            onStatusClick = {
                                statusText = user?.status.orEmpty()
                                showStatusDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AboutSection(
                            aboutMe = user?.aboutMe,
                            isOwnProfile = true,
                            onEdit = { navController.navigate("edit_profile") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RecentHeader()
                        Spacer(modifier = Modifier.height(8.dp))
                        RecentActivitySection(
                            navController = navController,
                            atividades = atividades,
                            desafios = desafios
                        )
                    }
                }
            }
        }

        if (showStatusDialog) {
            ModalBottomSheet(
                onDismissRequest = { showStatusDialog = false },
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
                    Text("Atualizar status")
                    ZenithTextField(
                        value = statusText,
                        onValueChange = { statusText = it.take(80) },
                        label = "Qual é o seu humor?",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = { showStatusDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                userViewModel.atualizarStatus(statusText, context)
                                showStatusDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238D25))
                        ) {
                            Text("Salvar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
