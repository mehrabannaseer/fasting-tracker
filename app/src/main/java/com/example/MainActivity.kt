package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.DatePickerDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.TrackerDatabase
import com.example.data.model.FastingLog
import com.example.data.model.WaterLog
import com.example.data.repository.TrackerRepository
import com.example.ui.TrackerViewModel
import com.example.ui.TrackerViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.t
import com.example.ui.LocalLang
import com.example.ui.AppLanguage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TrackerDatabase.getDatabase(this)
        val repository = TrackerRepository(database.trackerDao())

        val viewModel: TrackerViewModel by viewModels {
            TrackerViewModelFactory(repository)
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences("fasting_tracker_prefs", android.content.Context.MODE_PRIVATE) }
            var isDarkModeEnabled by remember {
                mutableStateOf(sharedPreferences.getBoolean("dark_mode_enabled", false))
            }
            var appLanguage by remember {
                mutableStateOf(sharedPreferences.getString("app_language", "en") ?: "en")
            }

            MyApplicationTheme(darkTheme = isDarkModeEnabled) {
                CompositionLocalProvider(LocalLang provides appLanguage) {
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(2200) // Show logo splash for 2.2 seconds
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashWelcomeScreen()
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            isDarkModeEnabled = isDarkModeEnabled,
                            onThemeToggle = { enabled ->
                                sharedPreferences.edit().putBoolean("dark_mode_enabled", enabled).apply()
                                isDarkModeEnabled = enabled
                            },
                            appLanguage = appLanguage,
                            onLanguageToggle = { lang ->
                                sharedPreferences.edit().putString("app_language", lang).apply()
                                appLanguage = lang
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: TrackerViewModel,
    isDarkModeEnabled: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    appLanguage: String,
    onLanguageToggle: (String) -> Unit
) {
    // Collect UI state from ViewModel
    val activeFastingLog by viewModel.activeFastingLog.collectAsStateWithLifecycle()
    val fastingHistory by viewModel.fastingHistory.collectAsStateWithLifecycle()
    val fastingDurationMillis by viewModel.fastingDurationMillis.collectAsStateWithLifecycle()
    val selectedTargetHours by viewModel.selectedTargetHours.collectAsStateWithLifecycle()
    val waterGoalMl by viewModel.waterGoalMl.collectAsStateWithLifecycle()
    val todayWaterLogs by viewModel.todayWaterLogs.collectAsStateWithLifecycle()
    val todayWaterTotal by viewModel.todayWaterTotal.collectAsStateWithLifecycle()
    val waterHistory by viewModel.waterHistory.collectAsStateWithLifecycle(initialValue = emptyList())

    // Dialog state
    var showWaterGoalDialog by remember { mutableStateOf(false) }
    var showCustomWaterDialog by remember { mutableStateOf(false) }
    var showEndFastDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var fastingNotes by remember { mutableStateOf("") }
    
    // Tab State: Local screen tabs (0 for Fasting, 1 for Hydration, 2 for Analytics)
    var activeTab by remember { mutableStateOf(0) }

    // Adaptive configuration - handle wide screens gracefully
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    // Onboarding Integration
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("fasting_tracker_prefs", android.content.Context.MODE_PRIVATE) }
    var useMetric by remember { mutableStateOf(sharedPreferences.getBoolean("use_metric", true)) }
    var showOnboarding by remember {
        mutableStateOf(!sharedPreferences.getBoolean("onboarding_completed", false))
    }

    var userAge by remember { mutableStateOf(sharedPreferences.getInt("user_age", 25)) }
    var userGender by remember { mutableStateOf(sharedPreferences.getString("user_gender", "Female") ?: "Female") }
    var userWeight by remember { mutableStateOf(sharedPreferences.getFloat("user_weight", 65f)) }
    var userHeight by remember { mutableStateOf(sharedPreferences.getFloat("user_height", 168f)) }
    var userActivityLevel by remember { mutableStateOf(sharedPreferences.getString("user_activity_level", "Lightly Active") ?: "Lightly Active") }

    if (showOnboarding) {
        OnboardingScreen(
            onCompleted = { age, gender, weight, height, activityLevel, userUnitMetric ->
                sharedPreferences.edit()
                    .putBoolean("onboarding_completed", true)
                    .putInt("user_age", age)
                    .putString("user_gender", gender)
                    .putFloat("user_weight", weight)
                    .putFloat("user_height", height)
                    .putString("user_activity_level", activityLevel)
                    .putBoolean("use_metric", userUnitMetric)
                    .apply()
                userAge = age
                userGender = gender
                userWeight = weight
                userHeight = height
                userActivityLevel = activityLevel
                useMetric = userUnitMetric
                showOnboarding = false
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Schedule, contentDescription = t("tab_fasting")) },
                    label = { Text(t("tab_fasting")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.LocalDrink, contentDescription = t("tab_hydration")) },
                    label = { Text(t("tab_hydration")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = t("tab_analytics")) },
                    label = { Text(t("tab_analytics")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Lightbulb, contentDescription = t("tab_facts")) },
                    label = { Text(t("tab_facts")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "AXIS IF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (activeTab) {
                                0 -> t("title_fasting")
                                1 -> t("title_hydration")
                                2 -> t("title_analytics")
                                3 -> t("title_facts")
                                else -> t("app_name")
                            },
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-1).sp
                            ),
                            modifier = Modifier.testTag("app_title")
                        )
                    }

                    // Rounded Settings Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showSettingsDialog = true }
                            .testTag("settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = t("settings"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // User Profile Compact Summary Chip
                if (sharedPreferences.getBoolean("onboarding_completed", false)) {
                    val weightDisplay = if (useMetric) "${String.format("%.1f", userWeight)} kg" else "${String.format("%.1f", userWeight * 2.20462f)} lbs"
                    val heightDisplay = if (useMetric) "${String.format("%.1f", userHeight)} cm" else "${String.format("%.1f", userHeight / 2.54f)} in"
                    val genderDisplay = if (userGender == "Female") t("female") else t("male")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${genderDisplay}, ${userAge}y • ${weightDisplay} • ${heightDisplay} • ${userActivityLevel}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Adaptive Layout
        if (isWideScreen) {
            if (activeTab == 2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    AnalyticsPane(
                        fastingHistory = fastingHistory,
                        waterHistory = waterHistory
                    )
                }
            } else if (activeTab == 3) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    FactsPane()
                }
            } else {
                // Tablet/Wide Screen layout - continuous view of both Fasting and Water Tracker side-by-side
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fasting Panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 500.dp)
                            .fillMaxHeight()
                    ) {
                        FastingPane(
                            activeFastingLog = activeFastingLog,
                            fastingHistory = fastingHistory,
                            fastingDurationMillis = fastingDurationMillis,
                            selectedTargetHours = selectedTargetHours,
                            onStartFasting = { target, customTime -> viewModel.startFasting(target, customTime) },
                            onEndClick = {
                                fastingNotes = ""
                                showEndFastDialog = true
                            },
                            onCancelFasting = { viewModel.cancelFasting() },
                            onDeleteLog = { log -> viewModel.deleteFastingLog(log) },
                            onTargetSelected = { hr -> viewModel.updateSelectedTargetHours(hr) },
                            onCustomFastLog = { startTime, endTime, notes ->
                                viewModel.startFasting(selectedTargetHours, startTime)
                                viewModel.endFasting(notes = notes, customEndTime = endTime)
                            }
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Water Panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 500.dp)
                            .fillMaxHeight()
                    ) {
                        WaterPane(
                            waterGoalMl = waterGoalMl,
                            todayWaterLogs = todayWaterLogs,
                            todayWaterTotal = todayWaterTotal,
                            onAddWater = { amount -> viewModel.addWater(amount) },
                            onDeleteWaterLog = { log -> viewModel.deleteWaterLog(log) },
                            onEditGoalClick = { showWaterGoalDialog = true },
                            onAddCustomClick = { showCustomWaterDialog = true }
                        )
                    }
                }
            }
        } else {
            // Mobile portrait layout - switch screens according to current tab selector
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> FastingPane(
                            activeFastingLog = activeFastingLog,
                            fastingHistory = fastingHistory,
                            fastingDurationMillis = fastingDurationMillis,
                            selectedTargetHours = selectedTargetHours,
                            onStartFasting = { target, customTime -> viewModel.startFasting(target, customTime) },
                            onEndClick = {
                                fastingNotes = ""
                                showEndFastDialog = true
                            },
                            onCancelFasting = { viewModel.cancelFasting() },
                            onDeleteLog = { log -> viewModel.deleteFastingLog(log) },
                            onTargetSelected = { hr -> viewModel.updateSelectedTargetHours(hr) },
                            onCustomFastLog = { startTime, endTime, notes ->
                                viewModel.startFasting(selectedTargetHours, startTime)
                                viewModel.endFasting(notes = notes, customEndTime = endTime)
                            }
                        )
                        1 -> WaterPane(
                            waterGoalMl = waterGoalMl,
                            todayWaterLogs = todayWaterLogs,
                            todayWaterTotal = todayWaterTotal,
                            onAddWater = { amount -> viewModel.addWater(amount) },
                            onDeleteWaterLog = { log -> viewModel.deleteWaterLog(log) },
                            onEditGoalClick = { showWaterGoalDialog = true },
                            onAddCustomClick = { showCustomWaterDialog = true }
                        )
                        2 -> AnalyticsPane(
                            fastingHistory = fastingHistory,
                            waterHistory = waterHistory
                        )
                        3 -> FactsPane()
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Personal Profile Dialog (Requirements 1, 2, 5)
    if (showProfileDialog) {
        var activeGender by remember { mutableStateOf(userGender) }
        var activeAge by remember { mutableStateOf(userAge.toString()) }
        var tempUseMetric by remember { mutableStateOf(useMetric) }

        // Represent values in active unit system:
        var activeWeight by remember {
            mutableStateOf(
                if (useMetric) userWeight.toString()
                else String.format("%.1f", userWeight * 2.20462f)
            )
        }
        var activeHeight by remember {
            mutableStateOf(
                if (useMetric) userHeight.toString()
                else String.format("%.1f", userHeight / 2.54f)
            )
        }
        
        var editError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { 
                showProfileDialog = false 
                showSettingsDialog = true // Return user to settings back
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("edit_profile"), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Unit System Switcher (Metric vs Imperial toggling) - Requirement 2
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(t("unit_system"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!tempUseMetric) {
                                        // Convert Imperial fields to Metric
                                        val lbs = activeWeight.toFloatOrNull() ?: 143f
                                        val inches = activeHeight.toFloatOrNull() ?: 67f
                                        activeWeight = String.format("%.1f", lbs / 2.20462f)
                                        activeHeight = String.format("%.1f", inches * 2.54f)
                                        tempUseMetric = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tempUseMetric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    t("metric"), 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (tempUseMetric) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Button(
                                onClick = {
                                    if (tempUseMetric) {
                                        // Convert Metric fields to Imperial
                                        val kg = activeWeight.toFloatOrNull() ?: 65f
                                        val cm = activeHeight.toFloatOrNull() ?: 170f
                                        activeWeight = String.format("%.1f", kg * 2.20462f)
                                        activeHeight = String.format("%.1f", cm / 2.54f)
                                        tempUseMetric = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!tempUseMetric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    t("imperial"), 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (!tempUseMetric) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Gender Biology Selection (Non-Binary is fully removed!) - Requirement 1
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(t("gender"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Female", "Male").forEach { g ->
                                val active = activeGender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { activeGender = g },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (g == "Female") t("female") else t("male"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Age Textfield
                    OutlinedTextField(
                        value = activeAge,
                        onValueChange = { activeAge = it.filter { char -> char.isDigit() } },
                        label = { Text(t("age")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Weight Textfield
                    val weightUnitLabel = if (tempUseMetric) "kg" else "lbs"
                    OutlinedTextField(
                        value = activeWeight,
                        onValueChange = { activeWeight = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("${t("weight")} ($weightUnitLabel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Height Textfield
                    val heightUnitLabel = if (tempUseMetric) "cm" else "in"
                    OutlinedTextField(
                        value = activeHeight,
                        onValueChange = { activeHeight = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("${t("height")} ($heightUnitLabel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    if (editError != null) {
                        Text(
                            text = editError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAge = activeAge.toIntOrNull()
                        val parsedWeight = activeWeight.toFloatOrNull()
                        val parsedHeight = activeHeight.toFloatOrNull()

                        if (parsedAge == null || parsedAge <= 0 || parsedAge > 120) {
                            editError = "Please enter a valid age (1-120)"
                        } else if (parsedWeight == null || parsedWeight <= 0) {
                            editError = "Please enter a valid weight"
                        } else if (parsedHeight == null || parsedHeight <= 0) {
                            editError = "Please enter a valid height"
                        } else {
                            // Compute metric equivalents to save in central storage
                            val weightKg = if (tempUseMetric) parsedWeight else parsedWeight / 2.20462f
                            val heightCm = if (tempUseMetric) parsedHeight else parsedHeight * 2.54f

                            sharedPreferences.edit()
                                .putInt("user_age", parsedAge)
                                .putString("user_gender", activeGender)
                                .putFloat("user_weight", weightKg)
                                .putFloat("user_height", heightCm)
                                .putBoolean("use_metric", tempUseMetric)
                                .apply()

                            // Update local screen state directly
                            userAge = parsedAge
                            userGender = activeGender
                            userWeight = weightKg
                            userHeight = heightCm
                            useMetric = tempUseMetric

                            showProfileDialog = false
                            showSettingsDialog = true // Bring them back to settings
                        }
                    }
                ) {
                    Text(t("save_profile"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProfileDialog = false
                        showSettingsDialog = true // Return user to settings back
                    }
                ) {
                    Text(t("cancel"))
                }
            }
        )
    }

    // 1. Water Goal Customize Dialog
    if (showWaterGoalDialog) {
        var inputGoal by remember { mutableStateOf(waterGoalMl.toString()) }
        AlertDialog(
            onDismissRequest = { showWaterGoalDialog = false },
            title = { Text("Set Daily Water Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Customize your daily water targets in milliliters.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputGoal,
                        onValueChange = { inputGoal = it.filter { char -> char.isDigit() } },
                        label = { Text("Water Goal (ml)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("water_goal_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputGoal.toIntOrNull() ?: 2000
                        viewModel.updateWaterGoal(parsed)
                        showWaterGoalDialog = false
                    },
                    modifier = Modifier.testTag("water_goal_confirm_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaterGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Custom Water Intake Dialog
    if (showCustomWaterDialog) {
        var inputAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomWaterDialog = false },
            title = { Text("Log Custom Water Intake", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Log custom hydration volume in milliliters.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("Volume (ml)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_water_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputAmount.toIntOrNull() ?: 0
                        if (parsed > 0) {
                            viewModel.addWater(parsed)
                        }
                        showCustomWaterDialog = false
                    },
                    modifier = Modifier.testTag("custom_water_confirm_button")
                ) {
                    Text("Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWaterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Finish/End Fast Dialog to capture notes optionally
    if (showEndFastDialog) {
        AlertDialog(
            onDismissRequest = { showEndFastDialog = false },
            title = { Text("Complete Your Fast", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Congratulations on finishing your fast! Write any optional feelings or observations:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = fastingNotes,
                        onValueChange = { fastingNotes = it },
                        label = { Text("Fasting Notes / Feelings") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fasting_notes_input"),
                        placeholder = { Text("E.g., Felt energetic, hungry at hour 14") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.endFasting(notes = fastingNotes)
                        showEndFastDialog = false
                    },
                    modifier = Modifier.testTag("end_fast_confirm_button")
                ) {
                    Text("Complete Fast")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndFastDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Axis IF Custom Settings Dialog
    if (showSettingsDialog) {
        var showClearConfirmation by remember { mutableStateOf(false) }
        var notificationEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("notification_enabled", true)) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("settings"), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // System Settings Section
                    Text(t("app_prefs"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))

                    // Dark Mode Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeToggle(!isDarkModeEnabled) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t("dark_theme"), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(t("dark_theme_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isDarkModeEnabled,
                            onCheckedChange = onThemeToggle,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    // Notification Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !notificationEnabled
                                notificationEnabled = next
                                sharedPreferences.edit().putBoolean("notification_enabled", next).apply()
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t("notifications"), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(t("notifications_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = { next ->
                                notificationEnabled = next
                                sharedPreferences.edit().putBoolean("notification_enabled", next).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Profile Summary Trigger Button (Requirement 5)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(t("personal_profile"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(t("profile_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                showSettingsDialog = false
                                showProfileDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t("personal_profile"))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Danger zone
                    Text(t("destruction"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))

                    Button(
                        onClick = { showClearConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t("destroy_btn"))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // About
                    Text(t("about_axis"), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Axis IF • Version 1.3.0", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("All fasting models and biological stage scoring thresholds conform to modern scientific metabolic and autophagic cellular recycling parameters for optimal blood glucose homeostasis. View stage progress to see exact stage data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(t("dismiss"))
                }
            }
        )

        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = { Text(t("confirm_dest"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text(t("confirm_dest_desc")) },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.clearAllData()
                            sharedPreferences.edit()
                                .remove("onboarding_completed")
                                .remove("user_age")
                                .remove("user_gender")
                                .remove("user_weight")
                                .remove("user_height")
                                .remove("user_activity_level")
                                .remove("use_metric")
                                .apply()
                            showOnboarding = true
                            showClearConfirmation = false
                            showSettingsDialog = false
                        }
                    ) {
                        Text(t("destroy_all"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmation = false }) {
                        Text(t("cancel"))
                    }
                }
            )
        }
    }
}

// ==================== BMI CALCULATOR SECTION ====================

@Composable
fun BmiCalculatorSection() {
    var isMetric by remember { mutableStateOf(true) }
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var bmiResult by remember { mutableStateOf<Float?>(null) }
    var bmiCategory by remember { mutableStateOf("") }
    var categoryDescription by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bmi_calculator_card"),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "BMI Calculator",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                // Metric/Imperial Toggle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        listOf(true, false).forEach { metric ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isMetric == metric) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isMetric = metric }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (metric) "Metric" else "Imperial",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isMetric == metric) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMetric) "WEIGHT (KG)" else "WEIGHT (LBS)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { char -> char.isDigit() || char == '.' } },
                        placeholder = { Text(if (isMetric) "e.g. 75" else "e.g. 165", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMetric) "HEIGHT (CM)" else "HEIGHT (IN)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it.filter { char -> char.isDigit() || char == '.' } },
                        placeholder = { Text(if (isMetric) "e.g. 175" else "e.g. 69", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val w = weightInput.toFloatOrNull()
                    val h = heightInput.toFloatOrNull()
                    if (w != null && h != null && h > 0) {
                        val result = if (isMetric) {
                            w / ((h / 100) * (h / 100))
                        } else {
                            703 * (w / (h * h))
                        }
                        bmiResult = result
                        val (cat, desc) = when {
                            result < 18.5f -> "Underweight" to "You have a body weight lower than what is considered healthy. Consider consulting a nutritionist."
                            result < 25f -> "Normal" to "You have a healthy body weight for your height. Great job maintaining your vitality!"
                            result < 30f -> "Overweight" to "You have a body weight higher than what is considered healthy. Intermittent fasting can help manage this."
                            else -> "Obese" to "Your BMI indicates obesity. This may increase the risk of certain health issues. Focus on sustainable lifestyle changes."
                        }
                        bmiCategory = cat
                        categoryDescription = desc
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "CALCULATE BMI METRICS",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
            }

            if (bmiResult != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR BMI RESULT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.1f", bmiResult),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = bmiCategory.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = categoryDescription,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

// ==================== FASTING TAB/PANE ====================

data class BiologicalStage(
    val startHour: Float,
    val endHour: Float,
    val name: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingPane(
    activeFastingLog: FastingLog?,
    fastingHistory: List<FastingLog>,
    fastingDurationMillis: Long,
    selectedTargetHours: Int,
    onStartFasting: (Int, Long?) -> Unit,
    onEndClick: () -> Unit,
    onCancelFasting: () -> Unit,
    onDeleteLog: (FastingLog) -> Unit,
    onTargetSelected: (Int) -> Unit,
    onCustomFastLog: (Long, Long, String) -> Unit
) {
    var selectedStageForInfo by remember { mutableStateOf<BiologicalStage?>(null) }
    var showBackdateDialog by remember { mutableStateOf(false) }
    val presetTargets = listOf(16, 18, 23, 36, 48, 72)
    val biologicalStages = listOf(
        BiologicalStage(0f, 4f, "Feeding State", "Anabolic Absorption", Icons.Default.Restaurant, Color(0xFF10B981), "Your body is actively digesting and absorbing energy from food. Insulin rises, signaling cells to store nutrients."),
        BiologicalStage(4f, 12f, "Post-Absorptive", "Glycogen Breakdown", Icons.Default.HourglassEmpty, Color(0xFF3B82F6), "Blood glucose values drop. The liver starts releasing its stored glycogen reserves to keep energy levels balanced."),
        BiologicalStage(12f, 16f, "Gluconeogenesis", "Alternative Glucose Production", Icons.Default.Build, Color(0xFFF59E0B), "Glycogen depletion triggers the liver to synthesize new glucose mainly from lactate, amino acids, and glycerol."),
        BiologicalStage(16f, 24f, "Ketosis", "Fat Oxidation Peak", Icons.Default.LocalFireDepartment, Color(0xFFEF4444), "Fatty acids are metabolized into energy-rich ketones, which serve as a premium fuel source for your brain. Fat burn is elevated."),
        BiologicalStage(24f, 72f, "Autophagy", "Cellular Recycling Renewal", Icons.Default.Favorite, Color(0xFF8B5CF6), "The biological recycling system peaks. Cells actively scan, clean up, and rebuild damaged or old proteins and components.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fasting_pane_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Active Fast Card or Setting Tracker
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_timer_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (activeFastingLog != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeFastingLog != null) {
                        // Ongoing fast state
                        val targetMillis = activeFastingLog.targetHours * 3600_000L
                        val progress = if (targetMillis > 0) {
                            (fastingDurationMillis.coerceAtLeast(0L).toFloat() / targetMillis).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        val percentComplete = (progress * 100).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Fasting Window",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            val activeProtocolLabel = when (activeFastingLog.targetHours) {
                                16 -> "16:8 Protocol"
                                18 -> "18:6 Protocol"
                                20 -> "20:4 Protocol"
                                23 -> "OMAD"
                                36 -> "36h Fast"
                                48 -> "48h Fast"
                                72 -> "72h Fast"
                                else -> "${activeFastingLog.targetHours}h Fast"
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = activeProtocolLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Beautiful circular timer progress surrounded by biological stages!
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(270.dp) // larger container to anchor external icons beautifully!
                        ) {
                            // Pulsing/Atmospheric canvas background
                            val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.04f,
                                targetValue = 0.12f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "PulseAlpha"
                            )

                            // Ring Canvas at size 200.dp
                            Canvas(modifier = Modifier.size(200.dp)) {
                                val radiusScale = size.minDimension / 2f

                                // Draw pulsing glow inside
                                drawCircle(
                                    color = Color.White,
                                    radius = radiusScale * 0.85f,
                                    alpha = pulseAlpha
                                )

                                // Base static ring - elegant white alpha
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.3f),
                                    radius = radiusScale * 0.92f,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Progress sweep path - high contrast primary color
                                drawArc(
                                    color = Color(0xFF0061A4),
                                    startAngle = -90f,
                                    sweepAngle = progress * 360f,
                                    useCenter = false,
                                    topLeft = Offset(radiusScale * 0.08f, radiusScale * 0.08f),
                                    size = Size(radiusScale * 1.84f, radiusScale * 1.84f),
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Dynamic Text Indicator Inside
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatDuration(fastingDurationMillis),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        letterSpacing = (-1).sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Elapsed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        letterSpacing = 1.5.sp
                                    )
                                )
                            }

                            // Position Biological status stage icons beautifully around the timer ring!
                            val targetHoursFloat = activeFastingLog.targetHours.toFloat()
                            val elapsedHoursFloat = fastingDurationMillis.toFloat() / (1000f * 60f * 60f)
                            val applicableStages = biologicalStages.filter { it.startHour < targetHoursFloat }

                            applicableStages.forEach { stage ->
                                val stageProgress = if (targetHoursFloat > 0) (stage.startHour / targetHoursFloat).coerceIn(0f, 1f) else 0f
                                val angleRad = (stageProgress * 360f - 90f) * (Math.PI / 180f)
                                val radiusDp = 114.0 // Perfect radius around the 200.dp (100.dp radius) progress circle
                                val xOffset = (radiusDp * kotlin.math.cos(angleRad)).toFloat()
                                val yOffset = (radiusDp * kotlin.math.sin(angleRad)).toFloat()

                                val isActiveStage = elapsedHoursFloat >= stage.startHour && (elapsedHoursFloat < stage.endHour || stage.endHour >= targetHoursFloat)
                                val circleSize = if (isActiveStage) 36.dp else 28.dp
                                val iconSize = if (isActiveStage) 18.dp else 14.dp

                                Box(
                                    modifier = Modifier
                                        .offset(x = xOffset.dp, y = yOffset.dp)
                                        .size(circleSize)
                                        .clip(CircleShape)
                                        .background(if (isActiveStage) stage.color else Color.White)
                                        .border(2.dp, stage.color, CircleShape)
                                        .clickable { selectedStageForInfo = stage },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = stage.icon,
                                        contentDescription = stage.name,
                                        tint = if (isActiveStage) Color.White else stage.color,
                                        modifier = Modifier.size(iconSize)
                                    )
                                }
                            }
                        }

                        // Display a beautiful details card for the current active biological status!
                        val elapsedHoursFloat = fastingDurationMillis.toFloat() / (1000f * 60f * 60f)
                        val currentStage = biologicalStages.find { elapsedHoursFloat >= it.startHour && elapsedHoursFloat < it.endHour } ?: biologicalStages.last()

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.5f),
                                    RoundedCornerShape(24.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, currentStage.color.copy(alpha = 0.25f)),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(currentStage.color, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentStage.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentStage.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Stage Active",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = currentStage.color,
                                            letterSpacing = 0.5.sp
                                        ),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentStage.description,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Informative dates card - glass container style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.45f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "STARTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatEpochTime(activeFastingLog.startTime),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ENDS AT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatEpochTime(activeFastingLog.startTime + targetMillis),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Control Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelFasting,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("cancel_fast_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cancel", 
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                    softWrap = false
                                )
                            }

                            // Unified Custom Log button (When fast is active)
                            var showCustomLogDialog by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { showCustomLogDialog = true },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(54.dp)
                                    .testTag("custom_log_active_fast_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Log", 
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Visible,
                                    softWrap = false
                                )
                            }

                            if (showCustomLogDialog) {
                                CustomEndFastDialog(
                                    onDismiss = { showCustomLogDialog = false },
                                    onConfirm = { startTime, endTime, notes ->
                                        onCustomFastLog(startTime, endTime, notes)
                                        showCustomLogDialog = false
                                    }
                                )
                            }

                            Button(
                                onClick = onEndClick,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(54.dp)
                                    .testTag("end_fast_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0061A4),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Finish Fasting", 
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                    } else {
                        // User needs to start a fast
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHOOSE FASTING PLAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Target hours selectors in 2 rows with 3 boxes per row (all same size, vivid design)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Row 1: 16 (16:8), 18 (18:6), 23 (OMAD)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(16, 18, 23).forEach { hr ->
                                    PresetPlanBox(
                                        hr = hr,
                                        selected = selectedTargetHours == hr,
                                        onClick = { onTargetSelected(hr) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            // Row 2: 36 (36h), 48 (48h), 72 (72h)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(36, 48, 72).forEach { hr ->
                                    PresetPlanBox(
                                        hr = hr,
                                        selected = selectedTargetHours == hr,
                                        onClick = { onTargetSelected(hr) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Explanations about the selected fasting protocol
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(24.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                val textProtocol = when (selectedTargetHours) {
                                    16 -> "16:8 Protocol (Daily Fast) - The golden standard. Encourages consistent fat burning, cellular repair, and blood sugar balance."
                                    18 -> "18:6 Protocol (Advanced Fast) - Amplified metabolic benefits, improved fat adaptation, and deeper cellular regeneration."
                                    20 -> "20:4 Warrior Fast (Intense) - 4-hour eating window. Promotes human growth hormone production and enhanced cellular repair."
                                    23 -> "OMAD (One Meal A Day) - Promotes rapid lipolysis, intensive insulin reset, and high mental clarity throughout the day."
                                    36 -> "36-Hour Monk Fast - Deep Autophagy reset. Excellent for resetting digestion, immune regeneration, and resetting food relationships."
                                    48 -> "48-Hour Extended Fast - Complete cellular renovation. Promotes major body stem-cell production, deep ketosis, and inflammation reduction."
                                    72 -> "72-Hour Prolonged Fast - Ultimate cell rejuvenation. Fully resets the immune system and maximizes metabolic adaptation. Must be done with care."
                                    else -> "$selectedTargetHours-hour fasting routine."
                                }
                                val subtitleText = when (selectedTargetHours) {
                                    16 -> "Protocol: 16:8"
                                    18 -> "Protocol: 18:6"
                                    20 -> "Protocol: 20:4"
                                    23 -> "Protocol: OMAD (23:1)"
                                    36 -> "Protocol: 36 Hours Extended"
                                    48 -> "Protocol: 48 Hours Extended"
                                    72 -> "Protocol: 72 Hours Extended"
                                    else -> "Protocol: Customized Fast"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = subtitleText,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = textProtocol,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large beautiful Action Button
                        Button(
                            onClick = { onStartFasting(selectedTargetHours, null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_fast_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0061A4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "START FASTING NOW",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Unified Custom Log button
                        var showCustomLogDialog by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = { showCustomLogDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("custom_log_fast_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "LOG CUSTOM FAST",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (showCustomLogDialog) {
                            CustomEndFastDialog(
                                onDismiss = { showCustomLogDialog = false },
                                onConfirm = { startTime, endTime, notes ->
                                    onCustomFastLog(startTime, endTime, notes)
                                    showCustomLogDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // BMI Calculator Section
        item {
            BmiCalculatorSection()
        }

        // Summary Statistics Header
        item {
            Text(
                text = "Fasting History & Stats",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // REDESIGNED STATS: Beautiful full width / equal margin structured boxes!
        item {
            val totalFasts = fastingHistory.size
            val completedFasts = fastingHistory.count { it.isCompleted }
            val longestFastHours = fastingHistory.maxOfOrNull { it.durationMillis }?.let { it / 3600_000L } ?: 0L

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Fasts Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Total Fasted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalFasts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Goal Hit Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Goal Cleared", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$completedFasts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
                    }
                }

                // Longest Fast Box
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Longest Fast", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${longestFastHours}h", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Historical Records List
        if (fastingHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No fasts completed yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(fastingHistory) { log ->
                val durationHrsValue = log.durationMillis / 3600_000f
                val formattedDuration = String.format("%.1fh", durationHrsValue)
                val targetText = "${log.targetHours}h Goal"
                val isCompleted = log.isCompleted
                val accentColor = if (isCompleted) Color(0xFF10B981) else Color(0xFFF59E0B)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fasting_log_item_${log.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thick Left Accent Indicator Strip
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(6.dp)
                                .background(accentColor)
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formattedDuration,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(targetText) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = accentColor
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ended: ${formatEpochTime(log.endTime ?: 0)} on ${formatEpochDate(log.endTime ?: 0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (log.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "\"${log.notes}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteLog(log) },
                                modifier = Modifier.testTag("delete_fasting_log_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Fasting Record",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedStageForInfo != null) {
        val stage = selectedStageForInfo!!
        AlertDialog(
            onDismissRequest = { selectedStageForInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = stage.icon,
                        contentDescription = null,
                        tint = stage.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stage.name, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = stage.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = stage.color)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stage.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStageForInfo = null }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showBackdateDialog) {
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val timePickerState = rememberTimePickerState(
            initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
            is24Hour = false
        )

        var showError by remember { mutableStateOf(false) }

        // Formatted display values
        val selectedDateStr = datePickerState.selectedDateMillis?.let {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Select Date"

        val selectedTimeStr = String.format("%02d:%02d %s",
            if (timePickerState.hour % 12 == 0) 12 else timePickerState.hour % 12,
            timePickerState.minute,
            if (timePickerState.hour >= 12) "PM" else "AM"
        )

        AlertDialog(
            onDismissRequest = { showBackdateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Custom Start", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "If you forgot to start fasting in the app when you actually started, specify your exact manual start time below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Date selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "STARTING DAY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(selectedDateStr)
                        }
                    }

                    // Time selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "STARTING TIME",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(selectedTimeStr)
                        }
                    }

                    if (showError) {
                        Text(
                            text = "Start time cannot be in the future!",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val calendar = java.util.Calendar.getInstance()
                        calendar.timeInMillis = selectedDate
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                        calendar.set(java.util.Calendar.SECOND, 0)
                        calendar.set(java.util.Calendar.MILLISECOND, 0)

                        val selectedEpoch = calendar.timeInMillis
                        if (selectedEpoch > System.currentTimeMillis()) {
                            showError = true
                        } else {
                            showError = false
                            onStartFasting(selectedTargetHours, selectedEpoch)
                            showBackdateDialog = false
                        }
                    }
                ) {
                    Text("Start Fast", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackdateDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            Dialog(onDismissRequest = { showTimePicker = false }) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimePicker(state = timePickerState)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showTimePicker = false }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomEndFastDialog(
    onDismiss: () -> Unit,
    onConfirm: (startTime: Long, endTime: Long, notes: String) -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis() - 86400000L)
    val startTimePickerState = rememberTimePickerState(initialHour = 20, initialMinute = 0)
    
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val endTimePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0)

    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Custom Fast", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Specify the exact start and end period for your fast.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Start
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("START TIME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(startDatePickerState.selectedDateMillis ?: 0)))
                        }
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", startTimePickerState.hour, startTimePickerState.minute))
                        }
                    }
                }

                // End
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("END TIME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(endDatePickerState.selectedDateMillis ?: 0)))
                        }
                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(String.format("%02d:%02d", endTimePickerState.hour, endTimePickerState.minute))
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    
                    cal.timeInMillis = startDatePickerState.selectedDateMillis ?: 0
                    cal.set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                    cal.set(Calendar.MINUTE, startTimePickerState.minute)
                    val start = cal.timeInMillis

                    cal.timeInMillis = endDatePickerState.selectedDateMillis ?: 0
                    cal.set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                    cal.set(Calendar.MINUTE, endTimePickerState.minute)
                    val end = cal.timeInMillis

                    if (end <= start) {
                        error = "End time must be after start time"
                    } else if (end > System.currentTimeMillis()) {
                        error = "End time cannot be in the future"
                    } else {
                        onConfirm(start, end, notes)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Fast Record")
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel") 
            } 
        }
    )

    if (showStartDatePicker) {
        DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("OK") } }) {
            DatePicker(state = startDatePickerState)
        }
    }
    if (showEndDatePicker) {
        DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("OK") } }) {
            DatePicker(state = endDatePickerState)
        }
    }
    if (showStartTimePicker || showEndTimePicker) {
        val state = if (showStartTimePicker) startTimePickerState else endTimePickerState
        Dialog(onDismissRequest = { showStartTimePicker = false; showEndTimePicker = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = state)
                    TextButton(onClick = { showStartTimePicker = false; showEndTimePicker = false }, modifier = Modifier.align(Alignment.End)) { Text("OK") }
                }
            }
        }
    }
}

// ==================== HYDRATION TAB/PANE ====================

@Composable
fun WaterPane(
    waterGoalMl: Int,
    todayWaterLogs: List<WaterLog>,
    todayWaterTotal: Int,
    onAddWater: (Int) -> Unit,
    onDeleteWaterLog: (WaterLog) -> Unit,
    onEditGoalClick: () -> Unit,
    onAddCustomClick: () -> Unit
) {
    val progress = if (waterGoalMl > 0) {
        (todayWaterTotal.toFloat() / waterGoalMl).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percentFraction = (progress * 100).toInt()
    val isGoalReached = todayWaterTotal >= waterGoalMl && waterGoalMl > 0

    // Particle state for celebration
    val infiniteTransition = rememberInfiniteTransition(label = "Celebration")
    val celebrationAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CelebrationAlpha"
    )
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CelebrationScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("water_pane_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hydration Progress Card with Wave Canvas Drawing
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_water_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Hydration",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        TextButton(
                            onClick = onEditGoalClick,
                            modifier = Modifier.testTag("water_edit_goal_button"),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Goal: ${waterGoalMl}ml", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean stats indicator row matching design: e.g. "1,450 / 2,500 ml"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$todayWaterTotal ",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "/ $waterGoalMl ml",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 8-segmented Glass Indicators Row as requested in raw HTML spec!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val totalSegments = 8
                        for (i in 0 until totalSegments) {
                            // Find percentage threshold for each segment
                            val segmentMinPercent = i.toFloat() / totalSegments
                            val isActiveNext = progress >= segmentMinPercent && progress < (i + 1).toFloat() / totalSegments
                            val isFilled = progress >= (i + 1).toFloat() / totalSegments

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isFilled -> Color(0xFFD1E4FF)
                                            isActiveNext -> Color(0xFFE0E2ED)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        }
                                    )
                                    .then(
                                        if (isActiveNext) {
                                            Modifier.border(
                                                BorderStroke(2.dp, Color(0xFF0061A4).copy(alpha = 0.4f)),
                                                RoundedCornerShape(8.dp)
                                            )
                                        } else Modifier
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Draw dynamic customized Water cup filled progress on Canvas!
                    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
                    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Drawing custom water glass cup with fill levels
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw glass outline coordinates
                            val glassPath = Path().apply {
                                moveTo(w * 0.28f, h * 0.12f) // top left
                                lineTo(w * 0.34f, h * 0.88f) // bottom left
                                lineTo(w * 0.66f, h * 0.88f) // bottom right
                                lineTo(w * 0.72f, h * 0.12f) // top right
                            }

                            // Draw liquid wave background clipping path
                            val liquidLevelY = h * 0.12f + (h * 0.76f) * (1f - progress)

                            // Glass backdrop background
                            drawPath(
                                path = glassPath,
                                color = primaryContainerColor.copy(alpha = 0.12f)
                            )

                            // Glass fill water color
                            val waterColor = Color(0xFF0EA5E9)

                            // If we have some logged water, fill the glass accordingly
                            if (progress > 0f) {
                                drawContext.canvas.save()
                                // Clip anything outside the glass container
                                drawContext.canvas.clipPath(glassPath)

                                // Draw filled water rectangle inside clipped glass boundary
                                drawRect(
                                    color = waterColor.copy(alpha = 0.85f),
                                    topLeft = Offset(0f, liquidLevelY),
                                    size = Size(w, h - liquidLevelY)
                                )

                                drawContext.canvas.restore()
                            }

                            // Draw transparent Glass stroke borders around
                            drawPath(
                                path = glassPath,
                                color = onSurfaceVariantColor.copy(alpha = 0.2f),
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // Hydro Stats overlay in center
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = isGoalReached,
                                transitionSpec = {
                                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                                },
                                label = "GoalTextTransition"
                            ) { reached ->
                                if (reached) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "GOAL!",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                letterSpacing = 2.sp
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "$percentFraction%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0EA5E9),
                                            letterSpacing = (-0.5).sp
                                        )
                                    )
                                }
                            }
                        }

                        // Celebration Particles
                        if (isGoalReached) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val particleCount = 8
                                for (i in 0 until particleCount) {
                                    val angle = (i * (360f / particleCount)) * (Math.PI / 180f)
                                    val distance = 80.dp.toPx() * celebrationScale
                                    val x = center.x + (kotlin.math.cos(angle) * distance).toFloat()
                                    val y = center.y + (kotlin.math.sin(angle) * distance).toFloat()
                                    
                                    drawCircle(
                                        color = Color(0xFF10B981).copy(alpha = 1f - celebrationAlpha),
                                        radius = 4.dp.toPx() * (1f - celebrationAlpha),
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons to quickly increment logged water
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(250, 500, 750).forEach { amt ->
                            Button(
                                onClick = { onAddWater(amt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("water_add_${amt}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${amt}ml",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Custom trigger
                        Button(
                            onClick = onAddCustomClick,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("water_add_custom"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0061A4),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Custom",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Logs header with Stats info
        item {
            Text(
                text = "Today's Logs History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (todayWaterLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Drink some water to begin logging!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(todayWaterLogs) { log ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("water_log_item_${log.id}"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0EA5E9).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle, 
                                    contentDescription = null, 
                                    tint = Color(0xFF0EA5E9),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${log.amountMl} ml logged",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Logged at ${formatEpochTime(log.timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteWaterLog(log) },
                            modifier = Modifier.testTag("delete_water_log_${log.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Water Intake",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helpers for Datetime formatting
fun formatEpochTime(millis: Long): String {
    if (millis <= 0L) return "N/A"
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun formatEpochDate(millis: Long): String {
    if (millis <= 0L) return "N/A"
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun formatDuration(millis: Long): String {
    val totalSeconds = Math.max(0, millis / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * Vivid selector box for fasting presets
 */
@Composable
fun PresetPlanBox(
    hr: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val planName = when (hr) {
        16 -> "16:8"
        18 -> "18:6"
        20 -> "20:4"
        23 -> "OMAD"
        36 -> "36h"
        48 -> "48h"
        72 -> "72h"
        else -> "${hr}h"
    }

    val vividColor = when (hr) {
        16 -> Color(0xFF6366F1) // Indigo
        18 -> Color(0xFF0D9488) // Teal
        20 -> Color(0xFFD97706) // Amber
        23 -> Color(0xFFDB2777) // Pink
        36 -> Color(0xFFEA580C) // Orange
        48 -> Color(0xFF7C3AED) // Purple
        72 -> Color(0xFFDC2626) // Red
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) vividColor else vividColor.copy(alpha = 0.12f))
            .border(
                width = if (selected) 2.5.dp else 1.5.dp,
                color = if (selected) Color.White.copy(alpha = 0.8f) else vividColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = planName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = if (selected) Color.White else vividColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Highly polished multi-page Onboarding & Demographics setup flow
 */
@Composable
fun OnboardingScreen(
    onCompleted: (age: Int, gender: String, weight: Float, height: Float, activityLevel: String, userUnitMetric: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    // User response state
    var ageStr by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Female") }
    var weightStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Lightly Active") }
    var useMetric by remember { mutableStateOf(true) }
    
    // Validation error message helper
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC) // Beautiful very clean background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header & Step indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF6366F1), // Indigo
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VITALITY FLOW",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color(0xFF1E293B)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Beautiful linear step bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..4) {
                        val active = i <= step
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) Color(0xFF6366F1) else Color(0xFFE2E8F0))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Step $step of 4",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                )
            }

            // Main Slide body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    1 -> OnboardingSlideIntro()
                    2 -> OnboardingSlideDemographics(
                        ageStr = ageStr,
                        onAgeChange = {
                            ageStr = it
                            errorMessage = null
                        },
                        selectedGender = gender,
                        onGenderChange = { gender = it }
                    )
                    3 -> OnboardingSlideMetrics(
                        weightStr = weightStr,
                        onWeightChange = {
                            weightStr = it
                            errorMessage = null
                        },
                        heightStr = heightStr,
                        onHeightChange = {
                            heightStr = it
                            errorMessage = null
                        },
                        useMetric = useMetric,
                        onUnitChange = { nextMetric ->
                            // Optional: convert current values when toggling? 
                            // Usually better to just let them re-input or do simple math
                            val w = weightStr.toFloatOrNull()
                            val h = heightStr.toFloatOrNull()
                            if (w != null) {
                                weightStr = if (nextMetric) String.format("%.1f", w / 2.20462f) else String.format("%.1f", w * 2.20462f)
                            }
                            if (h != null) {
                                heightStr = if (nextMetric) String.format("%.1f", h * 2.54f) else String.format("%.1f", h / 2.54f)
                            }
                            useMetric = nextMetric
                        }
                    )
                    4 -> OnboardingSlideLifestyle(
                        selectedActivity = activityLevel,
                        onActivityChange = { activityLevel = it }
                    )
                }
            }

            // Error Message Display
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // Bottom navigation action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = {
                            step--
                            errorMessage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "Back",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Button(
                    onClick = {
                        // Handle validation
                        when (step) {
                            1 -> {
                                step = 2
                            }
                            2 -> {
                                val age = ageStr.toIntOrNull()
                                if (age == null || age <= 0 || age > 120) {
                                    errorMessage = "Please enter a valid age (1 - 120)"
                                } else {
                                    step = 3
                                }
                            }
                            3 -> {
                                val weight = weightStr.toFloatOrNull()
                                val height = heightStr.toFloatOrNull()
                                if (useMetric) {
                                    if (weight == null || weight <= 10f || weight > 500f) {
                                        errorMessage = "Please enter a valid weight in kg"
                                    } else if (height == null || height <= 40f || height > 280f) {
                                        errorMessage = "Please enter a valid height in cm"
                                    } else {
                                        step = 4
                                    }
                                } else {
                                    if (weight == null || weight <= 22f || weight > 1100f) {
                                        errorMessage = "Please enter a valid weight in lbs"
                                    } else if (height == null || height <= 15f || height > 110f) {
                                        errorMessage = "Please enter a valid height in inches"
                                    } else {
                                        step = 4
                                    }
                                }
                            }
                            4 -> {
                                val age = ageStr.toIntOrNull() ?: 25
                                val weight = weightStr.toFloatOrNull() ?: 65f
                                val height = heightStr.toFloatOrNull() ?: 170f
                                onCompleted(age, gender, weight, height, activityLevel, useMetric)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)), // Electric Indigo
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (step == 4) "Complete Setup" else "Continue",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 1: Intermittent Fasting benefits & scientific summary
 */
@Composable
fun OnboardingSlideIntro() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Embark on Your Well-being Journey",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            ),
            color = Color(0xFF0F172A)
        )
        
        Text(
            text = "Unlock the deep biological and metabolic strength of scheduled intermittent fasting paired with precise hydration habits.",
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            ),
            color = Color(0xFF475569)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Highlights list cards
        listOf(
            Triple(Icons.Default.Refresh, "Deep Cellular Autophagy", "Fasting naturally triggers our bodies to break down and recycle waste within old, damaged cells."),
            Triple(Icons.Default.Star, "Metabolic Flexibility", "Learn to safely utilize body fat stores for energy, helping balance cholesterol and insulin levels."),
            Triple(Icons.Default.Done, "Mental Sharpness & Focus", "Increases BDNF development to elevate focus, cognitive speed, and neural resilience throughout the day.")
        ).forEach { (icon, title, desc) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0E7FF), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 2: Demographics - Age & Gender
 */
@Composable
fun OnboardingSlideDemographics(
    ageStr: String,
    onAgeChange: (String) -> Unit,
    selectedGender: String,
    onGenderChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Personal Profile",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "We use these details to help calibrate your target metabolic indicators.",
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Age input field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "HOW OLD ARE YOU?",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = ageStr,
                onValueChange = onAgeChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., 28") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )
        }

        // Gender row select
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "WHAT IS YOUR GENDER BIOLOGY?",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Female", "Male").forEach { g ->
                    val active = selectedGender == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Color(0xFF6366F1) else Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (active) Color(0xFF6366F1) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onGenderChange(g) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = g,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 3: Body metrics - Weight (kg) & Height (cm)
 */
@Composable
fun OnboardingSlideMetrics(
    weightStr: String,
    onWeightChange: (String) -> Unit,
    heightStr: String,
    onHeightChange: (String) -> Unit,
    useMetric: Boolean,
    onUnitChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your Metrics",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "These values help determine physical water replacement estimates.",
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                color = Color(0xFF64748B)
            )
        }

        // Unit System Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onUnitChange(true) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (useMetric) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Metric (kg/cm)", color = if (useMetric) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onUnitChange(false) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!useMetric) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Imperial (lb/in)", color = if (!useMetric) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weight
        Column(modifier = Modifier.fillMaxWidth()) {
            val weightLabel = if (useMetric) "WEIGHT (KG)" else "WEIGHT (LBS)"
            Text(
                text = weightLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = weightStr,
                onValueChange = onWeightChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (useMetric) "e.g., 72.5" else "e.g., 160") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )
        }

        // Height
        Column(modifier = Modifier.fillMaxWidth()) {
            val heightLabel = if (useMetric) "HEIGHT (CM)" else "HEIGHT (IN)"
            Text(
                text = heightLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = heightStr,
                onValueChange = onHeightChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (useMetric) "e.g., 176" else "e.g., 69") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}

/**
 * Slide 4: Lifestyle & Activity Level
 */
@Composable
fun OnboardingSlideLifestyle(
    selectedActivity: String,
    onActivityChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Activity Level",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Identify the statement describing your normal daily routine.",
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Beautiful list of activity cards
        listOf(
            "Sedentary" to "Desk jobs, minimal walks or structured exercise program.",
            "Lightly Active" to "Active around home, light exercise 1-3 times a week.",
            "Moderately Active" to "Active job, moderate workouts/sports 3-5 days.",
            "Very Active" to "Heavy workouts, physical profession, daily athletic load."
        ).forEach { (title, subtitle) ->
            val active = selectedActivity == title
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (active) Color(0xFFE0E7FF) else Color.White
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (active) Color(0xFF6366F1) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActivityChange(title) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = active,
                        onClick = { onActivityChange(title) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6366F1))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Beautiful launching screen displaying the application logo (AXIS PHYSIQUE) on a crisp white background
 */
@Composable
fun SplashWelcomeScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "Splash Alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.02f else 0.85f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "Splash Scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0061A4)), // Primary Blue background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(
                alpha = alphaAnim,
                scaleX = scaleAnim,
                scaleY = scaleAnim
            )
        ) {
            // High-resolution clock logo from Play Store asset
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_playstore),
                contentDescription = "AXIS PHYSIQUE",
                modifier = Modifier.size(200.dp), // Sharper size
                contentScale = ContentScale.Fit
            )
            
            // Adjusted space to prevent overlap
            Spacer(modifier = Modifier.height(12.dp))
            
            // Animate text slide-up slightly
            val textSlideAnim by animateFloatAsState(
                targetValue = if (startAnimation) 0f else 10f,
                animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = FastOutSlowInEasing),
                label = "TextSlide"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = textSlideAnim.dp) // Removed negative offset to fix mixing/overlap
            ) {
                Text(
                    text = "AXIS PHYSIQUE",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 26.sp,
                        letterSpacing = 2.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Character-by-character reveal from left to right
                val subtitle = "Intermittent Fasting"
                Row {
                    subtitle.forEachIndexed { index, char ->
                        val charAlpha by animateFloatAsState(
                            targetValue = if (startAnimation) 0.9f else 0f,
                            animationSpec = tween(
                                durationMillis = 600,
                                delayMillis = 400 + (index * 40),
                                easing = LinearOutSlowInEasing
                            ),
                            label = "CharAlpha$index"
                        )
                        
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = charAlpha),
                                fontSize = 16.sp,
                                letterSpacing = if (char == ' ') 4.sp else 0.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ==================== ANALYTICS TAB/PANE ====================

@Composable
fun AnalyticsPane(
    fastingHistory: List<FastingLog>,
    waterHistory: List<WaterLog>
) {
    var selectedStatsPeriod by remember { mutableStateOf(0) } // Fasting: 0 = Weekly, 1 = Monthly, 2 = Yearly
    var selectedWaterStatsPeriod by remember { mutableStateOf(0) } // Water: 0 = Weekly, 1 = Monthly, 2 = Yearly
    val currentTimeMillis = System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Metabolic Insights",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = "Unlock your fat digestion and clean cellular autophagic scores.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Period Tab Bar for Fasting
        TabRow(
            selectedTabIndex = selectedStatsPeriod,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedStatsPeriod]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedStatsPeriod == 0,
                onClick = { selectedStatsPeriod = 0 },
                text = { Text("Weekly", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedStatsPeriod == 1,
                onClick = { selectedStatsPeriod = 1 },
                text = { Text("Monthly", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedStatsPeriod == 2,
                onClick = { selectedStatsPeriod = 2 },
                text = { Text("Yearly", fontWeight = FontWeight.Bold) }
            )
        }

        if (fastingHistory.isEmpty()) {
            // Elegant empty state for Fasting
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Fasting Analytics Yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Complete your first fasting session to unlock metabolic insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Filtering of statistics based on period
            val filteredLogs = when (selectedStatsPeriod) {
                0 -> { // Weekly (last 7 days)
                    val startOfWeek = currentTimeMillis - (7L * 24 * 60 * 60 * 1000)
                    fastingHistory.filter { (it.endTime ?: 0L) >= startOfWeek }
                }
                1 -> { // Monthly (last 30 days)
                    val startOfMonth = currentTimeMillis - (30L * 24 * 60 * 60 * 1000)
                    fastingHistory.filter { (it.endTime ?: 0L) >= startOfMonth }
                }
                else -> { // Yearly (last 365 days)
                    val startOfYear = currentTimeMillis - (365L * 24 * 60 * 60 * 1000)
                    fastingHistory.filter { (it.endTime ?: 0L) >= startOfYear }
                }
            }

            val totalFasts = filteredLogs.size
            val successfulFasts = filteredLogs.count { it.isCompleted }
            val averageDurationHours = if (totalFasts > 0) {
                filteredLogs.map { it.durationMillis }.sum().toFloat() / (totalFasts * 3600_000f)
            } else 0f
            val maxSuccessPercent = if (totalFasts > 0) (successfulFasts.toFloat() / totalFasts * 100f).toInt() else 0

            // Insights Dashboard
            Text(
                text = "Metabolic Activity",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Goal Success", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$maxSuccessPercent%", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Average Period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format("%.1fh", averageDurationHours), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Beautiful custom progress-bar styled chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Duration Distribution",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredLogs.isEmpty()) {
                        Text(
                            "No logged records in this selected period.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        // Render a premium bar list representing up to 5 peak records in selected timeframe
                        val displayLogs = filteredLogs.take(5)
                        displayLogs.forEachIndexed { index, log ->
                            val durationHrs = log.durationMillis / 3600_000f
                            val maxLimit = maxOf(24f, displayLogs.maxOf { it.durationMillis / 3600_000f })
                            val fillRatio = if (maxLimit > 0) (durationHrs / maxLimit).coerceIn(0.1f, 1f) else 1f
                            val formattedDateLabel = formatEpochDate(log.endTime ?: log.startTime)

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(formattedDateLabel, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(String.format("%.1fh fast", durationHrs), style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fillRatio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (log.isCompleted) Color(0xFF10B981) else Color(0xFFFBBF24)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cellular Renewal & Ketosis insights callout block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Biology Scoring Matrix", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Ketosis Hours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ketosis (Fat Burning) Zone Reached: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val ketosisCount = filteredLogs.count { (it.durationMillis / 3600_000f) >= 16f }
                        Text("$ketosisCount times", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Autophagy Hours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Autophagy (Cellular Clean up) Triggered: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val autophagyCount = filteredLogs.count { (it.durationMillis / 3600_000f) >= 24f }
                        Text("$autophagyCount times", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        // --- HYDRATION STATS SECTION ---
        var selectedMonthOffset by remember { mutableStateOf(0) }
        
        val filteredWaterLogs = when (selectedWaterStatsPeriod) {
            0 -> {
                val startOfWeek = currentTimeMillis - (7L * 24 * 60 * 60 * 1000)
                waterHistory.filter { it.timestamp >= startOfWeek }
            }
            1 -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, selectedMonthOffset)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfMonth = cal.timeInMillis
                
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val endOfMonth = cal.timeInMillis
                
                waterHistory.filter { it.timestamp in startOfMonth..endOfMonth }
            }
            else -> {
                val startOfYear = currentTimeMillis - (365L * 24 * 60 * 60 * 1000)
                waterHistory.filter { it.timestamp >= startOfYear }
            }
        }

        // Group by day to calculate daily stats for the filtered period
        val filteredWaterByDay = filteredWaterLogs.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { entry -> entry.value.sumOf { it.amountMl } }

        // Prepare aggregation for the charts (all history)
        val allDailyTotals = waterHistory.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { it.value.sumOf { log -> log.amountMl } }

        val totalWaterLogged = filteredWaterLogs.sumOf { it.amountMl }
        val activeDaysCount = filteredWaterByDay.size.coerceAtLeast(1)
        val averageWaterDaily = totalWaterLogged.toFloat() / activeDaysCount

        Text(
            text = "Hydration Analysis",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        // Separate Period Tab Bar for Water
        TabRow(
            selectedTabIndex = selectedWaterStatsPeriod,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedWaterStatsPeriod]),
                    color = Color(0xFF0EA5E9)
                )
            }
        ) {
            Tab(
                selected = selectedWaterStatsPeriod == 0,
                onClick = { selectedWaterStatsPeriod = 0 },
                text = { Text("Weekly", fontWeight = FontWeight.Bold, color = if(selectedWaterStatsPeriod == 0) Color(0xFF0EA5E9) else Color.Unspecified) }
            )
            Tab(
                selected = selectedWaterStatsPeriod == 1,
                onClick = { selectedWaterStatsPeriod = 1 },
                text = { Text("Monthly", fontWeight = FontWeight.Bold, color = if(selectedWaterStatsPeriod == 1) Color(0xFF0EA5E9) else Color.Unspecified) }
            )
            Tab(
                selected = selectedWaterStatsPeriod == 2,
                onClick = { selectedWaterStatsPeriod = 2 },
                text = { Text("Yearly", fontWeight = FontWeight.Bold, color = if(selectedWaterStatsPeriod == 2) Color(0xFF0EA5E9) else Color.Unspecified) }
            )
        }

        // Month Picker (Only for Monthly view)
        if (selectedWaterStatsPeriod == 1) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, selectedMonthOffset)
            val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedMonthOffset-- }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { selectedMonthOffset++ }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Daily Average", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${averageWaterDaily.toInt()}ml", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0EA5E9))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Volume", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${totalWaterLogged}ml", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedWaterStatsPeriod == 0) "Weekly Intake History" else if (selectedWaterStatsPeriod == 1) "Monthly Intake History" else "Yearly Intake History",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val bars = when (selectedWaterStatsPeriod) {
                    0 -> { // Weekly: show 7 days centered on today
                        (-3..3).map { i ->
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, i)
                            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                            val dailyTotal = allDailyTotals[cal.timeInMillis] ?: 0
                            Triple(
                                SimpleDateFormat("dd", Locale.getDefault()).format(cal.time),
                                dailyTotal.toFloat(),
                                dailyTotal > 0
                            )
                        }
                    }
                    1 -> { // Monthly: show 12 months centered on today
                        (-5..6).map { i ->
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.MONTH, i)
                            val currentCal = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
                            val startM = currentCal.apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                            val endM = currentCal.apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
                            
                            val monthDailyTotals = allDailyTotals.filter { it.key in startM..endM }
                            val isLogged = monthDailyTotals.isNotEmpty()
                            val average = if (isLogged) monthDailyTotals.values.average().toFloat() else 0f
                            
                            val label = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
                            Triple(label, average, isLogged)
                        }
                    }
                    else -> { // Yearly: show 5 years centered on today
                        (-2..2).map { i ->
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.YEAR, i)
                            val currentCal = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis }
                            val startY = currentCal.apply { set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                            val endY = currentCal.apply { set(Calendar.MONTH, 11); set(Calendar.DAY_OF_MONTH, 31); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
                            
                            val yearDailyTotals = allDailyTotals.filter { it.key in startY..endY }
                            val isLogged = yearDailyTotals.isNotEmpty()
                            val average = if (isLogged) yearDailyTotals.values.average().toFloat() else 0f
                            
                            val label = cal.get(Calendar.YEAR).toString()
                            Triple(label, average, isLogged)
                        }
                    }
                }

                if (bars.all { it.second == 0f } && selectedWaterStatsPeriod != 0) {
                    Text(
                        "No water logs in this selected period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    val maxValue = maxOf(2000f, bars.maxOf { it.second })
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        bars.forEach { (label, value, isLogged) ->
                            val fillRatio = (value / maxValue).coerceIn(0f, 1f)
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(42.dp)
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = if (value > 0) value.toInt().toString() else "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isLogged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Bar Container
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .weight(1f),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (fillRatio > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fillRatio)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(if (isLogged) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        )
                                    } else {
                                        // Minimum visible line for empty bars
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
