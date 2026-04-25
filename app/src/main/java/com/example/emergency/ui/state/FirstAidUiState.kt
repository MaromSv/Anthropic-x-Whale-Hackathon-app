package com.example.emergency.ui.state

enum class FirstAidUrgency { CRITICAL, GENERAL }

enum class FirstAidTopicId {
    CPR, BLEEDING, CHOKING, BURNS, DROWNING, HEAT_STROKE, HYPOTHERMIA, SEIZURE
}

data class FirstAidTopic(
    val id: FirstAidTopicId,
    val title: String,
    val summary: String,
    val urgency: FirstAidUrgency,
    val readTimeLabel: String,
    val tag: String? = null,
)

data class FirstAidUiState(
    val title: String,
    val subtitle: String,
    val topics: List<FirstAidTopic>,
)

val SampleFirstAidUiState = FirstAidUiState(
    title = "First aid",
    subtitle = "Step-by-step guides for common emergencies. Works offline.",
    topics = listOf(
        FirstAidTopic(
            id = FirstAidTopicId.CPR,
            title = "CPR for adults",
            summary = "Chest compressions, rescue breaths, AED basics",
            urgency = FirstAidUrgency.CRITICAL,
            readTimeLabel = "45 sec read",
            tag = "step-by-step",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.BLEEDING,
            title = "Severe bleeding",
            summary = "Direct pressure, packing wounds, tourniquets",
            urgency = FirstAidUrgency.CRITICAL,
            readTimeLabel = "30 sec read",
            tag = "step-by-step",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.CHOKING,
            title = "Choking",
            summary = "Back blows and abdominal thrusts for adults",
            urgency = FirstAidUrgency.CRITICAL,
            readTimeLabel = "25 sec read",
            tag = "video",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.BURNS,
            title = "Burns and scalds",
            summary = "Cool the burn, cover loosely, when to seek help",
            urgency = FirstAidUrgency.GENERAL,
            readTimeLabel = "30 sec read",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.DROWNING,
            title = "Drowning",
            summary = "Safe rescue, rescue breaths, recovery position",
            urgency = FirstAidUrgency.GENERAL,
            readTimeLabel = "40 sec read",
            tag = "step-by-step",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.HEAT_STROKE,
            title = "Heat stroke",
            summary = "Recognising warning signs, rapid cooling steps",
            urgency = FirstAidUrgency.GENERAL,
            readTimeLabel = "30 sec read",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.HYPOTHERMIA,
            title = "Hypothermia",
            summary = "Warming gradually, what to avoid, when to call",
            urgency = FirstAidUrgency.GENERAL,
            readTimeLabel = "35 sec read",
        ),
        FirstAidTopic(
            id = FirstAidTopicId.SEIZURE,
            title = "Seizure",
            summary = "Protect the head, time the seizure, aftercare",
            urgency = FirstAidUrgency.GENERAL,
            readTimeLabel = "25 sec read",
        ),
    ),
)
