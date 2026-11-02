package com.example.daemon_expedite.frontend

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.daemon_expedite.ui_components.Togglebutton
import kotlinx.coroutines.delay

/**
 * The views a person can switch between from their profile.
 *
 * Everyone is an [ENTRANT]. Anyone can become an [OWNER]: the first switch creates their record
 * in the "owners" collection. [ADMIN] is only offered to people a developer has added to the
 * "admins" collection (see [SessionState.Ready.isAdmin]). All three records share the same id.
 */
enum class ProfileRole(val title: String) {
    ENTRANT("Entrant"),
    OWNER("Owner"),
    ADMIN("Admin"),
}

/** The roles to offer: Entrant and Owner for everyone, plus Admin for admins. */
fun availableRoles(isAdmin: Boolean): List<ProfileRole> =
    if (isAdmin) ProfileRole.entries else listOf(ProfileRole.ENTRANT, ProfileRole.OWNER)

/**
 * How long to let the toggle's pill slide before switching screens. The pill's spring (see
 * Togglebutton) settles in about 400ms; switching a little before that ends shows the whole
 * slide while the next screen starts appearing.
 */
private const val SWITCH_DELAY_MS = 350L

/**
 * If the screen is still showing this long after [onRoleSelected] (because that role isn't
 * available yet, e.g. Admin), the pill slides back to [current]. Longer than the screen-change
 * animation, so it never slides back while the page is leaving.
 */
private const val RESET_DELAY_MS = 1_000L

/**
 * Segmented toggle for switching between entrant, owner and (for admins) admin views.
 *
 * Used on both the entrant and the owner profile screens. Tapping another role first slides the
 * pill to it, then calls [onRoleSelected] once the slide has played ([SWITCH_DELAY_MS]), so the
 * animation is visible before the screen changes. Taps are ignored while a switch is pending.
 *
 * @param current The role whose screens the user is in right now. Shown as selected.
 * @param isAdmin Whether to include the Admin option.
 * @param onRoleSelected Called with the tapped role, only when it differs from [current].
 */
@Composable
fun RoleSelector(
    current: ProfileRole,
    isAdmin: Boolean,
    onRoleSelected: (ProfileRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roles = availableRoles(isAdmin)
    // The role the pill shows: jumps to the tapped role straight away so the pill animates.
    var shown by remember(current) { mutableStateOf(current) }
    // A tapped role waiting for the slide to finish before switching.
    var pending by remember(current) { mutableStateOf<ProfileRole?>(null) }
    // Always call the latest callback, even though the effect below started earlier.
    val latestOnRoleSelected by rememberUpdatedState(onRoleSelected)

    LaunchedEffect(pending) {
        val role = pending ?: return@LaunchedEffect
        delay(SWITCH_DELAY_MS)
        latestOnRoleSelected(role)
        // Normally the screen changes and this effect is cancelled before getting here.
        delay(RESET_DELAY_MS)
        shown = current
        pending = null
    }

    Togglebutton(
        options = roles.map { it.title },
        selectedIndex = roles.indexOf(shown).coerceAtLeast(0),
        onOptionSelected = { index ->
            val role = roles[index]
            if (pending == null && role != current) {
                shown = role
                pending = role
            }
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF121514)
@Composable
private fun RoleSelectorPreview() {
    Column(Modifier.padding(16.dp)) {
        RoleSelector(current = ProfileRole.ENTRANT, isAdmin = false, onRoleSelected = {})
        RoleSelector(current = ProfileRole.OWNER, isAdmin = true, onRoleSelected = {},
            modifier = Modifier.padding(top = 12.dp))
    }
}
