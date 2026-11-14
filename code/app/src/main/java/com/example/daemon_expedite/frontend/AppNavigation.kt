// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Add navigation routes for the new screens", 2026-11-01 to 2026-11-05.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import android.widget.Toast
import com.example.daemon_expedite.ui_components.BottomNavBar
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daemon_expedite.R
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.Id
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles

/** Route names for every screen in the app. Add new screens here. */
object Routes {
    const val SPLASH = "splash"

    // Entrant
    const val HOME = "home"
    const val ENTRANT_PROFILE = "entrant_profile"
    const val EDIT_PROFILE = "edit_profile"
    const val EDIT_PROFILE_PHOTO = "edit_profile_photo"
    const val CAMPSITE_ID_ARG = "campsiteId"
    const val CAMPSITE_DETAIL = "campsite/{$CAMPSITE_ID_ARG}"
    const val RESERVATIONS = "reservations"
    const val ENTRANT_LOTTERIES = "entrant_lotteries"
    const val ENTRANT_NOTIFICATIONS = "entrant_notifications"

    const val CAMPSITE_POLICIES = "campsite/{$CAMPSITE_ID_ARG}/policies"

    // Map view takes the coordinates in the route, so both details screens can open it.
    const val LATITUDE_ARG = "latitude"
    const val LONGITUDE_ARG = "longitude"
    const val CAMPSITE_MAP = "campsite_map/{$LATITUDE_ARG}/{$LONGITUDE_ARG}"

    /** The [CAMPSITE_DETAIL] route for one campsite. */
    fun campsiteDetail(id: Id) = "campsite/${id.value}"

    fun campsitePolicies(id: Id) = "campsite/${id.value}/policies"

    fun campsiteMap(
        latitude: Double,
        longitude: Double,
    ) = "campsite_map/$latitude/$longitude"

    // Owner
    const val OWNER_HOME = "owner_home"
    const val OWNER_PROFILE = "owner_profile"
    const val OWNER_LOTTERIES = "owner_lotteries"
    const val OWNER_NOTIFICATIONS = "owner_notifications"
    const val LOTTERY_ID_ARG = "lotteryId"

    // A lottery winner's booking screen. The campsite is the one picked first.
    const val BOOK_LOTTERY_STAY = "lottery/{$LOTTERY_ID_ARG}/book/{$CAMPSITE_ID_ARG}"
    fun bookLotteryStay(lotteryId: Id, campsiteId: Id) = "lottery/${lotteryId.value}/book/${campsiteId.value}"

    const val OWNER_LOTTERY_DETAIL = "owner_lottery/{$LOTTERY_ID_ARG}"

    const val EDIT_LOTTERY = "owner_lottery/{$LOTTERY_ID_ARG}/edit"

    const val OWNER_LOTTERY_WAITLIST = "owner_lottery/{$LOTTERY_ID_ARG}/waitlist"

    fun ownerLotteryDetail(id: Id) = "owner_lottery/${id.value}"

    fun editLottery(id: Id) = "owner_lottery/${id.value}/edit"

    fun ownerLotteryWaitlist(id: Id) = "owner_lottery/${id.value}/waitlist"

    const val CREATE_LOTTERY = "create_lottery" // opened from My Lotteries only
    const val REGISTER_CAMPSITE = "register_campsite"
    const val SELECT_LOCATION = "select_location" // opened from Register / Edit campsite only
    const val OWNER_CAMPSITE_DETAIL = "owner_campsite/{$CAMPSITE_ID_ARG}"
    const val EDIT_CAMPSITE = "owner_campsite/{$CAMPSITE_ID_ARG}/edit"

    fun ownerCampsiteDetail(id: Id) = "owner_campsite/${id.value}"

    fun editCampsite(id: Id) = "owner_campsite/${id.value}/edit"

    // Admin (only reachable for users in the "admins" collection)
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_PROFILE = "admin_profile"
    const val ADMIN_NOTIFICATIONS = "admin_notifications"
    const val ADMIN_CAMPSITE_DETAIL = "admin_campsite/{$CAMPSITE_ID_ARG}"

    fun adminCampsiteDetail(id: Id) = "admin_campsite/${id.value}"

    // Not built yet. When a screen is added, give it a route here and replace the
    // matching `showComingSoon(...)` call below with `navController.navigate(...)`.
    // const val LOTTERIES = "lotteries"
    // const val MESSAGES = "messages"
    const val POLICIES = "policies"
}

/**
 * Root of the app's navigation. Every screen is connected here.
 *
 * How data flows:
 * - The signed-in user lives in [SessionViewModel], shared by every screen. Screens read the
 *   user from it and change the user only through its functions, which also save to the database.
 * - Screens with their own data get their own ViewModel ([HomeViewModel], [OwnerViewModel]).
 * - Screen composables only draw what they are given and report taps through callbacks.
 *   This file turns those callbacks into ViewModel calls or navigation.
 *
 * Flow:
 * ```
 * Splash ─► Home ─┬─► Campsite details ─┬─► Map view
 *                 │                     └─► Campsite policies
 *                 ├─► My reservations
 *                 ├─► My lotteries
 *                 ├─► Notifications
 *                 └─► Entrant profile ─┬─► Edit profile ─► Edit photo
 *                                      ├─► Edit photo
 *                                      ├─► Owner home ─┬─► Owner profile (role toggle: Entrant / Owner / Admin*)
 *                                      │               ├─► Owner campsite details ─┬─► Map view
 *                                      │               │                           └─► Edit campsite ─► Select location
 *                                      │               ├─► Notifications
 *                                      │               ├─► My lotteries ─┬─► Lottery details ─┬─► Owner campsite details
 *                                      │               │                 │                   └─► Edit lottery
 *                                      │               │                 └─► Create lottery
 *                                      │               └─► Register campsite ─► Select location (map)
 *                                      └─► Admin home* ─┬─► Admin profile (role toggle)
 *                                                       ├─► Notifications
 *                                                       └─► Admin campsite details ─┬─► Map view
 *                                                                                   └─► Campsite policies
 * ```
 * All three profile pages have the role toggle: Entrant / Owner, plus Admin for users found in
 * the "admins" collection at start-up (see [SessionState.Ready.isAdmin]). The toggle on any of
 * them can open any role's home; the tree above only shows the first way in.
 *
 * @param session Created here by default; pass one in for tests.
 */
