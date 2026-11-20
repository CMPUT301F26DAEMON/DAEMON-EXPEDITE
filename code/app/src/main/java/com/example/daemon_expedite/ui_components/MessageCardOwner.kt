package com.example.daemon_expedite.ui_components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A reusable conversation card for Messaging Page
 *
 * Shows a profile picture, sender name, latest text and a timestamp
 * Unread conversations display a green timestamp and unread indicator
 *
 * @param senderName Name of the person
 * @param lastMessage Display latest message
 * @param timestamp Time and date of message
 * @param onClick Called when tapped
 * @param modifier [Modifier] applied to root
 * @param isUnread green dot if message unread
 * @param imageUrl Web address of the profile photo. Loaded with Coil.
 * @param profileImageRes Optional drawable for the profile picture fallback
 * @param showDivider Whether to draw a separator line below card
 */
@Composable
fun MessageCardOwner(
    senderName: String,
    lastMessage: String,
    timestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUnread: Boolean = false,
    imageUrl: String? = null,
    @DrawableRes profileImageRes: Int? = null,
    showDivider: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 83.dp)
                    .clickable(onClick = onClick)
                    .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Profile picture
            if (imageUrl != null || profileImageRes != null) {
                ProfileImage(
                    imageUrl = imageUrl,
                    size = 40.dp,
                    contentDescription = "$senderName profile picture",
                    fallbackRes = profileImageRes ?: com.example.daemon_expedite.R.drawable.mock_profile,
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceDisabled),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Profile,
                        contentDescription = null,
                        tint = AppColors.Secondary,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Sender name, timestamp, and most recent message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = senderName,
                        style = AppTextStyles.ValueEmphasis,
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = timestamp,
                        style = AppTextStyles.BodySmall,
                        color =
                            if (isUnread) {
                                AppColors.Secondary
                            } else {
                                AppColors.TextMuted
                            },
                        maxLines = 1,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = lastMessage,
                        style =
                            AppTextStyles.BodyMedium.copy(
                                fontWeight =
                                    if (isUnread) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    },
                            ),
                        color =
                            if (isUnread) {
                                AppColors.TextPrimary
                            } else {
                                AppColors.TextSecondary
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    // Unread message indicator
                    if (isUnread) {
                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Primary),
                        )
                    }
                }
            }
        }

        // Divider between conversations
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = AppColors.OutlineSubtle,
            )
        }
    }
}

/**
 * Preview showing reusable message cards with sample conversations.
 */
@Preview(
    showBackground = true,
    widthDp = 405,
    heightDp = 340,
)
@Composable
private fun MessageCardPreview() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        MessageCardOwner(
            senderName = "Maya Bennett",
            lastMessage = "Perfect, we’ll arrive around 3 PM. See you soon!",
            timestamp = "9:41 AM",
            isUnread = true,
            onClick = {},
        )

        MessageCardOwner(
            senderName = "Amitej",
            lastMessage = "Hello, where is the campsite?",
            timestamp = "Yesterday",
            isUnread = true,
            onClick = {},
        )
        MessageCardOwner(
            senderName = "Keya",
            lastMessage = "Hello, does the campsite have a firepit?",
            timestamp = "11 Oct",
            isUnread = false,
            showDivider = false,
            onClick = {},
        )
    }
}
