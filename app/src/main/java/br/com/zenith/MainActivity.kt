package br.com.zenith

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.zenith.data.SupabaseConfig
import br.com.zenith.ui.animations.ZenithLoading
import br.com.zenith.ui.components.app.CustomBottomNavigationBar
import br.com.zenith.ui.notifications.ZenithTopNotificationHost
import br.com.zenith.ui.screens.activity.ActivityDetailScreen
import br.com.zenith.ui.screens.activity.ActivityRegisteredScreen
import br.com.zenith.ui.screens.activity.NewActivityScreen
import br.com.zenith.ui.screens.activity.RegisterActivityScreen
import br.com.zenith.ui.screens.activity.SleepSettingsScreen
import br.com.zenith.ui.screens.activity.StartActivityScreen
import br.com.zenith.ui.screens.app.LoginScreen
import br.com.zenith.ui.screens.app.OnboardingScreen
import br.com.zenith.ui.screens.app.RegisterScreen
import br.com.zenith.ui.screens.app.VerifyEmailScreen
import br.com.zenith.ui.screens.app.WelcomeScreen
import br.com.zenith.ui.screens.home.HomeScreen
import br.com.zenith.ui.screens.progress.ProgressReportsScreen
import br.com.zenith.ui.screens.progress.ProgressScreen
import br.com.zenith.ui.screens.goals.GoalsScreen
import br.com.zenith.ui.screens.profile.EditProfileScreen
import br.com.zenith.ui.screens.profile.AchievementsScreen
import br.com.zenith.ui.screens.profile.BadgeSelectScreen
import br.com.zenith.ui.screens.profile.ChallengeAwardsScreen
import br.com.zenith.ui.screens.profile.MedalsScreen
import br.com.zenith.ui.screens.profile.ProfileActivitiesScreen
import br.com.zenith.ui.screens.profile.ProfileFriendsScreen
import br.com.zenith.ui.screens.profile.ProfileScreen
import br.com.zenith.ui.screens.profile.StreakScreen
import br.com.zenith.ui.screens.profile.TitleSelectScreen
import br.com.zenith.viewmodels.progress.ProgressPeriod
import br.com.zenith.ui.screens.challenge.CreateChallengeScreen
import br.com.zenith.ui.screens.challenge.ChallengeScreen
import br.com.zenith.ui.screens.settings.AboutSettingsScreen
import br.com.zenith.ui.screens.settings.AppBlockSettingsScreen
import br.com.zenith.ui.screens.settings.EmailSettingsScreen
import br.com.zenith.ui.screens.settings.PasswordSettingsScreen
import br.com.zenith.ui.screens.settings.PrivacySettingsScreen
import br.com.zenith.ui.screens.settings.SecuritySettingsScreen
import br.com.zenith.ui.screens.settings.SettingsScreen
import br.com.zenith.ui.screens.profile.PublicProfileScreen
import br.com.zenith.ui.screens.social.SocialScreen
import br.com.zenith.ui.theme.White
import br.com.zenith.ui.theme.ZenithTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import java.net.URLDecoder

private val bottomTabRoutes = listOf(
    "challenge",
    "social",
    "home",
    "progress",
    "settings"
)

private fun isBottomTabRoute(route: String): Boolean = route in bottomTabRoutes

private fun bottomTabDirection(initialRoute: String, targetRoute: String): Int {
    val initialIndex = bottomTabRoutes.indexOf(initialRoute)
    val targetIndex = bottomTabRoutes.indexOf(targetRoute)
    return if (initialIndex >= 0 && targetIndex >= 0) {
        targetIndex.compareTo(initialIndex)
    } else {
        0
    }
}

private fun isVerticalForwardRoute(route: String): Boolean =
        route == "sleep_settings" ||
        route == "app_blocks" ||
        route == "create_challenge" ||
        route.startsWith("settings_") ||
        route.startsWith("achievements") ||
        route.startsWith("challenge_awards") ||
        route.startsWith("new_activity")

private fun isActivityFlowRoute(route: String): Boolean =
        route.startsWith("start_activity") ||
        route.startsWith("register_activity") ||
        route.startsWith("activity_registered")

private fun isDetailForwardRoute(route: String): Boolean =
        route.startsWith("challenge/") ||
        route.startsWith("activity_detail/")