@Composable
fun AppNavigation(session: SessionViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionState by session.state.collectAsState()
    val ready = sessionState as? SessionState.Ready
    // Null while loading. Screens show empty values until the user arrives.
    val user = ready?.user
    val photoUrl = ready?.profileImageUrl
    val isAdmin = ready?.isAdmin ?: false

    // Shared by the owner screens. Created here so all of them see the same owner.
    val ownerViewModel: OwnerViewModel = viewModel()

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun showComingSoon(screen: String) = toast("$screen isn't built yet")

    /**
     * Switches to [role]'s home screen from any profile page.
     *
     * Entrant home is always at the bottom of the back stack, so switching to it pops back
     * there; switching to Owner or Admin replaces whatever is above Entrant home.
     */
    fun switchRole(role: ProfileRole) =
        when (role) {
            ProfileRole.ENTRANT -> {
                navController.popBackStack(Routes.HOME, inclusive = false)
            }

            ProfileRole.OWNER -> {
                navController.navigate(Routes.OWNER_HOME) {
                    popUpTo(Routes.HOME) // keep Entrant home underneath, drop the profile pages
                }
            }

            // Only offered by the toggle when isAdmin is true.
            ProfileRole.ADMIN -> {
                navController.navigate(Routes.ADMIN_HOME) {
                    popUpTo(Routes.HOME) // keep Entrant home underneath, drop the profile pages
                }
            }
        }

    /**
     * Opens tab [index] of [tabs]. Each role's home is at the bottom of that role's part of the
     * back stack, so a tab that is the home pops back there; the other tabs sit directly above
     * it, so moving between tabs never builds up a stack.
     */
    fun openTab(tabs: RoleTabs, index: Int) {
        val route = tabs.routes.getOrNull(index) ?: return showComingSoon(tabs.labels[index])
        // Already there. Navigating anyway would pop this tab and open a fresh copy (popUpTo
        // runs before launchSingleTop checks the top), replaying the slide.
        if (navController.currentDestination?.route == route) return
        if (route == tabs.home) {
            navController.popBackStack(tabs.home, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(tabs.home)
                launchSingleTop = true
            }
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    // The role whose bar belongs on this screen, or null for screens without one.
    val barTabs = RoleTabs.forRoute(currentRoute)

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = Routes.SPLASH,
            // Slide for opening/going back, zoom for role switches, fade from splash (NavTransitions.kt).
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
        ) {
            composable(Routes.SPLASH) {
                SplashRoute(
                    state = sessionState,
                    onRetry = session::retry,
                    onReady = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }

            // region Entrant

            composable(Routes.HOME) {
                val home: HomeViewModel = viewModel()
                // Runs every time Home comes back on screen (for example after switching back from
                // the owner view), so newly registered campsites appear without restarting the app.
                LaunchedEffect(Unit) { home.onScreenShown() }
                // Hides the signed-in user's own campsites. The splash screen waits for the user,
                // so this is set before the first search finishes.
                LaunchedEffect(user) { home.setViewer(user?.id) }
                val query by home.query.collectAsState()
                val campsites by home.campsites.collectAsState()
                EntrantHomeScreen(
                    profileImageUrl = photoUrl,
                    userName = user?.name.orEmpty(),
                    searchQuery = query,
                    onSearchQueryChange = home::onQueryChange,
                    campsites = campsites,
                    onRetry = home::retry,
                    onCampsiteClick = { id -> navController.navigate(Routes.campsiteDetail(id)) },
                    onFilterClick = home::onFilterClick,
                    onProfileClick = { navController.navigate(Routes.ENTRANT_PROFILE) },
                )
            }

            composable(Routes.ENTRANT_LOTTERIES) {
                val lotteries: EntrantLotteriesViewModel = viewModel()
                // Runs each time this screen comes back, so a lottery joined meanwhile shows up.
                LaunchedEffect(user) { user?.let(lotteries::load) }
                val state by lotteries.lotteries.collectAsState()
                var leavingId by remember { mutableStateOf<Id?>(null) }
                EntrantLotteriesScreen(
                    state = state,
                    leavingId = leavingId,
                    onRetry = lotteries::retry,

                    onLeaveConfirmed = { lotteryId ->
                        leavingId = lotteryId
                        session.leaveLottery(lotteryId) { result ->
                            leavingId = null
                            when (result) {
                                LotteryActionResult.DONE -> {
                                    toast("You left the waiting list")
                                    lotteries.removeFromInProgress(lotteryId)
                                }

                                LotteryActionResult.CLOSED -> {
                                    toast("Entries for this lottery are closed, so you can't leave now.")
                                }

                                LotteryActionResult.FULL, LotteryActionResult.FAILED -> {
                                    toast("Something went wrong. Check your connection and try again.")
                                }
                            }
                        }
                    },
                )
            }

            composable(Routes.RESERVATIONS) {
                val reservations: EntrantReservationsViewModel = viewModel()
                // Waits for the user to load, since the reservation ids are stored on the user.
                LaunchedEffect(user) { user?.let(reservations::load) }
                val state by reservations.reservations.collectAsState()
                val cancellingId by reservations.cancellingId.collectAsState()
                val ratingId by reservations.ratingId.collectAsState()
                EntrantReservationsScreen(
                    state = state,
                    cancellingId = cancellingId,
                    ratingId = ratingId,
                    onRetry = reservations::retry,

                    onCancelConfirmed = { id ->
                        reservations.cancel(id) { problem -> toast(problem ?: "Reservation cancelled") }
                    },
                    onRatingSubmitted = { id, stars, review ->
                        reservations.rate(id, stars, review) { problem -> toast(problem ?: "Thanks for rating your stay") }
                    },
                )
            }

            composable(Routes.CAMPSITE_DETAIL) {
                // Reads the campsite id from the route through its SavedStateHandle.
                val detail: CampsiteDetailViewModel = viewModel()
                // Runs each time the page comes back (for example after booking), and keeps the old
                // details on screen while it reloads.
                LaunchedEffect(user) { user?.let(detail::refresh) }
                val state by detail.details.collectAsState()
                var lotteryBusy by remember { mutableStateOf(false) }

                /** Joins or leaves, then reloads the page so the card shows the new state. */
                fun lotteryAction(join: Boolean) {
                    val lotteryId = (state as? UiState.Content)?.data?.lotteryId ?: return
                    lotteryBusy = true
                    val action = if (join) session::joinLottery else session::leaveLottery
                    action(lotteryId) { result ->
                        lotteryBusy = false
                        when (result) {
                            LotteryActionResult.DONE -> {
                                toast(if (join) "You're on the waiting list" else "You left the waiting list")
                            }

                            LotteryActionResult.FULL -> {
                                toast("Sorry, the waiting list just filled up.")
                            }

                            LotteryActionResult.CLOSED -> {
                                toast("Entries for this lottery are closed.")
                            }

                            LotteryActionResult.FAILED -> {
                                toast("Something went wrong. Check your connection and try again.")
                            }
                        }
                        session.user?.let(detail::refresh)
                    }
                }

                CampsiteDetailScreen(
                    state = state,
                    isLotteryBusy = lotteryBusy,
                    onBackClick = { navController.popBackStack() },
                    onRetry = detail::retry,
                    onViewMapClick = {
                        (state as? UiState.Content)?.let {
                            navController.navigate(Routes.campsiteMap(it.data.latitude, it.data.longitude))
                        }
                    },
                    onPoliciesClick = {
                        (state as? UiState.Content)?.let { navController.navigate(Routes.campsitePolicies(it.data.id)) }
                    },
                    // TODO: open the campsite reviews page (Figma "Pine Creek Hideaway · Reviews (v2)") once it's built.
                    onReviewsClick = { showComingSoon("Reviews") },
                    onJoinLotteryClick = { lotteryAction(join = true) },
                    onLeaveLotteryClick = { lotteryAction(join = false) },
                    onBookClick = {
                        (state as? UiState.Content)?.data?.let { details ->
                            details.lotteryId?.let { navController.navigate(Routes.bookLotteryStay(it, details.id)) }
                        }
                    },
                    // TODO: open messages once they're built.
                    onMessageOwnerClick = { showComingSoon("Messages") },
                )
            }

            composable(Routes.CAMPSITE_MAP) { entry ->
                val args = entry.arguments
                CampsiteMapScreen(
                    latitude = args?.getString(Routes.LATITUDE_ARG)?.toDoubleOrNull() ?: 0.0,
                    longitude = args?.getString(Routes.LONGITUDE_ARG)?.toDoubleOrNull() ?: 0.0,
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Routes.CAMPSITE_POLICIES) { entry ->
                // Loads the campsite named in the route, so any screen can link to its policies.
                val campsiteId = remember(entry) { entry.arguments?.getString(Routes.CAMPSITE_ID_ARG).toIdOrNull() }
                val policies by produceState<String?>(null, campsiteId) {
                    value = campsiteId?.let { id ->
                        quietly("Couldn't load policies for $id") { Database.fetchCampsite(id).policies }
                    }.orEmpty()
                }
                CampsitePoliciesScreen(
                    policies = policies,
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Routes.BOOK_LOTTERY_STAY) {
                // Reads the lottery and first campsite from the route.
                val booking: BookingViewModel = viewModel()
                val options by booking.options.collectAsState()
                val selection by booking.selection.collectAsState()
                val declining by booking.isDeclining.collectAsState()
                var isBooking by remember { mutableStateOf(false) }
                val viewer = user?.id
                BookingScreen(
                    state = options,
                    selection = selection,
                    problem = viewer?.let { booking.problem(it) },
                    isBooking = isBooking,
                    isDeclining = declining,
                    onBackClick = { navController.popBackStack() },
                    onRetry = booking::load,
                    onCampsiteClick = booking::selectCampsite,
                    onDatesChange = booking::onDatesChange,
                    onAgreeChange = booking::setAgreedToRules,
                    onPoliciesClick = { id -> navController.navigate(Routes.campsitePolicies(id)) },
                    onReserveClick = {
                        val lotteryId = (options as? UiState.Content)?.data?.lottery?.id
                        val campsite = selection.campsite
                        val period = selection.period
                        if (lotteryId != null && campsite != null && period != null) {
                            isBooking = true
                            session.bookLotteryStay(lotteryId, campsite, period) { problem ->
                                isBooking = false
                                if (problem == null) {
                                    toast("Booked! See you at camp.")
                                    navController.popBackStack() // the campsite page refreshes when shown
                                } else {
                                    toast(problem)
                                    booking.load() // someone may have taken those nights
                                }
                            }
                        }
                    },
                    onDeclineConfirmed = {
                        if (viewer != null) {
                            booking.decline(viewer) { problem ->
                                if (problem == null) {
                                    toast("You declined this lottery")
                                    navController.popBackStack()
                                } else {
                                    toast(problem)
                                }
                            }
                        }
                    },
                )
            }

            composable(Routes.ENTRANT_PROFILE) {
                EntrantProfileDetailsScreen(
                    name = user?.name.orEmpty(),
                    email = user?.email.orEmpty(),
                    phone = user?.phoneNumber.orEmpty(),
                    photoUrl = photoUrl,
                    notificationsEnabled = user?.notificationsEnabled ?: true,
                    onBackClick = { navController.popBackStack() },
                    onEditPhotoClick = { navController.navigate(Routes.EDIT_PROFILE_PHOTO) },
                    onUpdateProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                    isAdmin = isAdmin,
                    onRoleSelected = { switchRole(it) },
                    onPoliciesClick = { navController.navigate(Routes.POLICIES) },
                    onNotificationsChanged = { enabled ->
                        session.setNotificationsEnabled(enabled) { ok ->
                            if (!ok) toast("Couldn't save notification setting")
                        }
                    },
                    onDeleteProfileClick = {
                        // The screen already asked "are you sure?" before calling this.
                        session.deleteProfile { ok ->
                            if (ok) navController.restartAtSplash() else toast("Couldn't delete profile")
                        }
                    },
                )
            }

            composable(Routes.POLICIES) {
                val context = LocalContext.current
                val mdText =
                    remember(context) {
                        loadMarkdownFromAsset(context, "app_policies.md")
                    }
                AppPolicyScreen(
                    markdownText = mdText,
                    onBackClick = { navController.popBackStack() },
                )
            }

            composable(Routes.ENTRANT_NOTIFICATIONS) {
                // A tab, like the other entrant tabs, so it has no back button.
                NotificationsTab(userId = user?.id)
            }

            composable(Routes.EDIT_PROFILE) {
                var saving by remember { mutableStateOf(false) }
                EditProfileScreen(
                    profileImageUrl = photoUrl,
                    name = user?.name.orEmpty(),
                    email = user?.email.orEmpty(),
                    phoneNumber = user?.phoneNumber.orEmpty(),
                    isSaving = saving,
                    onBackClick = { navController.popBackStack() },
                    onCancelClick = { navController.popBackStack() },
                    onProfilePhotoClick = { navController.navigate(Routes.EDIT_PROFILE_PHOTO) },
                    onSaveClick = { name, email, phone ->
                        saving = true
                        session.updateProfile(name, email, phone) { ok ->
                            saving = false
                            if (ok) navController.popBackStack() else toast("Couldn't save your profile")
                        }
                    },
                )
            }

            composable(Routes.EDIT_PROFILE_PHOTO) {
                // Opens the system photo picker. No storage permission is needed for this.
                val pickPhoto =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.PickVisualMedia(),
                    ) { uri ->
                        if (uri == null) return@rememberLauncherForActivityResult // user cancelled
                        session.uploadProfilePhoto(uri) { ok ->
                            if (ok) navController.popBackStack() else toast("Couldn't upload photo")
                        }
                    }
                EditProfilePhotoScreen(
                    profileImageUrl = photoUrl,
                    name = user?.name.orEmpty(),
                    onBackClick = { navController.popBackStack() },
                    onChoosePhotoClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onDeletePhotoClick = {
                        session.removeProfilePhoto { ok ->
                            if (ok) navController.popBackStack() else toast("Couldn't remove photo")
                        }
                    },
                )
            }

            // endregion

            // region Owner

            composable(Routes.OWNER_HOME) {
                // Load (or create) the owner once the user is available.
                LaunchedEffect(user) { user?.let(ownerViewModel::load) }
                val query by ownerViewModel.query.collectAsState()
                val campsites by ownerViewModel.campsites.collectAsState()
                CampsiteOwnerMainPage(
                    profileImageUrl = photoUrl,
                    searchQuery = query,
                    onSearchQueryChange = ownerViewModel::onQueryChange,
                    campsites = campsites,
                    onRetry = { user?.let { ownerViewModel.load(it, force = true) } },
                    onCampsiteClick = { id -> navController.navigate(Routes.ownerCampsiteDetail(id)) },
                    onProfileClick = { navController.navigate(Routes.OWNER_PROFILE) },
                    onRegisterCampsiteClick = { navController.navigate(Routes.REGISTER_CAMPSITE) },
                )
            }

            composable(Routes.OWNER_NOTIFICATIONS) {
                // A tab: the bar's Home tab goes home, so there's no back button.
                NotificationsTab(userId = user?.id)
            }

            composable(Routes.OWNER_PROFILE) {
                ProfileDetailsScreen(
                    profileImageUrl = photoUrl,
                    name = user?.name.orEmpty(),
                    email = user?.email.orEmpty(),
                    phoneNumber = user?.phoneNumber.orEmpty(),
                    isAdmin = isAdmin,
                    // Read-only page: photo and details are changed from the entrant profile.
                    onBackClick = { navController.popBackStack() },
                    onRoleSelected = { switchRole(it) },
                    onDeleteProfile = {
                        // Owner page: delete only the owner side, then go back to being an entrant.
                        ownerViewModel.deleteOwnerProfile { ok ->
                            if (ok) {
                                toast("Owner profile deleted")
                                switchRole(ProfileRole.ENTRANT)
                            } else {
                                toast("Couldn't delete owner profile")
                            }
                        }
                    },
                )
            }

            composable(Routes.OWNER_LOTTERIES) {
                val lotteries: OwnerLotteriesViewModel = viewModel()
                val owner by ownerViewModel.owner.collectAsState()
                val loadedOwner = (owner as? UiState.Content)?.data
                // Runs each time this screen comes back, so a newly created lottery shows up.
                LaunchedEffect(loadedOwner) { loadedOwner?.let(lotteries::load) }
                val state by lotteries.lotteries.collectAsState()
                OwnerLotteriesScreen(
                    state = state,
                    onRetry = lotteries::retry,
                    onLotteryClick = { id -> navController.navigate(Routes.ownerLotteryDetail(id)) },
                    onCreateClick = { navController.navigate(Routes.CREATE_LOTTERY) },
                )
            }

            composable(Routes.OWNER_LOTTERY_WAITLIST) {
                val waitlist: LotteryWaitingListViewModel = viewModel()
                // Runs each time the page comes back, so new entrants show up.
                LaunchedEffect(Unit) { waitlist.load() }
                val state by waitlist.entrants.collectAsState()
                val sending by waitlist.isSending.collectAsState()
                val owner by ownerViewModel.owner.collectAsState()
                LotteryWaitingList(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onRetry = waitlist::load,
                    onMessageClick = { showComingSoon("Messages") },
                    isSendingNotification = sending,
                    onSendNotification = { title, text, onResult ->
                        val loadedOwner = (owner as? UiState.Content)?.data
                        // The owner's id is the user's id, so either works as the sender.
                        val senderId = loadedOwner?.id ?: user?.id
                        if (senderId == null) {
                            onResult(false)
                        } else {
                            waitlist.sendNotification(senderId, title, text) { sent ->
                                if (sent != null) {
                                    // Already saved; keep the in-memory owner in step so a later
                                    // updateOwner doesn't drop it.
                                    loadedOwner?.addSentNotification(sent)
                                    toast("Notification sent")
                                } else {
                                    toast("Couldn't send notification")
                                }
                                onResult(sent != null)
                            }
                        }
                    },
                )
            }

            composable(Routes.OWNER_LOTTERY_DETAIL) {
                val detail: OwnerLotteryDetailViewModel = viewModel()
                // Runs each time the page comes back, so edits show up.
                LaunchedEffect(Unit) { detail.load() }
                val state by detail.details.collectAsState()
                val cancelling by detail.isCancelling.collectAsState()
                OwnerLotteryDetailScreen(
                    state = state,
                    isCancelling = cancelling,
                    onBackClick = { navController.popBackStack() },
                    onRetry = detail::load,
                    onCampsiteClick = { id -> navController.navigate(Routes.ownerCampsiteDetail(id)) },
                    onViewWaitlistClick = {
                        (state as? UiState.Content)?.let { navController.navigate(Routes.ownerLotteryWaitlist(it.data.id)) }
                    },
                    onEditClick = {
                        (state as? UiState.Content)?.let { navController.navigate(Routes.editLottery(it.data.id)) }
                    },
                    onCancelConfirmed = {
                        detail.cancel { result ->
                            when (result) {
                                CancelResult.DELETED -> {
                                    toast("Lottery deleted")
                                    navController.popBackStack() // My Lotteries reloads when shown
                                }

                                CancelResult.CANCELLED -> {
                                    toast("Lottery cancelled")
                                    detail.load() // now shows as Cancelled
                                }

                                CancelResult.TOO_LATE -> {
                                    toast("Stays have started, so this lottery can't be cancelled any more.")
                                    detail.load()
                                }

                                CancelResult.FAILED -> {
                                    toast("Couldn't cancel the lottery. Check your connection and try again.")
                                }
                            }
                        }
                    },
                )
            }

            composable(Routes.CREATE_LOTTERY) {
                // Scoped to this screen, so leaving it throws the form away.
                val form: CreateLotteryViewModel = viewModel()
                LotteryFormRoute(navController, form, ownerViewModel, toast = ::toast)
            }

            composable(Routes.EDIT_LOTTERY) {
                // Reads the lottery id from the route, so it knows which lottery to edit.
                val form: CreateLotteryViewModel = viewModel()
                LaunchedEffect(Unit) { form.startEditing() }
                val load by form.editLoad.collectAsState()
                when (val loaded = load) {
                    UiState.Loading -> CenteredSpinner()
                    is UiState.Error -> CenteredMessage("${loaded.message}. Tap to try again.", onClick = form::startEditing)
                    is UiState.Content -> LotteryFormRoute(navController, form, ownerViewModel, toast = ::toast)
                }
            }

            composable(Routes.OWNER_CAMPSITE_DETAIL) {
                val detail: OwnerCampsiteDetailViewModel = viewModel()
                val owner by ownerViewModel.owner.collectAsState()
                val loadedOwner = (owner as? UiState.Content)?.data
                // Runs each time this screen comes back (for example after Edit campsite), so saved
                // changes show up. The old details stay on screen while it reloads.
                LaunchedEffect(loadedOwner) { loadedOwner?.let(detail::load) }
                val state by detail.details.collectAsState()
                val deleting by detail.isDeleting.collectAsState()
                OwnerCampsiteDetailScreen(
                    state = state,
                    isDeleting = deleting,
                    onBackClick = { navController.popBackStack() },
                    onRetry = detail::retry,
                    onViewMapClick = {
                        (state as? UiState.Content)?.let {
                            navController.navigate(Routes.campsiteMap(it.data.latitude, it.data.longitude))
                        }
                    },
                    onViewWaitlistClick = {
                        // The campsite's current lottery, if it's in one that isn't finished or cancelled.
                        val lotteryId = (state as? UiState.Content)?.data?.lotteryId
                        if (lotteryId != null) {
                            navController.navigate(Routes.ownerLotteryWaitlist(lotteryId))
                        } else {
                            toast("This campsite isn't in a lottery right now")
                        }
                    },
                    onEditClick = {
                        (state as? UiState.Content)?.let { navController.navigate(Routes.editCampsite(it.data.id)) }
                    },
                    onDeleteConfirmed = {
                        detail.delete { result ->
                            when (result) {
                                DeleteResult.DELETED -> {
                                    toast("Campsite deleted")
                                    // Reload the owner so their campsite list no longer has it.
                                    user?.let { ownerViewModel.load(it, force = true) }
                                    navController.popBackStack()
                                }

                                DeleteResult.LOTTERY_RUNNING -> {
                                    toast("This campsite has a lottery running. Delete it after the lottery ends.")
                                }

                                DeleteResult.FAILED -> {
                                    toast("Couldn't delete campsite. Check your connection and try again.")
                                }
                            }
                        }
                    },
                )
            }

            composable(Routes.REGISTER_CAMPSITE) {
                // Scoped to this screen, so leaving it (save, cancel or back) throws the form away.
                // The map screen borrows the same instance to fill in the location.
                val form: RegisterCampsiteViewModel = viewModel()
                CampsiteFormRoute(
                    navController = navController,
                    form = form,
                    ownerViewModel = ownerViewModel,
                    isEditing = false,
                    toast = ::toast,
                )
            }

            composable(Routes.EDIT_CAMPSITE) { entry ->
                val form: RegisterCampsiteViewModel = viewModel()
                val campsiteId = remember(entry) { entry.arguments?.getString(Routes.CAMPSITE_ID_ARG).toIdOrNull() }
                LaunchedEffect(campsiteId) { campsiteId?.let { form.startEditing(it) } }
                val load by form.editLoad.collectAsState()
                when {
                    campsiteId == null -> {
                        CenteredMessage(CAMPSITE_NOT_FOUND)
                    }

                    load is UiState.Loading -> {
                        CenteredSpinner()
                    }

                    load is UiState.Error -> {
                        CenteredMessage(
                            "${(load as UiState.Error).message}. Tap to try again.",
                            onClick = { form.startEditing(campsiteId, force = true) },
                        )
                    }

                    else -> {
                        CampsiteFormRoute(
                            navController = navController,
                            form = form,
                            ownerViewModel = ownerViewModel,
                            isEditing = true,
                            toast = ::toast,
                        )
                    }
                }
            }

            composable(Routes.SELECT_LOCATION) { entry ->
                // Same form as the Register or Edit campsite screen underneath.
                val formEntry = remember(entry) { navController.previousBackStackEntry ?: entry }
                val form: RegisterCampsiteViewModel = viewModel(formEntry)
                val draft by form.draft.collectAsState()
                SelectLocationScreen(
                    campsiteName = draft.name,
                    initialLatitude = draft.latitude,
                    initialLongitude = draft.longitude,
                    onConfirm = { latitude, longitude, address ->
                        form.setLocation(latitude, longitude, address)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }

            // endregion

            // region Admin

            composable(Routes.ADMIN_HOME) {
                // Same campsite list and search as Entrant home, with its own HomeViewModel
                // (scoped to this screen, so the admin's search doesn't change Entrant home's).
                // No setViewer here: admins see every campsite, their own included.
                val home: HomeViewModel = viewModel()
                // Runs every time Admin home comes back, so deleted campsites disappear.
                LaunchedEffect(Unit) { home.onScreenShown() }
                val query by home.query.collectAsState()
                val campsites by home.campsites.collectAsState()
                AdminHomeScreen(
                    profileImageUrl = photoUrl,
                    searchQuery = query,
                    onSearchQueryChange = home::onQueryChange,
                    campsites = campsites,
                    onRetry = home::retry,
                    onCampsiteClick = { id -> navController.navigate(Routes.adminCampsiteDetail(id)) },
                    onFilterClick = home::onFilterClick,
                    onProfileClick = { navController.navigate(Routes.ADMIN_PROFILE) },
                )
            }

            composable(Routes.ADMIN_NOTIFICATIONS) {
                // A tab: the bar's Home tab goes home, so there's no back button.
                NotificationsTab(userId = user?.id)
            }

            composable(Routes.ADMIN_PROFILE) {
                // The owner's read-only profile page, with Admin selected and no delete button.
                ProfileDetailsScreen(
                    profileImageUrl = photoUrl,
                    name = user?.name.orEmpty(),
                    email = user?.email.orEmpty(),
                    phoneNumber = user?.phoneNumber.orEmpty(),
                    isAdmin = isAdmin,
                    currentRole = ProfileRole.ADMIN,
                    onBackClick = { navController.popBackStack() },
                    onRoleSelected = { switchRole(it) },
                    onDeleteProfile = null,
                )
            }

            composable(Routes.ADMIN_CAMPSITE_DETAIL) {
                // Reads the campsite id from the route through its SavedStateHandle.
                val detail: AdminCampsiteDetailViewModel = viewModel()
                LaunchedEffect(Unit) { detail.load() }
                val state by detail.details.collectAsState()
                val deleting by detail.isDeleting.collectAsState()

                /** Shows [message] and goes back to Admin home, which reloads when shown. */
                fun onDeleted(message: String) {
                    toast(message)
                    // The admin may have deleted their own campsite or owner profile. Reload the
                    // shared owner data so the owner screens don't keep showing it.
                    user?.takeIf { it.id == detail.ownerId }?.let { ownerViewModel.load(it, force = true) }
                    navController.popBackStack()
                }

                AdminCampsiteDetailScreen(
                    state = state,
                    isDeleting = deleting,
                    onBackClick = { navController.popBackStack() },
                    onRetry = detail::load,
                    onViewMapClick = {
                        (state as? UiState.Content)?.let {
                            navController.navigate(Routes.campsiteMap(it.data.latitude, it.data.longitude))
                        }
                    },
                    onPoliciesClick = {
                        (state as? UiState.Content)?.let { navController.navigate(Routes.campsitePolicies(it.data.id)) }
                    },
                    onDeleteCampsiteConfirmed = {
                        detail.deleteCampsite { ok ->
                            if (ok) onDeleted("Campsite deleted") else toast("Couldn't delete campsite. Check your connection and try again.")
                        }
                    },
                    onDeleteOwnerConfirmed = {
                        detail.deleteOwner { ok ->
                            if (ok) onDeleted("Owner deleted") else toast("Couldn't delete owner. Check your connection and try again.")
                        }
                    },
                )
            }

            // endregion
        }

        // One bar for every role, drawn over the NavHost so it stays put while that role's home
        // and tab screens slide underneath it. It slides away when a screen without a bar opens.
        AnimatedVisibility(
            visible = barTabs != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(TAB_BAR_MS)) { it },
            exit = slideOutVertically(tween(TAB_BAR_MS)) { it },
        ) {
            RoleBottomNavBar(tabs = barTabs, selectedIndex = barTabs?.indexOf(currentRoute) ?: -1, onTabClick = ::openTab)
        }
    }
}

/**
 * The Register / Edit campsite form with its photo pickers and save handling. [form] decides
 * whether it registers a new campsite or updates one (see [RegisterCampsiteViewModel.startEditing]).
 */
@Composable
private fun CampsiteFormRoute(
    navController: NavHostController,
    form: RegisterCampsiteViewModel,
    ownerViewModel: OwnerViewModel,
    isEditing: Boolean,
    toast: (String) -> Unit,
) {
    val draft by form.draft.collectAsState()
    val errors by form.errors.collectAsState()
    val saving by form.isSaving.collectAsState()
    val owner by ownerViewModel.owner.collectAsState()

    // System photo picker, no storage permission needed. Asks for no more than the
    // photos still allowed (the multi-picker needs a limit of at least 2).
    val remaining = RegisterCampsiteViewModel.MAX_PHOTOS - draft.photos.size
    val pickPhotos =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxOf(remaining, 2)),
        ) { uris -> form.addPhotos(uris) }
    val pickPhoto =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri -> uri?.let { form.addPhotos(listOf(it)) } }

    RegisterCampsiteScreen(
        draft = draft,
        errors = errors,
        isSaving = saving,
        isEditing = isEditing,
        onDraftChange = form::update,
        onAddressChange = form::onAddressChange,
        onOccupancyChange = form::changeOccupancy,
        onPriceChange = form::onPriceChange,
        onAddAmenity = form::addAmenity,
        onRemoveAmenity = form::removeAmenity,
        onChooseOnMapClick = { navController.navigate(Routes.SELECT_LOCATION) },
        onAddPhotosClick = {
            val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            if (remaining > 1) {
                pickPhotos.launch(request)
            } else if (remaining == 1) {
                pickPhoto.launch(request)
            }
        },
        onRemovePhoto = form::removePhoto,
        onRegisterClick = {
            val loadedOwner = (owner as? UiState.Content)?.data
            if (loadedOwner == null) {
                toast("Your owner profile is still loading. Try again in a moment.")
            } else {
                form.submit(loadedOwner) { result ->
                    when (result) {
                        SubmitResult.SAVED -> {
                            toast(if (isEditing) "Changes saved" else "Campsite registered")
                            ownerViewModel.refreshCampsites()
                            navController.popBackStack()
                        }

                        SubmitResult.INVALID -> {
                            toast("Some details are missing. Check the form.")
                        }

                        SubmitResult.FAILED -> {
                            toast(
                                if (isEditing) {
                                    "Couldn't save changes. Check your connection and try again."
                                } else {
                                    "Couldn't register campsite. Check your connection and try again."
                                },
                            )
                        }
                    }
                }
            }
        },
        onCancelClick = { navController.popBackStack() },
        onBackClick = { navController.popBackStack() },
    )
}

