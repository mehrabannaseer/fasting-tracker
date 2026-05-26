package com.axisphysique.axisfasting.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axisphysique.axisfasting.R
import com.axisphysique.axisfasting.model.FastingPlan
import com.axisphysique.axisfasting.ui.screens.timer.TimerViewModel
import com.axisphysique.axisfasting.ui.theme.ThemeViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToTips: () -> Unit,
    onNavigateToWeight: () -> Unit,
    onNavigateToWater: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    timerViewModel: TimerViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val timerUiState by timerViewModel.uiState.collectAsState()
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val animatedProgress by animateFloatAsState(targetValue = timerUiState.progress, label = "TimerProgress")
    
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomTimePickerDialog(
            onDismiss = { showDatePicker = false },
            onTimeSelected = { dateTime ->
                timerViewModel.startTimerWithCustomTime(dateTime)
                showDatePicker = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom Header
        Surface(
            color = if (isDarkMode) Color(0xFF111111) else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .height(80.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = if (isDarkMode) R.drawable.app_logo else R.drawable.app_logo_black),
                        contentDescription = "Axis Fasting Logo",
                        modifier = Modifier.height(44.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Intermittent Fasting",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard("Fasting Tips", Icons.Default.Lightbulb, Color(0xFFFFC107), onNavigateToTips, Modifier.weight(1f))
                    QuickActionCard("Weight Tracking", Icons.Default.MonitorWeight, Color(0xFF4CAF50), onNavigateToWeight, Modifier.weight(1f))
                    QuickActionCard("Water Intake", Icons.Default.LocalDrink, Color(0xFF2196F3), onNavigateToWater, Modifier.weight(1f))
                }
            }

            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Current Streak",
                        value = homeUiState.currentStreak,
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Fasts",
                        value = homeUiState.totalFasts,
                        icon = Icons.Default.History,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Timer Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!timerUiState.isRunning) {
                        Text(
                            text = "Choose Your Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val plans = FastingPlan.entries
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            plans.chunked(4).forEach { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunk.forEach { plan ->
                                        Surface(
                                            onClick = { timerViewModel.selectPlan(plan) },
                                            color = if (timerUiState.selectedPlan == plan) Color(plan.colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (timerUiState.selectedPlan == plan) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(plan.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                    if (chunk.size < 4) {
                                        Spacer(modifier = Modifier.weight((4 - chunk.size).toFloat()))
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Goal: ${timerUiState.selectedPlan.displayName} Fast",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(timerUiState.selectedPlan.colorHex),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(300.dp)
                    ) {
                        MetabolicStagesBackground(currentProgress = timerUiState.progress, isDarkMode = isDarkMode)
                        
                        CircularProgressIndicator(
                            progress = 1f,
                            modifier = Modifier.size(240.dp),
                            color = if (timerUiState.isRunning) Color(timerUiState.selectedPlan.colorHex).copy(alpha = 0.2f) else Color(0xFFE1F5FE),
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round
                        )
                        CircularProgressIndicator(
                            progress = animatedProgress,
                            modifier = Modifier.size(240.dp),
                            color = if (timerUiState.isRunning) Color(timerUiState.selectedPlan.colorHex) else Color(0xFF03A9F4),
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round
                        )
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (timerUiState.isRunning) "ELAPSED" else "READY",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = timerUiState.formattedTime,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (timerUiState.isRunning) {
                                Text(
                                    text = "${(timerUiState.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(timerUiState.selectedPlan.colorHex),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Metabolic Stage Description
            if (timerUiState.isRunning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = timerUiState.metabolicStage,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = timerUiState.stageDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!timerUiState.isRunning) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START FROM...")
                        }
                    }
                    
                    Button(
                        onClick = { timerViewModel.toggleTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerUiState.isRunning) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (timerUiState.isRunning) "END FAST" else "START FASTING",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Recent Fasts",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(homeUiState.recentFasts) { fast ->
                ListItem(
                    headlineContent = { Text(fast.type) },
                    supportingContent = { Text(fast.date) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fast.duration, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { homeViewModel.deleteFast(fast.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Default.Timer, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetabolicStagesBackground(currentProgress: Float, isDarkMode: Boolean = false) {
    val stageIcons = listOf(
        Icons.Default.RestaurantMenu,      // Feeding
        Icons.Default.AccessTime,          // Post-absorptive
        Icons.Default.LocalFireDepartment, // Fat Burning
        Icons.Default.FlashOn,             // Ketosis
        Icons.Default.AutoFixHigh           // Autophagy
    )
    val stageColors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFFE91E63)  // Pink
    )

    Canvas(modifier = Modifier.size(300.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 20.dp.toPx()
        
        // Draw colorful arc background - Light Blue
        drawCircle(
            color = Color(0xFFE1F5FE).copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        val stages = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        stages.forEachIndexed { index, progress ->
            val angle = (progress * 360f - 90f) * (Math.PI / 180f)
            val startX = center.x + (radius - 12.dp.toPx()) * cos(angle).toFloat()
            val startY = center.y + (radius - 12.dp.toPx()) * sin(angle).toFloat()
            val endX = center.x + (radius + 8.dp.toPx()) * cos(angle).toFloat()
            val endY = center.y + (radius + 8.dp.toPx()) * sin(angle).toFloat()
            
            drawLine(
                color = if (currentProgress >= progress) stageColors[index] else Color(0xFFE0E0E0),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    
    Box(modifier = Modifier.size(300.dp)) {
        val stages = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        stages.forEachIndexed { index, progress ->
            val angle = (progress * 360f - 90f)
            val radius = 150.dp - 32.dp
            
            val angleRad = angle * (Math.PI / 180f)
            val offsetX = (radius.value * cos(angleRad)).dp
            val offsetY = (radius.value * sin(angleRad)).dp

            Surface(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center)
                    .offset(x = offsetX, y = offsetY),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (currentProgress >= progress) stageColors[index] else (if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = stageIcons.getOrElse(index) { Icons.Default.Circle },
                        contentDescription = null,
                        tint = if (currentProgress >= progress) Color.White else (if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (java.time.LocalDateTime) -> Unit
) {
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    
    if (selectedDate == null) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = dateState.selectedDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    } ?: java.time.LocalDate.now()
                }) { Text("NEXT") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = java.time.LocalDateTime.now().hour,
            initialMinute = java.time.LocalDateTime.now().minute
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    val dateTime = selectedDate!!.atTime(timeState.hour, timeState.minute)
                    onTimeSelected(dateTime)
                }) { Text("START") }
            },
            dismissButton = {
                TextButton(onClick = { selectedDate = null }) { Text("BACK") }
            },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}