private fun isSameTabReload(initialRoute: String, targetRoute: String): Boolean =
    initialRoute == targetRoute &&
        targetRoute != "home" &&
        isBottomTabRoute(targetRoute)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()

        enableEdgeToEdge()

        setContent {
            ZenithTheme {

                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    SupabaseConfig.getClient().auth.sessionStatus.collectLatest { status ->
                        when (status) {

                            is SessionStatus.Initializing -> {}

                            is SessionStatus.Authenticated -> {
                                if (startDestination == null) {
                                    startDestination = "home"
                                }
                            }

                            is SessionStatus.NotAuthenticated -> {
                                if (startDestination == null) {
                                    startDestination = "welcome"
                                }
                            }

                            else -> {}
                        }
                    }
                }

                val stableDestination = startDestination

                if (stableDestination == null) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(White),
                        contentAlignment = Alignment.Center
                    ) {
                        ZenithLoading(
                            modifier = Modifier.size(120.dp)
                        )
                    }

                } else {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route.orEmpty()
                    val showBottomBar = currentRoute in setOf(
                        "challenge",
                        "social",
                        "home",
                        "progress",
                        "settings",
                        "profile"
                    ) || currentRoute.startsWith("user_profile")

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = stableDestination,
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = {
                                val initial = initialState.destination.route.orEmpty()
                                val target = targetState.destination.route.orEmpty()
                                val tabDirection = bottomTabDirection(initial, target)

                                if (isSameTabReload(initial, target)) {
                                    slideInVertically(
                                        animationSpec = tween(260),
                                        initialOffsetY = { -it / 3 }
                                    )
                                } else if (tabDirection != 0) {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { if (tabDirection > 0) it else -it }
                                    )
                                } else if (isActivityFlowRoute(target)) {
                                    fadeIn(animationSpec = tween(180)) +
                                        slideInVertically(
                                            animationSpec = tween(300),
                                            initialOffsetY = { it / 5 }
                                        )
                                } else if (isDetailForwardRoute(target)) {
                                    fadeIn(animationSpec = tween(180)) +
                                        slideInHorizontally(
                                            animationSpec = tween(280),
                                            initialOffsetX = { it / 4 }
                                        )
                                } else if (isVerticalForwardRoute(target)) {
                                    fadeIn(animationSpec = tween(180)) +
                                        slideInVertically(
                                            animationSpec = tween(300),
                                            initialOffsetY = { it / 4 }
                                        )
                                } else {
                                    fadeIn(animationSpec = tween(170)) +
                                        slideInHorizontally(
                                            animationSpec = tween(260),
                                            initialOffsetX = { it / 3 }
                                        )
                                }
                            },
                            exitTransition = {
                                val initial = initialState.destination.route.orEmpty()
                                val target = targetState.destination.route.orEmpty()
                                val tabDirection = bottomTabDirection(initial, target)

                                if (isSameTabReload(initial, target)) {
                                    slideOutVertically(
                                        animationSpec = tween(240),
                                        targetOffsetY = { it }
                                    )
                                } else if (tabDirection != 0) {
                                    slideOutHorizontally(
                                        animationSpec = tween(240),
                                        targetOffsetX = { if (tabDirection > 0) -it else it }
                                    )
                                } else if (isActivityFlowRoute(target) || isVerticalForwardRoute(target)) {
                                    fadeOut(animationSpec = tween(140)) +
                                        slideOutVertically(
                                            animationSpec = tween(240),
                                            targetOffsetY = { -it / 10 }
                                        )
                                } else if (isDetailForwardRoute(target)) {
                                    fadeOut(animationSpec = tween(140)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(240),
                                            targetOffsetX = { -it / 8 }
                                        )
                                } else {
                                    fadeOut(animationSpec = tween(130)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(240),
                                            targetOffsetX = { -it / 8 }
                                        )
                                }
                            },
                            popEnterTransition = {
                                val target = targetState.destination.route.orEmpty()
                                if (isActivityFlowRoute(target) || isVerticalForwardRoute(target)) {
                                    fadeIn(animationSpec = tween(160)) +
                                        slideInVertically(
                                            animationSpec = tween(260),
                                            initialOffsetY = { -it / 8 }
                                        )
                                } else if (isDetailForwardRoute(target)) {
                                    fadeIn(animationSpec = tween(160)) +
                                        slideInHorizontally(
                                            animationSpec = tween(240),
                                            initialOffsetX = { -it / 8 }
                                        )
                                } else {
                                    fadeIn(animationSpec = tween(150)) +
                                        slideInHorizontally(
                                            animationSpec = tween(240),
                                            initialOffsetX = { -it / 4 }
                                        )
                                }
                            },
                            popExitTransition = {
                                val initial = initialState.destination.route.orEmpty()
                                if (isActivityFlowRoute(initial) || isVerticalForwardRoute(initial)) {
                                    fadeOut(animationSpec = tween(140)) +
                                        slideOutVertically(
                                            animationSpec = tween(260),
                                            targetOffsetY = { it / 4 }
                                        )
                                } else if (isDetailForwardRoute(initial)) {
                                    fadeOut(animationSpec = tween(130)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(240),
                                            targetOffsetX = { it / 4 }
                                        )
                                } else {
                                    fadeOut(animationSpec = tween(130)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(240),
                                            targetOffsetX = { it / 3 }
                                        )
                                }
                            }
                        ) {
                        composable("welcome") {
                            WelcomeScreen(navController)
                        }

                        composable("login") {
                            LoginScreen(navController)
                        }

                        composable("register") {
                            RegisterScreen(navController)
                        }

                        composable("onboarding") {
                            OnboardingScreen(navController)
                        }

                        composable("verify_email") {
                            VerifyEmailScreen(navController)
                        }

                        composable("home") {
                            HomeScreen(navController)
                        }

                        composable("challenge") {
                            ChallengeScreen(navController)
                        }

                        composable("challenge/{challengeId}") { back ->
                            ChallengeScreen(
                                navController = navController,
                                initialChallengeId = back.arguments?.getString("challengeId")
                            )
                        }

                        composable("create_challenge") {
                            CreateChallengeScreen(navController)
                        }

                        composable("social") {
                            SocialScreen(navController)
                        }

                        composable("user_profile/{userId}") { back ->
                            PublicProfileScreen(
                                navController = navController,
                                userId = back.arguments?.getString("userId") ?: ""
                            )
                        }

                        composable("profile_activities/{userId}") { back ->
                            ProfileActivitiesScreen(
                                navController = navController,
                                userId = back.arguments?.getString("userId") ?: ""
                            )
                        }

                        composable("profile_friends/{userId}") { back ->
                            ProfileFriendsScreen(
                                navController = navController,
                                userId = back.arguments?.getString("userId") ?: ""
                            )
                        }

                        composable("progress") {
                            ProgressScreen(navController)
                        }

                        composable("progress_reports/{period}") { back ->
                            ProgressReportsScreen(
                                navController = navController,
                                initialPeriod = runCatching {
                                    ProgressPeriod.valueOf(back.arguments?.getString("period") ?: ProgressPeriod.Weekly.name)
                                }.getOrDefault(ProgressPeriod.Weekly)
                            )
                        }

                        composable("goals") {
                            GoalsScreen(navController)
                        }

                        composable("settings") {
                            SettingsScreen(navController)
                        }

                        composable("settings_email") {
                            EmailSettingsScreen(navController)
                        }

                        composable("settings_password") {
                            PasswordSettingsScreen(navController)
                        }

                        composable("settings_security") {
                            SecuritySettingsScreen(navController)
                        }

                        composable("settings_privacy") {
                            PrivacySettingsScreen(navController)
                        }

                        composable("settings_about") {
                            AboutSettingsScreen(navController)
                        }

                        composable("profile") {
                            ProfileScreen(navController)
                        }

                        composable("edit_profile") {
                            EditProfileScreen(navController)
                        }

                        composable("title_select") {
                            TitleSelectScreen(navController)
                        }

                        composable("badge_select") {
                            BadgeSelectScreen(navController)
                        }

                        composable("medals") {
                            MedalsScreen(navController)
                        }

                        composable("challenge_awards/{userId}") { back ->
                            ChallengeAwardsScreen(
                                navController = navController,
                                userId = back.arguments?.getString("userId") ?: ""
                            )
                        }

                        composable("streak/{userId}") { back ->
                            StreakScreen(
                                navController = navController,
                                userId = back.arguments?.getString("userId") ?: ""
                            )
                        }

                        composable("achievements/{userId}") { back ->
                            AchievementsScreen(
                                navController = navController,
                                userId = back.arguments?.getString("userId") ?: ""
                            )
                        }

                        composable("app_blocks") {
                            AppBlockSettingsScreen(navController)
                        }

                        composable("activity_detail/{atividadeId}") { back ->

                            ActivityDetailScreen(
                                navController,
                                back.arguments?.getString("atividadeId")
                            )
                        }

                        composable("new_activity") {
                            NewActivityScreen(navController)
                        }

                        composable("sleep_settings") {
                            SleepSettingsScreen(navController)
                        }

                        composable(
                            "start_activity/{exercicioId}/{exercicioNome}/{exercicioUnidade}"
                        ) { back ->

                            StartActivityScreen(
                                navController = navController,

                                exercicioId = back.arguments
                                    ?.getString("exercicioId") ?: "",

                                exercicioNome = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioNome") ?: "",
                                    "UTF-8"
                                ),

                                exercicioUnidade = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioUnidade") ?: "",
                                    "UTF-8"
                                )
                            )
                        }

                        composable(
                            "start_activity/{exercicioId}/{exercicioNome}/{exercicioUnidade}/{desafioId}"
                        ) { back ->

                            StartActivityScreen(
                                navController = navController,

                                exercicioId = back.arguments
                                    ?.getString("exercicioId") ?: "",

                                exercicioNome = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioNome") ?: "",
                                    "UTF-8"
                                ),

                                exercicioUnidade = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioUnidade") ?: "",
                                    "UTF-8"
                                ),

                                desafioId = back.arguments
                                    ?.getString("desafioId")
                            )
                        }

                        composable(
                            "register_activity/{exercicioId}/{exercicioNome}/{exercicioUnidade}/{duracaoMin}"
                        ) { back ->

                            RegisterActivityScreen(
                                navController = navController,

                                exercicioId = back.arguments
                                    ?.getString("exercicioId") ?: "",

                                exercicioNome = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioNome") ?: "",
                                    "UTF-8"
                                ),

                                exercicioUnidade = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioUnidade") ?: "",
                                    "UTF-8"
                                ),

                                duracaoMin = back.arguments
                                    ?.getString("duracaoMin")
                                    ?.toIntOrNull() ?: 0
                            )
                        }

                        composable(
                            "register_activity/{exercicioId}/{exercicioNome}/{exercicioUnidade}/{duracaoMin}/{verificada}"
                        ) { back ->

                            RegisterActivityScreen(
                                navController = navController,

                                exercicioId = back.arguments
                                    ?.getString("exercicioId") ?: "",

                                exercicioNome = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioNome") ?: "",
                                    "UTF-8"
                                ),

                                exercicioUnidade = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioUnidade") ?: "",
                                    "UTF-8"
                                ),

                                duracaoMin = back.arguments
                                    ?.getString("duracaoMin")
                                    ?.toIntOrNull() ?: 0,

                                verificada = back.arguments
                                    ?.getString("verificada")
                                    ?.toBooleanStrictOrNull() ?: false
                            )
                        }

                        composable(
                            "register_activity/{exercicioId}/{exercicioNome}/{exercicioUnidade}/{duracaoMin}/{verificada}/{desafioId}"
                        ) { back ->

                            RegisterActivityScreen(
                                navController = navController,

                                exercicioId = back.arguments
                                    ?.getString("exercicioId") ?: "",

                                exercicioNome = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioNome") ?: "",
                                    "UTF-8"
                                ),

                                exercicioUnidade = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioUnidade") ?: "",
                                    "UTF-8"
                                ),

                                duracaoMin = back.arguments
                                    ?.getString("duracaoMin")
                                    ?.toIntOrNull() ?: 0,

                                verificada = back.arguments
                                    ?.getString("verificada")
                                    ?.toBooleanStrictOrNull() ?: false,

                                desafioId = back.arguments
                                    ?.getString("desafioId")
                            )
                        }

                        composable("register_activity") {
                            RegisterActivityScreen(navController)
                        }

                        composable(
                            "activity_registered/{verificada}/{exercicioNome}/{valor}/{exercicioUnidade}/{duracaoMin}"
                        ) { back ->

                            ActivityRegisteredScreen(
                                navController = navController,

                                verificada = back.arguments
                                    ?.getString("verificada")
                                    ?.toBooleanStrictOrNull() ?: false,

                                exercicioNome = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioNome") ?: "",
                                    "UTF-8"
                                ),

                                valor = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("valor") ?: "",
                                    "UTF-8"
                                ),

                                unidade = URLDecoder.decode(
                                    back.arguments
                                        ?.getString("exercicioUnidade") ?: "",
                                    "UTF-8"
                                ),

                                duracaoMin = back.arguments
                                    ?.getString("duracaoMin")
                                    ?.toIntOrNull() ?: 0
                            )
                        }
                        }

                        if (showBottomBar) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                CustomBottomNavigationBar(navController = navController)
                            }
                        }

                        ZenithTopNotificationHost(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (permissions.isNotEmpty()) {

            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                1001
            )
        }
    }
}
