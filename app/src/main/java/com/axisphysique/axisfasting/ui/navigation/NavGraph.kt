package com.axisphysique.axisfasting.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.axisphysique.axisfasting.ui.screens.home.HomeScreen
import com.axisphysique.axisfasting.ui.screens.timer.TimerScreen
import com.axisphysique.axisfasting.ui.screens.settings.SettingsScreen
import com.axisphysique.axisfasting.ui.screens.tips.FastingTipsScreen
import com.axisphysique.axisfasting.ui.screens.weight.WeightTrackerScreen
import com.axisphysique.axisfasting.ui.screens.water.WaterTrackerScreen
import com.axisphysique.axisfasting.ui.theme.ThemeViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToTips = { navController.navigate(Screen.FastingTips.route) },
                onNavigateToWeight = { navController.navigate(Screen.WeightTracker.route) },
                onNavigateToWater = { navController.navigate(Screen.WaterTracker.route) },
                themeViewModel = themeViewModel
            )
        }
        composable(Screen.Timer.route) {
            TimerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTips = { navController.navigate(Screen.FastingTips.route) },
                themeViewModel = themeViewModel
            )
        }
        composable(Screen.FastingTips.route) {
            FastingTipsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WeightTracker.route) {
            WeightTrackerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WaterTracker.route) {
            WaterTrackerScreen(onBack = { navController.popBackStack() })
        }
    }
}
