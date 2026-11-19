package com.example.daemon_expedite.frontend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.BackButton
import com.example.daemon_expedite.ui_components.ScreenHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AdminMessageUi(
    val id: String,
    val text: String,
    val date: LocalDate,
    val fromOwner: Boolean
)

data class AdminConversationUi(
    val ownerId: String,
    val ownerName: String,
    val entrantId: String,
    val entrantName: String,
    val messages: List<AdminMessageUi> = emptyList()
)
@Composable
fun AdminMessagesScreen(
    conversations: List<AdminConversationUi>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {var selectedOwnerId by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedEntrantId by rememberSaveable {mutableStateOf<String?>(null) }

    //Camp site owners
    val owners = conversations.distinctBy { it.ownerId }

    val selectedOwner = owners.firstOrNull {
        it.ownerId == selectedOwnerId
    } ?: owners.firstOrNull()

    //Entrants
    val entrants = conversations.filter { it.ownerId==selectedOwner?.ownerId}.distinctBy { it.entrantId }

    val selectedConversation = entrants.firstOrNull {it.entrantId == selectedEntrantId} ?: entrants.firstOrNull()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(title = "Messages", onBackClick = onBackClick)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 21.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AdminNameDropdown(
                modifier = Modifier.weight(1f),
                label = "CAMPSITE OWNER",
                selectedName = selectedOwner?.ownerName ?: "Select owner",
                options = owners,
                optionLabel = { it.ownerName },
                onSelect = {
                    selectedOwnerId = it.ownerId
                    selectedEntrantId = null
                }
            )
            AdminNameDropdown(
                modifier = Modifier.weight(1f),
                label = "ENTRANT",
                selectedName = selectedConversation?.entrantName
                    ?: "Select entrant",
                options = entrants,
                optionLabel = { it.entrantName },
                onSelect = {
                    selectedEntrantId = it.entrantId
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        val messages = selectedConversation?.messages.orEmpty()

        LazyColumn( //Empty Lazy Columnfor now implementing later
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            itemsIndexed(
                items = messages,
                key = { _, message -> message.id }
            ) { index, message ->

                Column {
                    if (
                        index == 0 ||
                        messages[index - 1].date != message.date
                    ) {
                        MessageDateLabel(message.date)
                        Spacer(modifier = Modifier.height(13.dp))
                    }

                    AdminMessageCard(message = message)
                }
            }
        }
    }
}
@Composable
private fun <T> AdminNameDropdown(
    label: String,
    selectedName: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = AppTextStyles.FieldLabel,
            color = AppColors.Secondary
        )

        Spacer(modifier = Modifier.height(5.dp))

        Box {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.FillField)
                    .border(
                        1.dp,
                        AppColors.OutlineField,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = options.isNotEmpty()) {
                        expanded = true
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedName,
                    style = AppTextStyles.BodySmall,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = "Select $label",
                    tint = AppColors.Secondary,
                    modifier = Modifier
                        .size(17.dp)
                        .rotate(90f)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(AppColors.SurfaceDeep)
                    .widthIn(min = 150.dp, max = 250.dp)
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel(option),
                                style = AppTextStyles.BodySmall,
                                color = AppColors.TextPrimary
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun MessageDateLabel(date: LocalDate) {
    Text(
        text = date.format(
            DateTimeFormatter.ofPattern(
                "d MMM yyyy",
                Locale.ENGLISH
            )
        ).uppercase(Locale.ENGLISH),
        style = AppTextStyles.FieldLabel,
        color = AppColors.Secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
@Composable
private fun AdminMessageCard( //Card for message can be used again
    message: AdminMessageUi
) {val bubbleColor = if (message.fromOwner) {
        AppColors.FillField
    } else {
        AppColors.Primary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromOwner) {
            Arrangement.Start
        } else {
            Arrangement.End
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .border(
                    1.dp,
                    AppColors.OutlineSubtle,
                    RoundedCornerShape(14.dp)
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                )
        ) {

            Text(
                text = message.text,
                style = AppTextStyles.BodyMedium,
                color = if (message.fromOwner) AppColors.TextPrimary else AppColors.TextOnPrimary
            )
        }
    }
}
/**
 * Preview sample messages:
 * Owner: Hi
 * Entrant: HI
 */
@Preview(showBackground=true,widthDp = 393,heightDp= 852)
@Composable
private fun AdminMessagesScreenPreview() {
    val sampleDate = LocalDate.of(2026, 10, 12)
    val conversations = listOf(
        AdminConversationUi(
            ownerId = "owner1",
            ownerName = "Oliver Hayes",
            entrantId = "entrant1",
            entrantName = "Maya",
            messages = listOf(

                AdminMessageUi(
                    id = "1",
                    text = "Hi", // Checking message cards
                    date = sampleDate,
                    fromOwner = true
                ),

                AdminMessageUi(
                    id = "2",
                    text = "HI",
                    date = sampleDate,
                    fromOwner = false
                )
            )
        ),

        // Additional names checking dropdown

        AdminConversationUi(
            ownerId = "owner1",
            ownerName = "Oliver Hayes",
            entrantId = "entrant2",
            entrantName = "Amitej"
        ),

        AdminConversationUi(
            ownerId = "owner2",
            ownerName = "Elena Park",
            entrantId = "entrant3",
            entrantName = "Armaan"
        )
    )
    AdminMessagesScreen(
        conversations = conversations,
        onBackClick = {}
    )
}
