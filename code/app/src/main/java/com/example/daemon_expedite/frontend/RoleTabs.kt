package com.example.daemon_expedite.frontend

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.daemon_expedite.ui_components.AppIcons

/**
 * One role's bottom bar: its tabs, and the screens they open.
 *
 * [AppNavigation] draws a single bar over the NavHost and picks the role whose screens are
 * showing, so the bar stays put while screens change underneath it.
 *
 * @param home The role's home route. Tab screens open directly above it, so moving between
 *   tabs never builds up a back stack, and back from any tab returns home.
 * @param labels Tab labels, in bar order.
 * @param icons Tab icons, in the same order.
 * @param routes Tab routes, in the same order. Null for a tab whose screen isn't built yet.
 */
internal class RoleTabs(
    val home: String,
    val labels: List<String>,
    val icons: List<ImageVector>,
    val routes: List<String?>,
) {
    init {
        require(labels.size == icons.size && icons.size == routes.size) {
            "labels, icons and routes must all be the same size"
        }
    }

    /** The tab showing [route], or -1 if it isn't one of this role's tabs. */
    fun indexOf(route: String?): Int = if (route == null) -1 else routes.indexOf(route)

    /** True on any screen where this role's bar is shown: home, or one of the tabs. */
    fun showsBarOn(route: String?): Boolean = route != null && (route == home || indexOf(route) >= 0)

    companion object {
        /** Explore is the entrant's home, so it's also the first tab. */
        val ENTRANT =
            RoleTabs(
                home = Routes.HOME,
                labels = listOf("Explore", "Reservations", "Lotteries", "Messages", "Notifications"),
                icons = listOf(AppIcons.Home, AppIcons.Reservation, AppIcons.LotteryTicket, AppIcons.Chat, AppIcons.BellIcon),
                routes = listOf(Routes.HOME, Routes.RESERVATIONS, Routes.ENTRANT_LOTTERIES, null, Routes.ENTRANT_NOTIFICATIONS),
            )

        /** Like the entrant bar, the owner's home is the first tab. */
        val OWNER =
            RoleTabs(
                home = Routes.OWNER_HOME,
                labels = listOf("Home", "Reservations", "Messages", "Notifications", "Lotteries"),
                icons = listOf(AppIcons.Home, AppIcons.Reservation, AppIcons.Chat, AppIcons.BellIcon, AppIcons.LotteryTicket),
                routes = listOf(Routes.OWNER_HOME, null, null, Routes.OWNER_NOTIFICATIONS, Routes.OWNER_LOTTERIES),
            )

        /** Like the entrant bar, the admin's home is the first tab. */
        val ADMIN =
            RoleTabs(
                home = Routes.ADMIN_HOME,
                labels = listOf("Home", "Messages", "Profiles", "Notifications", "Images"),
                icons = listOf(AppIcons.Home, AppIcons.Chat, AppIcons.Profile, AppIcons.BellIcon, AppIcons.AddPhoto),
                routes = listOf(Routes.ADMIN_HOME, null, null, Routes.ADMIN_NOTIFICATIONS, null),
            )

        val ALL = listOf(ENTRANT, OWNER, ADMIN)

        /** The role whose bar belongs on [route], or null for a screen without a bar. */
        fun forRoute(route: String?): RoleTabs? = ALL.firstOrNull { it.showsBarOn(route) }
    }
}
