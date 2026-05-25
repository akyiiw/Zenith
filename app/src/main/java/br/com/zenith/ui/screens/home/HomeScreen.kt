package br.com.zenith.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.components.app.CustomBottomNavigationBar
import br.com.zenith.ui.components.home.ActivitySection
import br.com.zenith.ui.components.home.Header
import br.com.zenith.ui.components.home.WelcomeCard
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.profile.UserViewModel

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel()
    val user by userViewModel.userState.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.fetchUserProfile(context)
    }

    ZenithTheme {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                Column {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    CustomBottomNavigationBar(navController = navController)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
            ) {
                item {
                    if (isLoading || user == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Green, strokeWidth = 3.dp)
                        }
                    } else {
                        Header(
                            pictureHash = user?.pictureHash,
                            onProfileClick = {
                                navController.navigate("profile")
                            }
                        )
                    }
                }

                item {
                    if (isLoading || user == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(49.dp)
                                .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        )
                    } else {
                        WelcomeCard(user = user)
                    }
                }

                item {
                    ActivitySection()
                }
            }
        }
    }
}
