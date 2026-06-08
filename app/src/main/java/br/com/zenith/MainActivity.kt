package br.com.zenith

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.HorizontalDivider
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
import br.com.zenith.ui.screens.progress.ProgressScreen
import br.com.zenith.ui.screens.profile.EditProfileScreen
import br.com.zenith.ui.screens.profile.ProfileScreen
import br.com.zenith.ui.screens.profile.TitleSelectScreen
import br.com.zenith.ui.screens.challenge.CreateChallengeScreen
import br.com.zenith.ui.screens.challenge.ChallengeScreen
import br.com.zenith.ui.screens.settings.SettingsScreen
import br.com.zenith.ui.screens.profile.PublicProfileScreen
import br.com.zenith.ui.screens.social.SocialScreen
import br.com.zenith.ui.theme.White
import br.com.zenith.ui.theme.ZenithTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import java.net.URLDecoder

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
                                val target = targetState.destination.route.orEmpty()
                                if (target == "sleep_settings" ||
                                    target == "create_challenge" ||
                                    target.startsWith("new_activity") ||
                                    target.startsWith("start_activity") ||
                                    target.startsWith("register_activity")
                                ) {
                                    slideInVertically(
                                        animationSpec = tween(320),
                                        initialOffsetY = { it }
                                    )
                                } else {
                                    slideInHorizontally(
                                        animationSpec = tween(260),
                                        initialOffsetX = { it }
                                    )
                                }
                            },
                            exitTransition = {
                                val target = targetState.destination.route.orEmpty()
                                if (target == "sleep_settings" ||
                                    target == "create_challenge" ||
                                    target.startsWith("new_activity") ||
                                    target.startsWith("start_activity") ||
                                    target.startsWith("register_activity")
                                ) {
                                    slideOutVertically(
                                        animationSpec = tween(260),
                                        targetOffsetY = { -it / 4 }
                                    )
                                } else {
                                    slideOutHorizontally(
                                        animationSpec = tween(240),
                                        targetOffsetX = { -it / 3 }
                                    )
                                }
                            },
                            popEnterTransition = {
                                val target = targetState.destination.route.orEmpty()
                                if (target == "sleep_settings" ||
                                    target == "create_challenge" ||
                                    target.startsWith("new_activity") ||
                                    target.startsWith("start_activity") ||
                                    target.startsWith("register_activity")
                                ) {
                                    slideInVertically(
                                        animationSpec = tween(260),
                                        initialOffsetY = { -it / 4 }
                                    )
                                } else {
                                    slideInHorizontally(
                                        animationSpec = tween(240),
                                        initialOffsetX = { -it / 3 }
                                    )
                                }
                            },
                            popExitTransition = {
                                val initial = initialState.destination.route.orEmpty()
                                if (initial == "sleep_settings" ||
                                    initial == "create_challenge" ||
                                    initial.startsWith("new_activity") ||
                                    initial.startsWith("start_activity") ||
                                    initial.startsWith("register_activity")
                                ) {
                                    slideOutVertically(
                                        animationSpec = tween(280),
                                        targetOffsetY = { it }
                                    )
                                } else {
                                    slideOutHorizontally(
                                        animationSpec = tween(240),
                                        targetOffsetX = { it }
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

                        composable("progress") {
                            ProgressScreen(navController)
                        }

                        composable("settings") {
                            SettingsScreen(navController)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 1.dp,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                CustomBottomNavigationBar(navController = navController)
                            }
                        }
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
