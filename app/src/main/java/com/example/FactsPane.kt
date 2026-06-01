package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FastingResource(
    val title: String,
    val summary: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val tag: String
)

@Composable
fun FactsPane() {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Tips, 1 = Mistakes, 2 = Facts
    var selectedResourceForDetail by remember { mutableStateOf<FastingResource?>(null) }

    val tips = listOf(
        FastingResource(
            title = "Hydrate Mindfully",
            summary = "Water, black coffee, and tea support metabolic rest.",
            description = "Sipping mineral water, plain black coffee, or herbal tea helps suppress insulin levels and stabilizes your hunger-hormone (ghrelin). Avoid creamers, sweeteners, or milk, as they raise blood glucose and break autophagic renewal.",
            icon = Icons.Default.Favorite,
            color = Color(0xFF3B82F6),
            tag = "HYDRATION"
        ),
        FastingResource(
            title = "Supplement Pink Salt",
            summary = "Alleviate headaches by restoring electrolytes.",
            description = "Fasting prompts kidneys to flush water and essential sodium. Adding a small pinch of pink Himalayan salt or pure electrolytes to your water helps regulate cellular hydration, preventing the standard keto-headache and fatigue.",
            icon = Icons.Default.Info,
            color = Color(0xFFFBBF24),
            tag = "ELECTROLYTES"
        ),
        FastingResource(
            title = "Transition Gradually",
            summary = "Allow metabolic machinery to adapt slowly.",
            description = "If you are new to fasting, start with a simple 12-hour or 14-hour window, then increment to 16/8 or 18/6. Your body needs several days to up-regulate enzymes responsible for fatty acid oxidation and ketone synthesis.",
            icon = Icons.Default.Refresh,
            color = Color(0xFF10B981),
            tag = "PROGRESSION"
        ),
        FastingResource(
            title = "Focus on Clean Breaking",
            summary = "Reintroduce food slowly without insulin spikes.",
            description = "Break your fasting periods with easily digestible, low-glycemic, high-protein options, such as bone broth, eggs, leafy greens, or healthy fats. Reintroducing massive carbs immediately causes a sudden and stressful insulin surge.",
            icon = Icons.Default.Settings,
            color = Color(0xFF8B5CF6),
            tag = "NUTRITION"
        )
    )

    val mistakes = listOf(
        FastingResource(
            title = "Hidden Liquid Calories",
            summary = "Supposedly 'diet' sodas can crash autophagy.",
            description = "Flavored liquid drops, sweeteners (like sucralose or stevia), and branched-chain amino acids (BCAAs) are common culprits. While they are zero-calorie, they can trigger cephalic-phase insulin releases, stopping autophagic recycling.",
            icon = Icons.Default.Warning,
            color = Color(0xFFEF4444),
            tag = "CEPHALIC PHASE"
        ),
        FastingResource(
            title = "Severe Hydration Neglect",
            summary = "Confounding signal: thirsty often feels like hungry.",
            description = "Many fasting discomforts (dizziness, dry eyes, mild nausea) are actually dehydration in disguise. Always ensure you are replacing lost bodily fluids at regular intervals throughout the non-eating schedule.",
            icon = Icons.Default.Close,
            color = Color(0xFFEF4444),
            tag = "DEHYDRATION"
        ),
        FastingResource(
            title = "Compensation Overeating",
            summary = "Feasting out of control defeats weight control goals.",
            description = "Thinking that a 16-hour fast grants license to consume unlimited fast foods is a major pitfall. For metabolic and weight management success, focus on high-quality and wholesome caloric intakes during your designated eating block.",
            icon = Icons.Default.Warning,
            color = Color(0xFFEF4444),
            tag = "CALORIC CONTROL"
        ),
        FastingResource(
            title = "Neglecting Body Warning Signs",
            summary = "Pushing through extreme distress leads to crashing.",
            description = "Fasting is a hormetic (mild, adaptive) stress. However, if you experience sudden cold shakes, extreme lightheadedness, or severe nausea, do not hesitate to end your fast immediately and seek biological balance.",
            icon = Icons.Default.Close,
            color = Color(0xFFEF4444),
            tag = "SAFETY FIRST"
        )
    )

    val facts = listOf(
        FastingResource(
            title = "HGH Surge Dynamics",
            summary = "Human Growth Hormone climbs up to 5x after 24h.",
            description = "Human Growth Hormone (HGH) peaks after 20-24 hours of total fasting. HGH preserves lean muscle mass, facilitates systemic fat tissue burning, and helps preserve metabolic vigor even during extended low-calorie hours.",
            icon = Icons.Default.Check,
            color = Color(0xFF8B5CF6),
            tag = "CELLULAR FORCE"
        ),
        FastingResource(
            title = "Autophagy Recycling Peak",
            summary = "Cellular cleanup sweeps misfolded target proteins.",
            description = "Autophagy is a vital housekeeping process where cells disassemble and clean up damaged, old, or toxic cellular organelles and junk proteins. It peaks heavily between 24 and 48 hours of fasting, supporting cellular renewal.",
            icon = Icons.Default.ThumbUp,
            color = Color(0xFF10B981),
            tag = "IMMUNE RESET"
        ),
        FastingResource(
            title = "The Ketone Brain Fuel",
            summary = "Beta-Hydroxybutyrate acts as clean mental fuel.",
            description = "When liver glycogen reserves are exhausted (typically around 14-16 hours), your body shifts to burning fat. Fat is converted into ketones (Beta-Hydroxybutyrate), which cross the blood-brain barrier to serve as clear, efficient brain energy.",
            icon = Icons.Default.Star,
            color = Color(0xFF3B82F6),
            tag = "BRAIN COGNITION"
        ),
        FastingResource(
            title = "Insulin Correction Baseline",
            summary = "Reduces systemic resistance and promotes recovery.",
            description = "Fasting allows the insulin-secreting beta cells in the pancreas to rest. Lower insulin allows fat storage cells to release energy naturally, which gradually reverses insulin resistance and long-term metabolic strain.",
            icon = Icons.Default.Notifications,
            color = Color(0xFFEC4899),
            tag = "METABOLISM"
        )
    )

    val activeList = when (selectedTab) {
        0 -> tips
        1 -> mistakes
        else -> facts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Knowledge & Guidelines",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = "Understand the cellular science, avoid standard pitfalls, and optimize your path.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Segmented Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tips & Rules", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Mistakes", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Science", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        // Active List displaying cards
        Text(
            text = when (selectedTab) {
                0 -> "Fasting Tips & Essential Rules"
                1 -> "Common Mistakes & Pitfalls"
                else -> "Metabolic & Cellular Insights"
            },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        activeList.forEachIndexed { index, resource ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedResourceForDetail = resource }
                    .testTag("resource_card_${selectedTab}_$index"),
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
                    // Thick Left Color Strip
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(resource.color)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icon Box
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(resource.color.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resource.icon,
                                contentDescription = null,
                                tint = resource.color,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text content
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = resource.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(resource.color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = resource.tag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = resource.color,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 0.5.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resource.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Pop-up Alert Dialog
    if (selectedResourceForDetail != null) {
        val resource = selectedResourceForDetail!!
        AlertDialog(
            onDismissRequest = { selectedResourceForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(resource.color.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resource.icon,
                            contentDescription = null,
                            tint = resource.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = resource.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = resource.summary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = resource.color
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = resource.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selectedResourceForDetail = null }
                ) {
                    Text("Got it")
                }
            }
        )
    }
}
