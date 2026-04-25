package com.example.emergency.ui.state

enum class CprIllustrationKind { SAFETY, RESPONSE, CALL, HANDS, METRONOME, RECOVERY }

data class CprStep(
    val id: String,
    val eyebrow: String,
    val title: String,
    val bullets: List<String>,
    val note: String,
    val illustration: CprIllustrationKind,
)

val CprWalkthroughSteps: List<CprStep> = listOf(
    CprStep(
        id = "safety",
        eyebrow = "Step 1 of 6",
        title = "Make sure the area is safe.",
        bullets = listOf(
            "Check for traffic, fire, or electrical hazards.",
            "If it isn't safe, don't approach \u2014 call 112 and wait.",
        ),
        note = "Your safety comes first. You can't help if you become a second casualty.",
        illustration = CprIllustrationKind.SAFETY,
    ),
    CprStep(
        id = "response",
        eyebrow = "Step 2 of 6",
        title = "Check for response.",
        bullets = listOf(
            "Tap their shoulders firmly.",
            "Shout: \u201CAre you okay?\u201D close to both ears.",
            "No response \u2192 continue. Responsive \u2192 keep them still and call 112.",
        ),
        note = "Don't shake the head or neck \u2014 assume possible spine injury.",
        illustration = CprIllustrationKind.RESPONSE,
    ),
    CprStep(
        id = "call",
        eyebrow = "Step 3 of 6",
        title = "Call 112. Get an AED.",
        bullets = listOf(
            "Put your phone on speaker so your hands stay free.",
            "Tell someone specific to find the nearest defibrillator.",
            "Don't stop to search yourself \u2014 stay with the person.",
        ),
        note = "In Amsterdam, a public AED is usually within 200 m. The map can show the nearest one.",
        illustration = CprIllustrationKind.CALL,
    ),
    CprStep(
        id = "position",
        eyebrow = "Step 4 of 6",
        title = "Place hands in the centre of the chest.",
        bullets = listOf(
            "Heel of one hand on the lower half of the breastbone.",
            "Cover with your other hand. Interlock your fingers.",
            "Keep your arms straight, shoulders directly above your hands.",
        ),
        note = "Lift your fingers off the ribs \u2014 only the heel of your hand presses down.",
        illustration = CprIllustrationKind.HANDS,
    ),
    CprStep(
        id = "compress",
        eyebrow = "Step 5 of 6",
        title = "Push hard, push fast.",
        bullets = listOf(
            "5\u20136 cm deep. Let the chest fully rise between pushes.",
            "100\u2013120 per minute \u2014 follow the metronome.",
            "After 30 compressions, give 2 rescue breaths if trained. Otherwise keep pushing.",
        ),
        note = "Don't stop. Switch with someone every 2 minutes if you can \u2014 it's exhausting and that's normal.",
        illustration = CprIllustrationKind.METRONOME,
    ),
    CprStep(
        id = "continue",
        eyebrow = "Step 6 of 6",
        title = "Don't stop until help arrives.",
        bullets = listOf(
            "An AED arrives \u2192 turn it on, follow its voice instructions.",
            "They start breathing normally \u2192 recovery position, monitor.",
            "You're too exhausted to continue safely \u2192 swap with someone.",
        ),
        note = "Any CPR is better than no CPR. You're doing the right thing.",
        illustration = CprIllustrationKind.RECOVERY,
    ),
)
