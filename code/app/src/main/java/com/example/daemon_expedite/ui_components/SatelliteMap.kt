package com.example.daemon_expedite.ui_components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Shared pieces for the satellite map screens (Select location, Map view).
 *
 * The map itself is an osmdroid [MapView] shown with `AndroidView`. Wrap it in a parent with
 * `clipToBounds()`: osmdroid draws tiles outside its own bounds otherwise.
 */

// region Map

/** Credit line that must be shown on any screen using [rememberSatelliteMapView]. */
const val SATELLITE_MAP_ATTRIBUTION = "Imagery © Esri, Maxar, Earthstar Geographics"

/**
 * Creates the osmdroid [MapView] once, showing satellite tiles with place names on top, and
 * pauses/resumes it with the screen so it stops downloading tiles in the background.
 *
 * @param center Where the map starts.
 * @param zoom Starting zoom level (3 to 19).
 */
@Composable
fun rememberSatelliteMapView(center: GeoPoint, zoom: Double): MapView {
    val context = LocalContext.current
    val mapView = remember {
        // osmdroid needs its settings loaded (tile cache location, user agent) before a map is made.
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(EsriWorldImagery)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER) // we draw our own
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0)
            overlays.add(
                TilesOverlay(MapTileProviderBasic(context, EsriPlaceNames), context).apply {
                    loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                    loadingLineColor = android.graphics.Color.TRANSPARENT
                }
            )
            controller.setZoom(zoom)
            controller.setCenter(center)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach() // also stops the place-name layer's tile downloads
        }
    }
    return mapView
}

/** Esri World Imagery. Its URLs are `.../tile/{z}/{y}/{x}` (y before x, unlike OSM). */
private object EsriWorldImagery : OnlineTileSourceBase(
    "EsriWorldImagery", 0, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
    "Esri, Maxar, Earthstar Geographics, and the GIS User Community",
) {
    override fun getTileURLString(index: Long): String = esriUrl(baseUrl, index)
}

/** Esri's transparent place-name and boundary layer, drawn over the satellite tiles. */
private object EsriPlaceNames : OnlineTileSourceBase(
    "EsriPlaceNames", 0, 19, 256, ".png",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/"),
    "Esri",
) {
    override fun getTileURLString(index: Long): String = esriUrl(baseUrl, index)
}

private fun esriUrl(base: String, index: Long) =
    "$base${MapTileIndex.getZoom(index)}/${MapTileIndex.getY(index)}/${MapTileIndex.getX(index)}"

// endregion

// region Controls

/**
 * Zoom in / zoom out buttons with a locate button underneath, from the Figma map screens.
 *
 * @param onZoomIn Called when + is tapped.
 * @param onZoomOut Called when − is tapped.
 * @param onLocate Called when the locate button is tapped.
 * @param locateDescription What the locate button does, read out by screen readers.
 * @param modifier [Modifier] applied to the column of controls.
 * @param locating True while a location lookup is running; dims the locate icon.
 */
@Composable
fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocate: () -> Unit,
    locateDescription: String,
    modifier: Modifier = Modifier,
    locating: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val controlModifier = Modifier
        .shadow(28.dp, shape, ambientColor = AppColors.MapControlShadow, spotColor = AppColors.MapControlShadow)
        .background(AppColors.SurfaceMapControl, shape)
        .border(1.dp, AppColors.OutlineStrong, shape)
        .clip(shape)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = controlModifier) {
            MapControlButton(AppIcons.Plus, "Zoom in", onZoomIn, height = 42)
            HorizontalDivider(modifier = Modifier.width(44.dp), thickness = 1.dp, color = AppColors.OutlineSubtle)
            MapControlButton(AppIcons.Minus, "Zoom out", onZoomOut, height = 42)
        }
        Box(modifier = controlModifier) {
            MapControlButton(
                AppIcons.MyLocation,
                locateDescription,
                onLocate,
                height = 44,
                tint = if (locating) AppColors.Secondary else AppColors.TextOnPrimary,
            )
        }
    }
}

@Composable
private fun MapControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    height: Int,
    tint: Color = AppColors.TextOnPrimary,
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = height.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

// endregion

// region Pin

/** Size of [MapPinMarker]. */
val MAP_PIN_WIDTH = 92.dp
val MAP_PIN_HEIGHT = 106.dp

/** How far the pin's tip is from the left and top of [MapPinMarker]. Line this point up with the location. */
val MAP_PIN_TIP_X = 46.dp
val MAP_PIN_TIP_Y = 90.dp

/**
 * The campsite pin from the Figma "Pin anchor": radius and halo rings, the green pin, a stem,
 * and a tip dot. The tip is at ([MAP_PIN_TIP_X], [MAP_PIN_TIP_Y]).
 */
@Composable
fun MapPinMarker(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = MAP_PIN_WIDTH, height = MAP_PIN_HEIGHT)) {
        Box(
            Modifier
                .offset(4.dp, 4.dp)
                .size(84.dp)
                .background(AppColors.MapSelectionRadius, CircleShape)
                .border(1.dp, AppColors.OutlineAccent, CircleShape)
        )
        Box(
            Modifier
                .offset(17.dp, 17.dp)
                .size(58.dp)
                .background(AppColors.MapSelectionHalo, CircleShape)
                .border(1.dp, AppColors.OutlineAccent, CircleShape)
        )
        Box(
            Modifier
                .offset(44.dp, 65.dp)
                .size(width = 4.dp, height = 23.dp)
                .background(AppColors.TextOnPrimary, CircleShape)
        )
        Box(
            Modifier
                .offset(40.dp, 84.dp)
                .size(12.dp)
                .background(AppColors.Primary, CircleShape)
                .border(2.dp, AppColors.TextOnPrimary, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(26.dp, 26.dp)
                .size(40.dp)
                .shadow(14.dp, CircleShape)
                .background(AppColors.Primary, CircleShape)
                .border(2.dp, AppColors.TextOnPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.MapPin, contentDescription = "Campsite pin", tint = AppColors.TextOnPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

// endregion
