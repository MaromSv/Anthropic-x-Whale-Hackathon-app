package com.example.emergency.ui.state

enum class AbcStepId { AIRWAY, BREATHING, CIRCULATION }

enum class AbcIllustrationKind { AIRWAY, BREATHING, CIRCULATION }

data class AbcStep(
    val id: AbcStepId,
    val letter: String,
    val eyebrow: String,
    val title: String,
    val bullets: List<String>,
    val note: String,
    val illustration: AbcIllustrationKind,
    val question: String,
)

data class AbcCheckUiState(
    val title: String,
    val steps: List<AbcStep>,
)

val SampleAbcCheckUiState = AbcCheckUiState(
    title = "ABC check",
    steps = listOf(
        AbcStep(
            id = AbcStepId.AIRWAY,
            letter = "A",
            eyebrow = "Step 1 of 3",
            title = "Open the airway.",
            bullets = listOf(
                "Place one hand on the forehead, two fingers under the chin.",
                "Tilt the head back gently. Lift the chin.",
                "Look in the mouth \u2014 only remove what you can clearly see.",
            ),
            note = "Don\u2019t sweep blindly with your fingers \u2014 you can push an obstruction deeper.",
            illustration = AbcIllustrationKind.AIRWAY,
            question = "Is the airway clear?",
        ),
        AbcStep(
            id = AbcStepId.BREATHING,
            letter = "B",
            eyebrow = "Step 2 of 3",
            title = "Check for normal breathing.",
            bullets = listOf(
                "Watch the chest rise and fall for 10 seconds.",
                "Listen and feel for breath against your cheek.",
                "Gasping or irregular gulps is NOT normal breathing.",
            ),
            note = "10 seconds. No longer. If unsure, treat as not breathing and start CPR.",
            illustration = AbcIllustrationKind.BREATHING,
            question = "Are they breathing normally?",
        ),
        AbcStep(
            id = AbcStepId.CIRCULATION,
            letter = "C",
            eyebrow = "Step 3 of 3",
            title = "Check circulation.",
            bullets = listOf(
                "Look at the skin colour \u2014 pale, blue, or grey is a warning sign.",
                "Check for major bleeding. Apply firm direct pressure to wounds.",
                "If trained, feel the carotid pulse \u2014 but don\u2019t delay CPR to do it.",
            ),
            note = "If they\u2019re not breathing, signs of life are absent \u2014 start CPR. Don\u2019t wait.",
            illustration = AbcIllustrationKind.CIRCULATION,
            question = "Signs of circulation?",
        ),
    ),
)
