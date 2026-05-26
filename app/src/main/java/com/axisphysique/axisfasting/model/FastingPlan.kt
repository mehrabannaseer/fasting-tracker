package com.axisphysique.axisfasting.model

import java.time.Duration

enum class FastingPlan(
    val displayName: String,
    val description: String,
    val fastingHours: Long,
    val colorHex: Long
) {
    PLAN_16_8("16:8", "16 hours fast, 8 hours window", 16, 0xFF4CAF50),
    PLAN_18_6("18:6", "18 hours fast, 6 hours window", 18, 0xFF2196F3),
    PLAN_20_4("20:4", "20 hours fast, 4 hours window", 20, 0xFFFF9800),
    OMAD("OMAD", "One Meal A Day (23:1)", 23, 0xFFE91E63),
    PLAN_36("36h", "36 hours Monk Fast", 36, 0xFF9C27B0),
    PLAN_48("48h", "48 hours Prolonged Fast", 48, 0xFF3F51B5),
    PLAN_72("72h", "72 hours Prolonged Fast", 72, 0xFFF44336);

    val targetDuration: Duration get() = Duration.ofHours(fastingHours)
}
