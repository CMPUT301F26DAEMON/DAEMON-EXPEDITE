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
 * - **Role switch** (entrant / owner / admin from a profile toggle): a short zoom, so changing "mode"
 *   reads differently from opening a page.
 * - **Tab switch** (bottom bar): both screens slide the full width together, toward the
 *   tab that was tapped, with no fade. The bar itself stays put.
 * - **From the splash screen**: a plain fade.
 */
private typealias NavScope = AnimatedContentTransitionScope<NavBackStackEntry>

private const val SLIDE_MS = 350
private const val ROLE_SWITCH_MS = 400
private const val FADE_MS = 250
private const val TAB_MS = 350

/** How long the entrant bottom bar takes to slide in or out, in step with the screens. */
internal const val TAB_BAR_MS = SLIDE_MS

/** Entrant tab routes in bottom bar order. Messages (the 4th tab) has no screen yet. */
internal val EntrantTabRoutes = listOf(Routes.HOME, Routes.RESERVATIONS, Routes.ENTRANT_LOTTERIES)

/** How far the screen underneath moves (a fraction of the width): a slight parallax. */
private const val PARALLAX = 4

private fun NavScope.from(route: String) = initialState.destination.route == route

/** Each role's profile page, mapped to that role's home. */
private val roleHomeByProfile =
    mapOf(
        Routes.ENTRANT_PROFILE to Routes.HOME,
        Routes.OWNER_PROFILE to Routes.OWNER_HOME,
        Routes.ADMIN_PROFILE to Routes.ADMIN_HOME,
    )

/**
 * True when this navigation is a role switch made with the profile toggle.
 *
 * Role switches are the only way to get from one role's profile to *another* role's home,
 * e.g. entrant profile → owner home, or admin profile → entrant home. (Going back from a
 * profile returns to the same role's home, which is a normal pop.)
 */
private fun NavScope.isRoleSwitch(): Boolean {
    val ownHome = roleHomeByProfile[initialState.destination.route] ?: return false
    val target = targetState.destination.route
    return target != ownHome && target in roleHomeByProfile.values
}

private fun NavScope.isFromSplash() = from(Routes.SPLASH)

/**
 * For a move between two entrant tabs, the way the screens slide: toward the start when the
 * new tab is to the right of the old one, toward the end otherwise. Null for any other move.
 */
private fun NavScope.tabSlide(): SlideDirection? {
    val from = EntrantTabRoutes.indexOf(initialState.destination.route)
    val to = EntrantTabRoutes.indexOf(targetState.destination.route)
    if (from < 0 || to < 0 || from == to) return null
    return if (to > from) SlideDirection.Start else SlideDirection.End
}

private fun NavScope.tabIn(direction: SlideDirection): EnterTransition =
    slideIntoContainer(direction, tween(TAB_MS, easing = FastOutSlowInEasing))

private fun NavScope.tabOut(direction: SlideDirection): ExitTransition =
    slideOutOfContainer(direction, tween(TAB_MS, easing = FastOutSlowInEasing))

// region Transitions handed to NavHost

val enterTransition: NavScope.() -> EnterTransition = {
    tabSlide()?.let { tabIn(it) } ?: when {
        isFromSplash() -> fadeIn(tween(FADE_MS))
        isRoleSwitch() -> roleSwitchIn()
        else -> slideIntoContainer(SlideDirection.Start, tween(SLIDE_MS, easing = FastOutSlowInEasing)) +
            fadeIn(tween(SLIDE_MS))
    }
}

val exitTransition: NavScope.() -> ExitTransition = {
    tabSlide()?.let { tabOut(it) } ?: when {
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
    tabSlide()?.let { tabIn(it) } ?: when {
        isRoleSwitch() -> roleSwitchIn()
        else -> slideIntoContainer(
            SlideDirection.End,
            tween(SLIDE_MS, easing = FastOutSlowInEasing),
            initialOffset = { it / PARALLAX },
        ) + fadeIn(tween(SLIDE_MS))
    }
}

val popExitTransition: NavScope.() -> ExitTransition = {
    tabSlide()?.let { tabOut(it) } ?: when {
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
