package com.example.daemon_expedite.frontend

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    // Owner
    const val OWNER_HOME = "owner_home"
    const val OWNER_PROFILE = "owner_profile"
    const val START_LOTTERY = "start_lottery"
    const val REGISTER_CAMPSITE = "register_campsite"
    const val SELECT_LOCATION = "select_location" // opened from Register campsite only

    // Not built yet. When a screen is added, give it a route here and replace the
    // matching `showComingSoon(...)` call below with `navController.navigate(...)`.
    // const val CAMPSITE_DETAIL = "campsite/{campsiteId}"
    // const val RESERVATIONS = "reservations"
    // const val LOTTERIES = "lotteries"
    // const val MESSAGES = "messages"
    // const val NOTIFICATIONS = "notifications"
    // const val POLICIES = "policies"
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
 * Splash ─► Home ─► Entrant profile ─┬─► Edit profile ─► Edit photo
 *                                    ├─► Edit photo
 *                                    └─► Owner home ─┬─► Owner profile (role toggle: Entrant / Owner / Admin*)
 *                                                    ├─► Start lottery (Lotteries tab)
 *                                                    └─► Register campsite ─► Select location (map)
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
    fun switchRole(role: ProfileRole) = when (role) {
        ProfileRole.ENTRANT -> navController.popBackStack(Routes.HOME, inclusive = false)
        ProfileRole.OWNER -> navController.navigate(Routes.OWNER_HOME) {
            popUpTo(Routes.HOME) // keep Entrant home underneath, drop the profile pages
        }
        // TODO: navigate to the admin home once it exists (only offered when isAdmin is true).
        ProfileRole.ADMIN -> showComingSoon("Admin view")
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
                // TODO: navigate to Routes.CAMPSITE_DETAIL with the id once that screen exists.
                onCampsiteClick = { showComingSoon("Campsite details") },
                onProfileClick = { navController.navigate(Routes.ENTRANT_PROFILE) },
                onReservationsClick = { showComingSoon("Reservations") },
                onLotteriesClick = { showComingSoon("Lotteries") },
                onMessagesClick = { showComingSoon("Messages") },
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
                onPoliciesClick = { showComingSoon("Policies") },
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
            val pickPhoto = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
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
                onCampsiteClick = { showComingSoon("Campsite details") },
                onProfileClick = { navController.navigate(Routes.OWNER_PROFILE) },
                onRegisterCampsiteClick = { navController.navigate(Routes.REGISTER_CAMPSITE) },
                onReservationsClick = { showComingSoon("Reservations") },
                onMessagesClick = { showComingSoon("Messages") },
                onNotificationsClick = { showComingSoon("Notifications") },
                // TEMPORARY: Start Lottery has no other entry point yet. In the Figma it is
                // reached from a campsite's owner view; move it there when that screen exists.
                onLotteriesClick = { navController.navigate(Routes.START_LOTTERY) },
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

        composable(Routes.START_LOTTERY) {
            // TODO: the form doesn't report its values yet. When it gets an onSubmit callback,
            //  build a Lottery from them and save it with Database.addLottery (already exposed),
            //  then add it to the owner with Owner.addOwnerLottery.
            StartLotteryScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.REGISTER_CAMPSITE) {
            // Scoped to this screen, so leaving it (save, cancel or back) throws the form away.
            // The map screen below borrows the same instance to fill in the location.
            val form: RegisterCampsiteViewModel = viewModel()
            val draft by form.draft.collectAsState()
            val errors by form.errors.collectAsState()
            val saving by form.isSaving.collectAsState()
            val owner by ownerViewModel.owner.collectAsState()

            // System photo picker, no storage permission needed. Asks for no more than the
            // photos still allowed (the multi-picker needs a limit of at least 2).
            val remaining = RegisterCampsiteViewModel.MAX_PHOTOS - draft.photos.size
            val pickPhotos = rememberLauncherForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(maxOf(remaining, 2))
            ) { uris -> form.addPhotos(uris) }
            val pickPhoto = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri -> uri?.let { form.addPhotos(listOf(it)) } }

            RegisterCampsiteScreen(
                draft = draft,
                errors = errors,
                isSaving = saving,
                onDraftChange = form::update,
                onAddressChange = form::onAddressChange,
                onOccupancyChange = form::changeOccupancy,
                onPriceChange = form::onPriceChange,
                onAddAmenity = form::addAmenity,
                onRemoveAmenity = form::removeAmenity,
                onChooseOnMapClick = { navController.navigate(Routes.SELECT_LOCATION) },
                onAddPhotosClick = {
                    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    if (remaining > 1) pickPhotos.launch(request) else if (remaining == 1) pickPhoto.launch(request)
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
                                    toast("Campsite registered")
                                    ownerViewModel.refreshCampsites()
                                    navController.popBackStack()
                                }
                                SubmitResult.INVALID -> toast("Some details are missing. Check the form.")
                                SubmitResult.FAILED ->
                                    toast("Couldn't register campsite. Check your connection and try again.")
                            }
                        }
                    }
                },
                onCancelClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.SELECT_LOCATION) { entry ->
            // Same form as the Register campsite screen underneath.
            val registerEntry = remember(entry) { navController.getBackStackEntry(Routes.REGISTER_CAMPSITE) }
            val form: RegisterCampsiteViewModel = viewModel(registerEntry)
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
private fun SplashRoute(state: SessionState, onRetry: () -> Unit, onReady: () -> Unit) {
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .clickable(onClick = onRetry)
                    .padding(12.dp),
            )
        }
    }
}
