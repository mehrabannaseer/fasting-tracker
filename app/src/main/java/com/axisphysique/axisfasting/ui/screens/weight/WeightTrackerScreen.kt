package com.axisphysique.axisfasting.ui.screens.weight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WeightEntry(
    val weight: Float,
    val isMetric: Boolean,
    val date: LocalDate = LocalDate.now()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen(
    onBack: () -> Unit,
    viewModel: WeightViewModel = viewModel()
) {
    var isMetric by remember { mutableStateOf(true) }
    var weightInput by remember { mutableStateOf("") }
    val weightEntries by viewModel.weightEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weight Tracking") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Metric (kg)")
                        Switch(
                            checked = !isMetric,
                            onCheckedChange = { isMetric = !it },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text("Imperial (lbs)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Current Weight (${if (isMetric) "kg" else "lbs"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.MonitorWeight, null) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val w = weightInput.toFloatOrNull()
                            if (w != null) {
                                viewModel.addEntry(w, isMetric)
                                weightInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Weight")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weightEntries) { entry ->
                    ListItem(
                        headlineContent = { 
                            val unit = if (entry.isMetric) "kg" else "lbs"
                            Text("${entry.weight} $unit", fontWeight = FontWeight.Bold) 
                        },
                        supportingContent = { 
                            Text(entry.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))) 
                        },
                        leadingContent = {
                            Icon(Icons.Default.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }
        }
    }
}
