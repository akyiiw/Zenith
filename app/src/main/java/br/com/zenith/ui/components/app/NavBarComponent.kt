package br.com.zenith.ui.components.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.com.zenith.R
import br.com.zenith.ui.theme.Green
import br.com.zenith.ui.theme.SecondaryGreen

sealed class NavItem(val route: String, val iconRes: Int, val contentDescription: String) {
    object Challenge : NavItem("challenge", R.drawable.nav_challenge, "Desafios")
    object Social : NavItem("social", R.drawable.nav_social, "Social")
    object Home : NavItem("home", R.drawable.nav_home, "Home")
    object Progress : NavItem("progress", R.drawable.nav_progress, "Progress")
    object Settings : NavItem("settings", R.drawable.nav_settings, "settings")
}

@Composable
fun CustomBottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavItem.Challenge,
        NavItem.Social,
        NavItem.Home,
        NavItem.Progress,
        NavItem.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedRoute = items.firstOrNull { it.route == currentRoute }?.route

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.LightGray
        )
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                val isHomeAndSelected = item.route == "home" && isSelected

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (isHomeAndSelected) {
                            navController.navigate("new_activity")
                        } else {
                            val shouldReloadSelectedTab = isSelected && item.route != "home"
                            navController.navigate(item.route) {
                                popUpTo("home") { inclusive = item.route == "home" }
                                launchSingleTop = !shouldReloadSelectedTab
                            }
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = if (isHomeAndSelected) {
                                    R.drawable.nav_add_activity
                                } else {
                                    item.iconRes
                                }
                            ),
                            contentDescription = if (isHomeAndSelected) {
                                "Nova Atividade"
                            } else {
                                item.contentDescription
                            },
                            modifier = Modifier.size(if (isSelected) 26.dp else 22.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Green,
                        unselectedIconColor = Color.Gray,
                        indicatorColor = SecondaryGreen.copy(alpha = 0.3f)
                    ),
                    alwaysShowLabel = false
                )
            }
        }
    }
}
