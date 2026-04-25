package com.example.emergency.ui.state

data class ConversationSummary(
    val id: String,
    val title: String,
    val messageCount: Int,
    val timeLabel: String,
)

data class ConversationsUiState(
    val title: String,
    val conversations: List<ConversationSummary>,
)

val SampleConversationsUiState = ConversationsUiState(
    title = "Conversations",
    conversations = listOf(
        ConversationSummary(
            id = "conv-ankle",
            title = "Twisted my ankle",
            messageCount = 12,
            timeLabel = "2 hours ago",
        ),
        ConversationSummary(
            id = "conv-power",
            title = "Power outage at the festival",
            messageCount = 8,
            timeLabel = "Yesterday",
        ),
        ConversationSummary(
            id = "conv-allergy",
            title = "Allergic reaction help",
            messageCount = 23,
            timeLabel = "Apr 21",
        ),
        ConversationSummary(
            id = "conv-lost-friend",
            title = "Lost my friend in the crowd",
            messageCount = 6,
            timeLabel = "Apr 18",
        ),
    ),
)

val EmptyConversationsUiState = ConversationsUiState("Conversations", emptyList())
