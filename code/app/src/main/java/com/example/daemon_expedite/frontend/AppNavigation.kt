package com.example.daemon_expedite.frontend

import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daemon_expedite.R
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

    // Opened from Campsite details only; it reads the campsite that screen already loaded.
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
    const val LOTTERY_ID_ARG = "lotteryId"
    const val OWNER_LOTTERY_DETAIL = "owner_lottery/{$LOTTERY_ID_ARG}"

    const val EDIT_LOTTERY = "owner_lottery/{$LOTTERY_ID_ARG}/edit"

    fun ownerLotteryDetail(id: Id) = "owner_lottery/${id.value}"

    fun editLottery(id: Id) = "owner_lottery/${id.value}/edit"

    const val CREATE_LOTTERY = "create_lottery" // opened from My Lotteries only
    const val REGISTER_CAMPSITE = "register_campsite"
    const val SELECT_LOCATION = "select_location" // opened from Register / Edit campsite only
    const val OWNER_CAMPSITE_DETAIL = "owner_campsite/{$CAMPSITE_ID_ARG}"
    const val EDIT_CAMPSITE = "owner_campsite/{$CAMPSITE_ID_ARG}/edit"

    fun ownerCampsiteDetail(id: Id) = "owner_campsite/${id.value}"

    fun editCampsite(id: Id) = "owner_campsite/${id.value}/edit"

    // Not built yet. When a screen is added, give it a route here and replace the
    // matching `showComingSoon(...)` call below with `navController.navigate(...)`.
    // const val RESERVATIONS = "reservations"
    // const val LOTTERIES = "lotteries"
    // const val MESSAGES = "messages"
    // const val NOTIFICATIONS = "notifications"
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
 *                 └─► Entrant profile ─┬─► Edit profile ─► Edit photo
 *                                      ├─► Edit photo
 *                                      └─► Owner home ─┬─► Owner profile (role toggle: Entrant / Owner / Admin*)
 *                                                      ├─► Owner campsite details ─┬─► Map view
 *                                                      │                           └─► Edit campsite ─► Select location
 *                                                      ├─► My lotteries ─┬─► Lottery details ─┬─► Owner campsite details
 *                                                      │                 │                   └─► Edit lottery
 *                                                      │                 └─► Create lottery
 *                                                      └─► Register campsite ─► Select location (map)
 * ```
 * Both profile pages have the role toggle: Entrant / Owner, plus Admin for users found in the
 * "admins" collection at start-up (see [SessionState.Ready.isAdmin]).
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
     * Switches to [role]'s home screen from either profile page.
     *
     * Entrant home is always at the bottom of the back stack, so switching to it pops back
     * there; switching to Owner replaces whatever is above Entrant home.
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

            // TODO: navigate to the admin home once it exists (only offered when isAdmin is true).
            ProfileRole.ADMIN -> {
                showComingSoon("Admin view")
            }
        }

    NavHost(
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
                searchQuery = query,
                onSearchQueryChange = home::onQueryChange,
                campsites = campsites,
                onRetry = home::retry,
                onCampsiteClick = { id -> navController.navigate(Routes.campsiteDetail(id)) },
                onFilterClick = home::onFilterClick,
                onProfileClick = { navController.navigate(Routes.ENTRANT_PROFILE) },
                onReservationsClick = { showComingSoon("Reservations") },
                onLotteriesClick = { showComingSoon("Lotteries") },
                onMessagesClick = { showComingSoon("Messages") },
            )
        }

        composable(Routes.CAMPSITE_DETAIL) {
            // Reads the campsite id from the route through its SavedStateHandle.
            val detail: CampsiteDetailViewModel = viewModel()
            LaunchedEffect(user) { user?.let(detail::load) }
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
                onJoinLotteryClick = { lotteryAction(join = true) },
                onLeaveLotteryClick = { lotteryAction(join = false) },
                // TODO: open the booking screen (next PR).
                onBookClick = { showComingSoon("Booking") },
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
            val details = rememberCampsiteDetails(navController, entry)
            CampsitePoliciesScreen(
                policies = details?.policies.orEmpty(),
                onBackClick = { navController.popBackStack() },
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
                onBackClick = { navController.popBackStack() },
                onChoosePhotoClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                // TODO: taking a photo needs the camera permission and a file to save into.
                onTakePhotoClick = { showComingSoon("Taking a photo") },
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
                onReservationsClick = { showComingSoon("Reservations") },
                onMessagesClick = { showComingSoon("Messages") },
                onNotificationsClick = { showComingSoon("Notifications") },
                onLotteriesClick = { navController.navigate(Routes.OWNER_LOTTERIES) },
            )
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
                onBackClick = { navController.popBackStack() },
                onRetry = lotteries::retry,
                onLotteryClick = { id -> navController.navigate(Routes.ownerLotteryDetail(id)) },
                onCreateClick = { navController.navigate(Routes.CREATE_LOTTERY) },
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
                onViewWaitlistClick = { showComingSoon("Waitlist") },
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
                onViewWaitlistClick = { showComingSoon("Waitlist") },
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
 * The campsite already loaded by the Campsite details screen under [entry] in the back stack,
 * or null if it isn't loaded. The map and policies pages only open once it has loaded.
 */
@Composable
private fun rememberCampsiteDetails(
    navController: NavHostController,
    entry: NavBackStackEntry,
): CampsiteDetails? {
    val detailEntry = remember(entry) { navController.getBackStackEntry(Routes.CAMPSITE_DETAIL) }
    val detail: CampsiteDetailViewModel = viewModel(detailEntry)
    val state by detail.details.collectAsState()
    return (state as? UiState.Content)?.data
}

/**
 * Goes back to the splash screen and clears everything behind it, so back can't return to
 * screens for a profile that no longer exists. Used after deleting the profile.
 */
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
