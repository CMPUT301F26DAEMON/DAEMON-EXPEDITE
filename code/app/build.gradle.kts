import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.dokka") version "2.3.0"
    id("com.google.gms.google-services")
}

// TURN relay for voice calls (voip/CallManager), read from local.properties so the credentials
// stay out of git. Each teammate adds their own; without them calls use STUN only.
val localProperties =
    Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }

fun localString(key: String): String = "\"" + (localProperties.getProperty(key) ?: "").replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.daemon_expedite"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.daemon_expedite"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "TURN_URLS", localString("turn.urls"))
        buildConfigField("String", "TURN_USERNAME", localString("turn.username"))
        buildConfigField("String", "TURN_PASSWORD", localString("turn.password"))
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // android.util.Log (and other framework stubs) return defaults instead of throwing in JVM
    // unit tests, so error-handling paths that log can be tested.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Push notifications (lottery results, campsite owner messages). See push/ExpediteMessagingService.
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.storage)
    implementation(libs.coil.compose)
    // Coil 3 can't download images from URLs without a network module (it fails silently).
    // Needed for profile and campsite photos, which are loaded from Firebase Storage URLs.
    implementation(libs.coil.network.okhttp)
    // OpenStreetMap map view, used with Esri satellite tiles on the Select location screen.
    implementation(libs.osmdroid.android)
    implementation(libs.geofire.android.common)
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)
    // Voice calls between entrants and owners in chats (Google's WebRTC, packaged). See voip/CallManager.
    implementation(libs.stream.webrtc.android)
    // Face tracking for the face filters in video calls. On-device model bundled in the app: no
    // account, no network. See voip/filters/FaceTracker.
    implementation(libs.mlkit.face.detection)
}
