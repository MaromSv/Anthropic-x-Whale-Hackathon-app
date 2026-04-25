package com.example.emergency.ui.state

enum class AbcStepId { AIRWAY, BREATHING, CIRCULATION }

data class AbcStep(
    val id: AbcStepId,
    val number: Int,
    val title: String,
    val subtitle: String,
    val checks: List<String>,
    val lookFor: String,
)

data class AbcCheckUiState(
    val title: String,
    val safetyNote: String,
    val steps: List<AbcStep>,
    val checked: Set<AbcStepId>,
)

val SampleAbcCheckUiState = AbcCheckUiState(
    title = "ABC check",
    safetyNote = "Stay safe yourself first. Tap each step as you complete it.",
    steps = listOf(
        AbcStep(
            id = AbcStepId.AIRWAY,
            number = 1,
            title = "Airway",
            subtitle = "Open the airway",
            checks = listOf(
                "Tilt the head back gently",
                "Lift the chin with two fingers",
                "Remove visible obstructions",
            ),
            lookFor = "Mouth and throat clear of obstruction",
        ),
        AbcStep(
            id = AbcStepId.BREATHING,
            number = 2,
            title = "Breathing",
            subtitle = "Look, listen, feel for 10 seconds",
            checks = listOf(
                "Watch the chest for rise and fall",
                "Listen for breath at the mouth",
                "Feel for air on your cheek",
            ),
            lookFor = "Normal, regular breathing — not gasping",
        ),
        AbcStep(
            id = AbcStepId.CIRCULATION,
            number = 3,
            title = "Circulation",
            subtitle = "Check for signs of life",
            checks = listOf(
                "Look for movement or response",
                "Check skin colour and temperature",
                "Scan for severe bleeding",
            ),
            lookFor = "Pulse, movement, normal skin tone",
        ),
    ),
    checked = emptySet(),
)
