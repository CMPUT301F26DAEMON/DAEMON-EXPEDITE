package com.example.daemon_expedite.frontend

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry

/**
 * Screen-change animations for the app's NavHost (see [AppNavigation]).
 *
 * Three kinds, so each kind of move feels different:
 * - **Push / pop** (opening a screen, going back): the new screen slides in from the right and
 *   the old one drifts a little to the left and fades, like turning a page. Back is the reverse.
 * - **Role switch** (entrant ⇄ owner from a profile toggle): a short zoom, so changing "mode"
 *   reads differently from opening a page.
 * - **From the splash screen**: a plain fade.
 */
private typealias NavScope = AnimatedContentTransitionScope<NavBackStackEntry>

private const val SLIDE_MS = 350
private const val ROLE_SWITCH_MS = 400
private const val FADE_MS = 250

/** How far the screen underneath moves (a fraction of the width): a slight parallax. */
private const val PARALLAX = 4

private fun NavScope.from(route: String) = initialState.destination.route == route
private fun NavScope.to(route: String) = targetState.destination.route == route

/**
 * True when this navigation is a role switch made with the profile toggle.
 *
 * Role switches are the only way to get from one role's profile to the *other* role's home:
 * entrant profile → owner home, or owner profile → entrant home. (Going back from a profile
 * returns to the same role's home, which is a normal pop.)
 */
private fun NavScope.isRoleSwitch() =
    (from(Routes.ENTRANT_PROFILE) && to(Routes.OWNER_HOME)) ||
        (from(Routes.OWNER_PROFILE) && to(Routes.HOME))

private fun NavScope.isFromSplash() = from(Routes.SPLASH)

// region Transitions handed to NavHost

val enterTransition: NavScope.() -> EnterTransition = {
    when {
        isFromSplash() -> fadeIn(tween(FADE_MS))
        isRoleSwitch() -> roleSwitchIn()
        else -> slideIntoContainer(SlideDirection.Start, tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
            fadeIn(tween(SLIDE_MS))
    }
}

val exitTransition: NavScope.() -> ExitTransition = {
    when {
        isFromSplash() -> fadeOut(tween(FADE_MS))
        isRoleSwitch() -> roleSwitchOut()
        else -> slideOutOfContainer(
            SlideDirection.Start,
            tween(SLIDE_MS, easing = FastOutSlowInEasing),
            targetOffset = { it / PARALLAX },
        ) + fadeOut(tween(SLIDE_MS))
    }
}

val popEnterTransition: NavScope.() -> EnterTransition = {
    when {
        isRoleSwitch() -> roleSwitchIn()
        else -> slideIntoContainer(
            SlideDirection.End,
            tween(SLIDE_MS, easing = FastOutSlowInEasing),
            initialOffset = { it / PARALLAX },
        ) + fadeIn(tween(SLIDE_MS))
    }
}

val popExitTransition: NavScope.() -> ExitTransition = {
    when {
        isRoleSwitch() -> roleSwitchOut()
        else -> slideOutOfContainer(SlideDirection.End, tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
            fadeOut(tween(SLIDE_MS))
    }
}

// endregion

/** New role's home grows in from slightly smaller while fading in. */
private fun roleSwitchIn(): EnterTransition =
    scaleIn(tween(ROLE_SWITCH_MS, easing = FastOutSlowInEasing), initialScale = 0.92f) +
        fadeIn(tween(ROLE_SWITCH_MS))

/** Old profile grows slightly and fades away, as if the new view comes from behind it. */
private fun roleSwitchOut(): ExitTransition =
    scaleOut(tween(ROLE_SWITCH_MS, easing = FastOutSlowInEasing), targetScale = 1.06f) +
        fadeOut(tween(ROLE_SWITCH_MS))
