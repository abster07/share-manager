package com.meroshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.meroshare.ui.accounts.AccountsScreen
import com.meroshare.ui.results.ResultsScreen
import com.meroshare.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeroShareTheme {
                MeroShareApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Results  : Screen("results", "Results", Icons.Default.BarChart)
    object Accounts : Screen("accounts", "Accounts", Icons.Default.ManageAccounts)
}

@Composable
fun MeroShareApp() {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Results, Screen.Accounts)

    Scaffold(
        containerColor = Navy900,
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                tonalElevation = 0.dp
            ) {
                val navBackStack by navController.currentBackStackEntryAsState()
                val current = navBackStack?.destination
                tabs.forEach { screen ->
                    val selected = current?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, screen.label) },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold400,
                            selectedTextColor = Gold400,
                            indicatorColor = Navy700,
                            unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF475569),
                            unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF475569)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Results.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Results.route) { ResultsScreen() }
            composable(Screen.Accounts.route) { AccountsScreen() }
        }
    }
}
