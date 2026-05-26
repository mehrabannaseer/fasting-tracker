package com.axisphysique.axisfasting.ui.screens.tips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FastingTip(
    val title: String,
    val description: String,
    val icon: ImageVector = Icons.Default.Lightbulb
)

val tips = listOf(
    FastingTip("Stay Hydrated", "Drink plenty of water, sparkling water, or herbal teas. Hydration helps manage hunger pangs."),
    FastingTip("Ride the Waves", "Hunger often comes in waves and usually passes after 20-30 minutes. Stay busy during these times."),
    FastingTip("Salt is Your Friend", "If you feel dizzy or have a headache, a pinch of high-quality sea salt in water can help replenish electrolytes."),
    FastingTip("Break Fast Gently", "Start with a small, nutritious meal. Avoid high-carb or high-sugar foods immediately after a long fast."),
    FastingTip("Listen to Your Body", "It's okay to break a fast early if you feel unwell. Fasting should be challenging but not painful."),
    FastingTip("Apple Cider Vinegar", "A tablespoon of ACV in a large glass of water can help stabilize blood sugar and reduce hunger."),
    FastingTip("Stay Busy", "The easiest way to fast is to keep your mind occupied. Work, hobbies, or light exercise can help."),
    FastingTip("Coffee & Tea", "Black coffee and plain tea (no sugar/milk) are generally allowed and can help suppress appetite.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingTipsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fasting Tips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tips) { tip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            tip.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = tip.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tip.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
