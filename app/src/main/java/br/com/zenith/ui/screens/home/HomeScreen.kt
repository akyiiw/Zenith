package br.com.zenith.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import br.com.zenith.ui.components.home.ActivitySection
import br.com.zenith.ui.components.home.FeedSection
import br.com.zenith.ui.components.home.HomeGoalItem
import br.com.zenith.ui.components.home.Header
import br.com.zenith.ui.components.home.NotificationsDrawer
import br.com.zenith.ui.components.home.PublishActivitySheet
import br.com.zenith.ui.components.home.WelcomeCard
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.ZenithTheme
import br.com.zenith.viewmodels.home.HomeFeedViewModel
import br.com.zenith.viewmodels.home.HomeNotificationsViewModel
import br.com.zenith.viewmodels.profile.UserViewModel
import br.com.zenith.viewmodels.progress.ProgressViewModel

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel()
    val notificationsViewModel: HomeNotificationsViewModel = viewModel()
    val progressViewModel: ProgressViewModel = viewModel()
    val homeFeedViewModel: HomeFeedViewModel = viewModel()
    val user by userViewModel.userState.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val notificationsState by notificationsViewModel.uiState.collectAsState()
    val progressState by progressViewModel.uiState.collectAsState()
    val feedState by homeFeedViewModel.uiState.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }
    var showPublishSheet by remember { mutableStateOf(false) }
    val homeGoals = progressState.weeklyGoals.map { goal ->
        HomeGoalItem(periodLabel = "Meta semanal", goal = goal)
    } + progressState.monthlyGoals.map { goal ->
        HomeGoalItem(periodLabel = "Meta mensal", goal = goal)
    }

    LaunchedEffect(Unit) {
        userViewModel.fetchUserProfile(context)
        notificationsViewModel.load(context)
        progressViewModel.load(context)
        homeFeedViewModel.load(context)
    }

    ZenithTheme {
        Scaffold(
            containerColor = Color.White
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(top = 19.dp, bottom = 24.dp)
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
                                notificationCount = notificationsState.unreadCount,
                                onNotificationsClick = { showNotifications = true },
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
                        ActivitySection(
                            goals = homeGoals,
                            isLoading = progressState.isLoading,
                            onManageGoals = { navController.navigate("goals") }
                        )
                    }

                    item {
                        FeedSection(
                            items = feedState.items,
                            isLoading = feedState.isLoading,
                            onPublishClick = { showPublishSheet = true },
                            onOpenProfile = { profile -> navController.navigate("user_profile/${profile.id}") },
                            onOpenActivity = { activity -> navController.navigate("activity_detail/${activity.id}") }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showNotifications,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.18f))
                            .clickable { showNotifications = false }
                    )
                }

                AnimatedVisibility(
                    visible = showNotifications,
                    enter = slideInHorizontally(initialOffsetX = { -it }),
                    exit = slideOutHorizontally(targetOffsetX = { -it }),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    NotificationsDrawer(
                        notifications = notificationsState.notifications,
                        isLoading = notificationsState.isLoading,
                        onNotificationClick = { notification ->
                            showNotifications = false
                            notification.targetRoute?.let { navController.navigate(it) }
                        }
                    )
                }
            }
        }

        if (showPublishSheet) {
            PublishActivitySheet(
                activities = feedState.publishableActivities,
                isPublishing = feedState.isPublishing,
                onDismiss = { showPublishSheet = false },
                onPublish = { activity ->
                    homeFeedViewModel.publishActivity(activity.id, context)
                    showPublishSheet = false
                }
            )
        }
    }
}
