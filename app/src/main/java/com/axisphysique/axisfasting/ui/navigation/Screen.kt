package com.axisphysique.axisfasting.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Timer : Screen("timer")
    object Settings : Screen("settings")
    object FastingTips : Screen("fasting_tips")
    object WeightTracker : Screen("weight_tracker")
    object WaterTracker : Screen("water_tracker")
}
