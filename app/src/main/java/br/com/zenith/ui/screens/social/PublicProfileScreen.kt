package br.com.zenith.ui.screens.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.app.CustomBottomNavigationBar
import br.com.zenith.ui.components.profile.ProfileHeader
import br.com.zenith.ui.components.profile.RecentActivitySection
import br.com.zenith.ui.components.profile.RecentHeader
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.social.PublicProfileViewModel

@Composable
fun PublicProfileScreen(
    navController: NavController,
    userId: String
) {
    ZenithTheme {
        val context = LocalContext.current
        val viewModel: PublicProfileViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        LaunchedEffect(userId) {
            viewModel.fetchProfile(userId, context)
        }

        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(3.dp))
                    CustomBottomNavigationBar(navController = navController)
                }
            }
        ) { padding ->
            if (isLoading) {
                CenteredZenithLoading(
                    modifier = Modifier.padding(padding)
                )
                return@Scaffold
            }

            var contentVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                contentVisible = true
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(220)) +
                    slideInVertically(
                        animationSpec = tween(260),
                        initialOffsetY = { it / 12 }
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 24.dp)
                ) {
                    item {
                        ProfileHeader(
                            user = uiState.profile,
                            stats = uiState.stats,
                            badge = uiState.badge,
                            onBack = { navController.popBackStack() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RecentHeader()
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.atividades.isEmpty()) {
                            Text(
                                text = "Nenhuma atividade recente",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF6F6C6C)
                                ),
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                            )
                        } else {
                            RecentActivitySection(
                                navController = navController,
                                atividades = uiState.atividades
                            )
                        }
                    }
                }
            }
        }
    }
}
