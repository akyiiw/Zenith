package br.com.zenith.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.R
import br.com.zenith.ui.animations.CenteredZenithLoading
import br.com.zenith.ui.components.common.BottomNavListPadding
import br.com.zenith.ui.theme.Inter
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.social.PublicProfileViewModel

@Composable
fun StreakScreen(
    navController: NavController,
    userId: String
) {
    ZenithTheme {
        val context = androidx.compose.ui.platform.LocalContext.current
        val viewModel: PublicProfileViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        LaunchedEffect(userId) {
            viewModel.fetchProfile(userId, context)
        }

        Scaffold(containerColor = Color.White) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
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
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF238D25),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            text = "Sequência",
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

                if (isLoading) {
                    CenteredZenithLoading(contentPadding = PaddingValues(bottom = BottomNavListPadding))
                } else {
                    val streak = uiState.profile?.streak ?: 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .border(1.dp, Color(0xFFE1E7DD), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FFF8)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .background(Color(0xFFEAF3DE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.firestreak),
                                    contentDescription = "Sequência",
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Text(
                                text = "$streak ${if (streak == 1) "dia" else "dias"}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.W800,
                                    color = Color(0xFF238D25)
                                )
                            )
                            Text(
                                text = "Atividades em dias seguidos mantêm a sequência ativa.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF667066)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(BottomNavListPadding))
                }
            }
        }
    }
}
