package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English")
}

object Localizer {
    private val translations = mapOf(
        "en" to mapOf(
            "app_name" to "Axis IF",
            "tab_fasting" to "Fasting",
            "tab_hydration" to "Hydration",
            "tab_analytics" to "Analytics",
            "tab_facts" to "Facts",
            "title_fasting" to "Fasting Tracker",
            "title_hydration" to "Hydration Tracker",
            "title_analytics" to "Metabolic Insights",
            "title_facts" to "Facts & Tips",
            "settings" to "Settings",
            "app_prefs" to "APPLICATION PREFERENCES",
            "dark_theme" to "Dark Theme Mode",
            "dark_theme_desc" to "Metabolic-friendly dark interface canvas",
            "notifications" to "System Notifications",
            "notifications_desc" to "Receive biological stage transition updates",
            "personal_profile" to "Personal Profile & Metrics",
            "profile_desc" to "Edit age, gender, weight, height & units",
            "app_language" to "Application Language",
            "app_language_desc" to "Change the language of the application",
            "destruction" to "DATA DESTRUCTION",
            "destroy_btn" to "Destroy All Fasts & Logs",
            "confirm_dest" to "Confirm Data Destruction",
            "confirm_dest_desc" to "Are you absolutely sure you want to permanently delete all fasting sessions and water history?",
            "destroy_all" to "Destroy All Data",
            "cancel" to "Cancel",
            "dismiss" to "Dismiss",
            "edit_profile" to "Edit Personal Profile",
            "gender" to "Gender Biology",
            "male" to "Male",
            "female" to "Female",
            "age" to "Age (Years)",
            "weight" to "Weight",
            "height" to "Height",
            "unit_system" to "Unit System",
            "metric" to "Metric (kg, cm)",
            "imperial" to "Imperial (lbs, in)",
            "save_profile" to "Save Profile Changes",
            "water_stats" to "Hydration Analysis",
            "water_average" to "Daily Average",
            "water_total" to "Total Water Logged",
            "metabolic_insights" to "Metabolic Insights",
            "insights_subtitle" to "Unlock your fat digestion and clean cellular autophagic scores.",
            "empty_analytics_title" to "No Analytics Available Yet",
            "empty_analytics_desc" to "Fasting and hydration records must be logged to calculate metabolic and autophagy thresholds. Start your journey today!",
            "metabolic_activity" to "Fasting History & Analysis",
            "goal_success" to "Goal Success",
            "average_period" to "Average Period",
            "duration_distribution" to "Duration Distribution",
            "no_logged_records" to "No logged records in this selected period.",
            "scoring_matrix" to "Biology Scoring Matrix",
            "ketosis_reached" to "Ketosis (Fat Burning) Zone Reached",
            "autophagy_triggered" to "Autophagy (Cellular Clean up) Triggered",
            "weekly" to "Weekly",
            "monthly" to "Monthly",
            "yearly" to "Yearly",
            "about_axis" to "ABOUT AXIS IF",
            "times" to "times",
            "fasting" to "Fasting",
            "hydration" to "Hydration"
        )
    )

    fun getString(key: String, languageCode: String): String {
        return translations[languageCode]?.get(key)
            ?: translations["en"]?.get(key)
            ?: key
    }
}

val LocalLang = staticCompositionLocalOf { "en" }

@Composable
fun t(key: String): String {
    val currentLang = LocalLang.current
    return Localizer.getString(key, currentLang)
}
