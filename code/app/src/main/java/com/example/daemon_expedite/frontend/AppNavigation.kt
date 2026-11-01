package com.example.daemon_expedite.frontend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.daemon_expedite.backend.Database
import com.example.daemon_expedite.backend.User
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import kotlin.coroutines.cancellation.CancellationException

/** Route names for every screen in the app. Add new screens here. */
object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
}

private const val TAG = "AppNavigation"

/**
 * Root of the app's navigation.
 *
 * Starts on the splash screen, which signs in and loads (or creates) the user. Once that
 * finishes it moves to the home screen and removes the splash screen from the back stack,
 * so pressing back on home closes the app instead of going back to the splash screen.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashRoute(
                onReady = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            EntrantHomeScreen()
        }
    }
}

/**
 * Shows [InitLoadScreen] while the database starts up and the user is loaded.
 *
 * If something fails (for example there is no internet), a message is shown at the bottom of
 * the screen and tapping it tries again.
 *
 * @param onReady Called once the signed-in user is available.
 */
@Composable
private fun SplashRoute(onReady: (User) -> Unit) {
    var failed by remember { mutableStateOf(false) }
    // Changing this restarts the LaunchedEffect below, which retries the start-up.
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        failed = false
        try {
            Database.init()
            onReady(loadOrCreateUser())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Start-up failed", e)
            failed = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        InitLoadScreen()
        if (failed) {
            Text(
                text = "Couldn't connect. Tap to try again.",
                style = AppTextStyles.BodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .clickable { attempt++ }
                    .padding(12.dp),
            )
        }
    }
}

/**
 * Returns the user linked to this device's Firebase sign-in, creating one on first launch.
 *
 * [Database.fetchOwnUser] throws when no user exists yet, which is how a first launch is
 * detected.
 */
private suspend fun loadOrCreateUser(): User =
    try {
        Database.fetchOwnUser()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.i(TAG, "No existing user found, creating a new one", e)
        User(notificationsEnabled = true).also { Database.addUser(it) }
    }