/**
 * The Create / Edit lottery form with its save handling. [form] decides whether it creates a
 * new lottery or edits one (see [CreateLotteryViewModel.isEditing]).
 */
@Composable
private fun LotteryFormRoute(
    navController: NavHostController,
    form: CreateLotteryViewModel,
    ownerViewModel: OwnerViewModel,
    toast: (String) -> Unit,
) {
    val owner by ownerViewModel.owner.collectAsState()
    val loadedOwner = (owner as? UiState.Content)?.data
    // The owner's campsites, as already loaded for Owner Home.
    val ownedCampsites by ownerViewModel.campsites.collectAsState()
    LaunchedEffect(loadedOwner, ownedCampsites) {
        val list = (ownedCampsites as? UiState.Content)?.data
        if (loadedOwner != null && list != null) form.load(loadedOwner, list)
    }
    val options by form.options.collectAsState()
    val draft by form.draft.collectAsState()
    val problems by form.problems.collectAsState()
    val saving by form.isSaving.collectAsState()
    val locks by form.locks.collectAsState()
    val ownedError = ownedCampsites as? UiState.Error
    val editing = form.isEditing

    CreateLotteryScreen(
        draft = draft,
        // If the owner's campsites failed to load, show that instead of loading forever.
        campsiteOptions = ownedError ?: options,
        problems = problems,
        isSaving = saving,
        today = form.today,
        isEditing = editing,
        locks = locks,
        onBackClick = { navController.popBackStack() },
        onRetryCampsites = { if (ownedError != null) ownerViewModel.refreshCampsites() else form.retry() },
        onToggleCampsite = form::toggleCampsite,
        onEntryFirstDayChange = form::setEntryFirstDay,
        onEntryLastDayChange = form::setEntryLastDay,
        onAddStayPeriod = form::addStayPeriod,
        onRemoveStayPeriod = form::removeStayPeriod,
        onWinnerCountChange = form::onWinnerCountChange,
        onWaitlistCapacityChange = form::onWaitlistCapacityChange,
        onCreateClick = {
            if (loadedOwner == null) {
                toast("Your owner profile is still loading. Try again in a moment.")
            } else {
                form.submit(loadedOwner) { result ->
                    when (result) {
                        SubmitResult.SAVED -> {
                            toast(if (editing) "Changes saved" else "Lottery created")
                            navController.popBackStack() // the page underneath reloads when shown
                        }

                        SubmitResult.INVALID -> {
                            toast("Some details need fixing. Check the form.")
                        }

                        SubmitResult.FAILED -> {
                            toast(
                                if (editing) {
                                    "Couldn't save changes. Check your connection and try again."
                                } else {
                                    "Couldn't create the lottery. Check your connection and try again."
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun CenteredSpinner() {
    Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.Primary)
    }
}

@Composable
private fun CenteredMessage(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = AppTextStyles.BodyMedium,
            color = AppColors.TextSecondary,
            modifier =
                Modifier
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(24.dp),
        )
    }
}

/**
 * Goes back to the splash screen and clears everything behind it, so back can't return to
 * screens for a profile that no longer exists. Used after deleting the profile.
 */
/**
 * The bottom bar for [tabs]' role (see [RoleTabs]).
 *
 * While the bar slides away, [tabs] is null; it keeps showing the last role and tab until it's
 * gone, so the bar doesn't empty out or its highlight jump as it leaves.
 */
@Composable
private fun RoleBottomNavBar(
    tabs: RoleTabs?,
    selectedIndex: Int,
    onTabClick: (RoleTabs, Int) -> Unit,
) {
    var shown by remember { mutableStateOf(tabs?.let { it to selectedIndex }) }
    SideEffect { if (tabs != null) shown = tabs to selectedIndex }
    val (shownTabs, shownIndex) = shown ?: return
    // A fresh bar per role, so the highlight doesn't slide over from another role's tab.
    key(shownTabs) {
        BottomNavBar(
            labels = shownTabs.labels,
            icons = shownTabs.icons,
            onClicks = shownTabs.labels.indices.map { index -> { onTabClick(shownTabs, index) } },
            selectedIndex = shownIndex,
        )
    }
}

private fun NavHostController.restartAtSplash() {
    navigate(Routes.SPLASH) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/**
 * Shows [InitLoadScreen] while the user loads, then calls [onReady].
 *
 * If loading fails (for example there is no internet), a message is shown at the bottom of
 * the screen and tapping it calls [onRetry].
 */
@Composable
private fun SplashRoute(
    state: SessionState,
    onRetry: () -> Unit,
    onReady: () -> Unit,
) {
    LaunchedEffect(state is SessionState.Ready) {
        if (state is SessionState.Ready) onReady()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        InitLoadScreen()
        if (state is SessionState.Failed) {
            Text(
                text = "Couldn't connect. Tap to try again.",
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .clickable(onClick = onRetry)
                        .padding(12.dp),
            )
        }
    }
}

/**
 * The Notifications tab for every role. Reloads each time it's opened, so new notifications
 * show up with a green dot; opening it marks them read (see [NotificationsViewModel]).
 *
 * @param userId The signed-in user, or null while they are still loading.
 */
@Composable
private fun NotificationsTab(userId: Id?) {
    val vm: NotificationsViewModel = viewModel()
    LaunchedEffect(userId) { userId?.let(vm::load) }
    val state by vm.notifications.collectAsState()
    NotificationsScreen(
        state = state,
        onBackClick = null,
        onRetry = { userId?.let(vm::load) },
    )
}
