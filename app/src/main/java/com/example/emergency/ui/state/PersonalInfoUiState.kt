package com.example.emergency.ui.state

data class EmergencyContact(
    val id: String,
    val name: String,
    val relation: String,
    val phone: String,
)

data class MedicalInfo(
    val bloodType: String?,
    val allergies: List<String>,
    val conditions: List<String>,
    val medications: List<String>,
)

data class PersonalInfoUiState(
    val name: String?,
    val dateOfBirth: String?,
    val medical: MedicalInfo,
    val contacts: List<EmergencyContact>,
    val showOnLockScreen: Boolean,
)

val SamplePersonalInfoUiState = PersonalInfoUiState(
    name = "Sascha K.",
    dateOfBirth = "12 Mar 1996",
    medical = MedicalInfo(
        bloodType = "A+",
        allergies = listOf("Penicillin", "Shellfish"),
        conditions = listOf("Asthma"),
        medications = listOf("Salbutamol inhaler"),
    ),
    contacts = listOf(
        EmergencyContact(
            id = "mom",
            name = "Eva K.",
            relation = "Mother",
            phone = "+31 6 1234 5678",
        ),
        EmergencyContact(
            id = "gp",
            name = "Dr. Visser",
            relation = "GP",
            phone = "+31 20 555 0142",
        ),
    ),
    showOnLockScreen = true,
)

val EmptyPersonalInfoUiState = PersonalInfoUiState(
    name = null,
    dateOfBirth = null,
    medical = MedicalInfo(
        bloodType = null,
        allergies = emptyList(),
        conditions = emptyList(),
        medications = emptyList(),
    ),
    contacts = emptyList(),
    showOnLockScreen = false,
)
