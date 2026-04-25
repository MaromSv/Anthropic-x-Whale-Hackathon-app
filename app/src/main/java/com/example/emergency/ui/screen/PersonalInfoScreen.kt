package com.example.emergency.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emergency.ui.state.EmergencyContact
import com.example.emergency.ui.state.MedicalInfo
import com.example.emergency.ui.state.PersonalInfoUiState
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme

@Composable
fun PersonalInfoScreen(
    state: PersonalInfoUiState,
    onBack: () -> Unit = {},
    onEditIdentity: () -> Unit = {},
    onEditMedical: () -> Unit = {},
    onAddContact: () -> Unit = {},
    onContactClick: (EmergencyContact) -> Unit = {},
    onContactCall: (EmergencyContact) -> Unit = {},
    onLockScreenToggle: (Boolean) -> Unit = {},
) {
    val colors = EmergencyTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        SubScreenTopBar(title = "Personal info", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            IdentityCard(
                name = state.name,
                dateOfBirth = state.dateOfBirth,
                onEdit = onEditIdentity,
            )

            Spacer(modifier = Modifier.height(20.dp))
            ScreenSectionHeader(text = "MEDICAL")
            Spacer(modifier = Modifier.height(8.dp))
            MedicalSection(medical = state.medical, onEdit = onEditMedical)

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ScreenSectionHeader(text = "EMERGENCY CONTACTS", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            ContactsSection(
                contacts = state.contacts,
                onContactClick = onContactClick,
                onContactCall = onContactCall,
                onAddContact = onAddContact,
            )

            Spacer(modifier = Modifier.height(20.dp))
            ScreenSectionHeader(text = "ON-DEVICE")
            Spacer(modifier = Modifier.height(8.dp))
            LockScreenRow(
                checked = state.showOnLockScreen,
                onCheckedChange = onLockScreenToggle,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saved on this device only \u00B7 Never synced.",
                style = EmergencyTheme.typography.helper.copy(fontSize = 12.sp),
                color = colors.textFaint,
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun IdentityCard(
    name: String?,
    dateOfBirth: String?,
    onEdit: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.card)
            .clickable(onClick = onEdit)
            .padding(16.dp),
    ) {
        InitialsAvatar(name = name)
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name ?: "Add your name",
                style = typography.listItem.copy(fontSize = 16.sp),
                color = if (name != null) colors.text else colors.textFaint,
            )
            dateOfBirth?.let { dob ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dob,
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textDim,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Edit identity",
            tint = colors.textFaint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun InitialsAvatar(name: String?) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    val initials = name
        ?.split(" ")
        ?.mapNotNull { it.firstOrNull()?.uppercase() }
        ?.take(2)
        ?.joinToString("")
        ?: "?"

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(EmergencyShapes.full)
            .background(colors.panel)
            .border(1.dp, colors.line, EmergencyShapes.full),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = typography.listItem.copy(fontSize = 14.sp),
            color = colors.textDim,
        )
    }
}

@Composable
private fun MedicalSection(
    medical: MedicalInfo,
    onEdit: () -> Unit,
) {
    GroupedListContainer {
        MedicalRow(label = "Blood type", value = medical.bloodType ?: "Not set", onClick = onEdit)
        GroupedListDivider()
        ChipMedicalRow(label = "Allergies", chips = medical.allergies, onClick = onEdit)
        GroupedListDivider()
        ChipMedicalRow(label = "Conditions", chips = medical.conditions, onClick = onEdit)
        GroupedListDivider()
        ChipMedicalRow(label = "Medications", chips = medical.medications, onClick = onEdit)
    }
}

@Composable
private fun MedicalRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = typography.listItem.copy(fontSize = 14.sp),
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = typography.body.copy(fontSize = 14.sp),
            color = colors.textDim,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textFaint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ChipMedicalRow(
    label: String,
    chips: List<String>,
    onClick: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            if (chips.isEmpty()) {
                Text(
                    text = "Add",
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textFaint,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (chips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chips.forEach { chip -> InfoChip(text = chip) }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography
    Box(
        modifier = Modifier
            .clip(EmergencyShapes.full)
            .background(colors.panel)
            .border(1.dp, colors.line, EmergencyShapes.full)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = typography.helper.copy(fontSize = 12.sp),
            color = colors.textDim,
        )
    }
}

@Composable
private fun ContactsSection(
    contacts: List<EmergencyContact>,
    onContactClick: (EmergencyContact) -> Unit,
    onContactCall: (EmergencyContact) -> Unit,
    onAddContact: () -> Unit,
) {
    GroupedListContainer {
        contacts.forEach { contact ->
            ContactRow(
                contact = contact,
                onClick = { onContactClick(contact) },
                onCall = { onContactCall(contact) },
            )
            GroupedListDivider()
        }
        AddContactRow(onClick = onAddContact)
    }
}

@Composable
private fun ContactRow(
    contact: EmergencyContact,
    onClick: () -> Unit,
    onCall: () -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = typography.listItem.copy(fontSize = 14.sp),
                color = colors.text,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${contact.relation} \u00B7 ${contact.phone}",
                style = typography.helper.copy(fontSize = 12.sp),
                color = colors.textDim,
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(EmergencyShapes.full)
                .background(colors.panel)
                .border(1.dp, colors.line, EmergencyShapes.full)
                .clickable(onClick = onCall),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = "Call ${contact.name}",
                tint = colors.text,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun AddContactRow(onClick: () -> Unit) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.textDim,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = "Add contact",
            style = typography.listItem.copy(fontSize = 14.sp),
            color = colors.text,
        )
    }
}

@Composable
private fun LockScreenRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    GroupedListContainer {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show on lock screen",
                    style = typography.listItem.copy(fontSize = 14.sp),
                    color = colors.text,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Responders can see allergies, blood type, and contacts.",
                    style = typography.helper.copy(fontSize = 12.sp),
                    color = colors.textDim,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.accentInk,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.surface,
                    uncheckedTrackColor = colors.panel,
                    uncheckedBorderColor = colors.line,
                ),
            )
        }
    }
}
