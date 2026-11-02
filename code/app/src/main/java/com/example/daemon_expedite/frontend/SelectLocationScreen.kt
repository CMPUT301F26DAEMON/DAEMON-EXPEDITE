package com.example.daemon_expedite.frontend

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppIcons
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.GreenButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/** Where the map opens when the campsite has no location yet (University of Alberta, Edmonton). */
private val DEFAULT_CENTER = GeoPoint(53.5232, -113.5263)
private const val DEFAULT_ZOOM = 12.0
private const val PICKED_ZOOM = 17.0

/**
 * "Select location" map, from the Figma frame "Add Campsite Location".
 *
 * The pin stays still and the owner moves the map under it. The spot under the pin's tip is
 * the campsite's location; its address is looked up with Android's [Geocoder] once the map
 * stops moving. The owner can also type a latitude and longitude and press Go to jump there.
 *
 * The map is OpenStreetMap's osmdroid showing Esri World Imagery satellite tiles, with Esri's
 * place-name layer on top. Neither needs an API key. Esri's terms require the attribution line
 * shown above the bottom sheet.
 *
 * @param campsiteName Shown as the sheet heading. Falls back to "Your campsite".
 * @param initialLatitude Spot picked earlier, or null to open on [DEFAULT_CENTER].
 * @param onConfirm Called with the spot under the pin and its address (null if none was found).
 */
@Composable
fun SelectLocationScreen(
    campsiteName: String,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onConfirm: (latitude: Double, longitude: Double, address: String?) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val start = remember {
        if (initialLatitude != null && initialLongitude != null) GeoPoint(initialLatitude, initialLongitude) else null
    }
    val mapView = rememberSatelliteMapView(start ?: DEFAULT_CENTER, if (start != null) PICKED_ZOOM else DEFAULT_ZOOM)

    var center by remember { mutableStateOf(start ?: DEFAULT_CENTER) }
    var address by remember { mutableStateOf<Address?>(null) }
    var lookingUp by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    var placedInitially by remember { mutableStateOf(false) }

    // Typed coordinates. While either field has focus they are left alone; otherwise they
    // follow the map.
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var latFocused by remember { mutableStateOf(false) }
    var lngFocused by remember { mutableStateOf(false) }
    var coordinateError by remember { mutableStateOf<String?>(null) }
    val editingCoordinates = latFocused || lngFocused

    // Pixel row of the pin's tip inside the map, worked out during layout below. Read through
    // rememberUpdatedState so the map listener always sees the latest value.
    var pinTipYState by remember { mutableIntStateOf(0) }
    val pinTipY by rememberUpdatedState(pinTipYState)

    /** The map point exactly under the pin's tip. */
    fun pointUnderPin(): GeoPoint {
        val p = mapView.projection.fromPixels(mapView.width / 2, pinTipY)
        return GeoPoint(p.latitude, p.longitude)
    }

    /**
     * Moves the map so [point] sits exactly under the pin's tip. The pin isn't in the middle of
     * the map (it sits above the sheet), so the map is centred on [point] first and then shifted
     * up by the distance between the middle and the tip.
     */
    fun placeUnderPin(point: GeoPoint, zoom: Double? = null) {
        if (mapView.width == 0 || mapView.height == 0) return
        zoom?.let { mapView.controller.setZoom(it) }
        mapView.controller.setCenter(point) // resets the projection straight away
        val shifted = mapView.projection.fromPixels(mapView.width / 2, mapView.height - pinTipY)
        mapView.controller.setCenter(GeoPoint(shifted.latitude, shifted.longitude))
        center = point
    }

    // Keep `center` (the point under the pin) in step with the map as the owner drags or zooms it.
    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                if (mapView.width > 0 && pinTipY > 0) center = pointUnderPin()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean = onScroll(null)
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    // Look the address up once the map has been still for a moment. Moving again restarts this.
    LaunchedEffect(center) {
        lookingUp = true
        delay(600)
        address = reverseGeocode(context, center.latitude, center.longitude)
        lookingUp = false
    }

    // Show the pin's coordinates in the fields, except while the owner is typing in them.
    LaunchedEffect(center, editingCoordinates) {
        if (!editingCoordinates) {
            latText = String.format(Locale.US, "%.5f", center.latitude)
            lngText = String.format(Locale.US, "%.5f", center.longitude)
            coordinateError = null
        }
    }

    val focusManager = LocalFocusManager.current

    /** Jumps to the typed coordinates, or explains what is wrong with them. */
    fun goToTypedCoordinates() {
        val lat = latText.toDoubleOrNull()
        val lng = lngText.toDoubleOrNull()
        coordinateError = when {
            lat == null || lat !in -90.0..90.0 -> "Latitude must be a number from -90 to 90"
            lng == null || lng !in -180.0..180.0 -> "Longitude must be a number from -180 to 180"
            else -> null
        }
        if (coordinateError != null || lat == null || lng == null) return
        placeUnderPin(GeoPoint(lat, lng), zoom = maxOf(mapView.zoomLevelDouble, PICKED_ZOOM))
        focusManager.clearFocus()
    }

    fun goToCurrentLocation() {
        locating = true
        scope.launch {
            val location = currentLocation(context)
            locating = false
            if (location == null) {
                Toast.makeText(context, "Couldn't find your location. Is location turned on?", Toast.LENGTH_SHORT).show()
            } else {
                placeUnderPin(GeoPoint(location.latitude, location.longitude), zoom = PICKED_ZOOM)
            }
        }
    }

    val requestLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) goToCurrentLocation()
        else Toast.makeText(context, "Location permission is needed to find you on the map", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Select location", onBackClick = onBackClick)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // osmdroid draws its tiles outside its own bounds unless clipped, which covered
                // the header above.
                .clipToBounds(),
        ) {
            // The pin's tip sits halfway between the guidance card and the bottom sheet, so it
            // is never hidden. The sheet's height is measured without the keyboard padding, so
            // opening the keyboard doesn't move the pin.
            val topClear = with(density) { 94.dp.roundToPx() }
            val bottomClear = sheetHeightPx + with(density) { 12.dp.roundToPx() }
            val heightPx = constraints.maxHeight
            pinTipYState = (topClear + (heightPx - bottomClear)) / 2

            // Once the sheet has been measured, put the starting point under the pin.
            LaunchedEffect(sheetHeightPx > 0) {
                if (sheetHeightPx > 0 && !placedInitially) {
                    mapView.post {
                        placeUnderPin(start ?: DEFAULT_CENTER)
                        placedInitially = true
                    }
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
            )

            // Darkens the top and bottom of the map so the cards stay readable (Figma gradient).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x66161D15),
                            0.35f to Color(0x00161D15),
                            1f to Color(0xA6071008),
                        )
                    ),
            )

            MapGuidance(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 28.dp, end = 28.dp),
            )

            MapControls(
                locating = locating,
                // Zoom around the pin's tip, so the chosen spot stays under it.
                onZoomIn = { mapView.controller.zoomInFixing(mapView.width / 2, pinTipY) },
                onZoomOut = { mapView.controller.zoomOutFixing(mapView.width / 2, pinTipY) },
                onLocate = {
                    val hasPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                    if (hasPermission) goToCurrentLocation()
                    else requestLocation.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 104.dp, end = 24.dp),
            )

            // Pin drawing is 92x106 with its tip 90dp down; line the tip up with pinTipY.
            SelectedPin(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = with(density) { pinTipYState.toDp() } - 90.dp),
            )

            Text(
                text = "Imagery © Esri, Maxar, Earthstar Geographics",
                style = AppTextStyles.FieldLabel,
                color = AppColors.TextOnPrimary.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = with(density) { bottomClear.toDp() } + 4.dp),
            )

            LocationSheet(
                campsiteName = campsiteName,
                address = address,
                lookingUp = lookingUp,
                editingCoordinates = editingCoordinates,
                latitudeText = latText,
                longitudeText = lngText,
                coordinateError = coordinateError,
                onLatitudeChange = { latText = it.coordinateChars(); coordinateError = null },
                onLongitudeChange = { lngText = it.coordinateChars(); coordinateError = null },
                onLatitudeFocus = { latFocused = it },
                onLongitudeFocus = { lngFocused = it },
                onLatitudeNext = { focusManager.moveFocus(FocusDirection.Next) },
                onGo = ::goToTypedCoordinates,
                onConfirm = {
                    if (editingCoordinates) {
                        // Use what was typed rather than silently ignoring it.
                        goToTypedCoordinates()
                        if (coordinateError != null) return@LocationSheet
                    }
                    onConfirm(center.latitude, center.longitude, address?.getAddressLine(0))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 20.dp, end = 17.dp, bottom = 12.dp)
                    // Lift the sheet above the keyboard while typing coordinates.
                    .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                    .onSizeChanged { sheetHeightPx = it.height },
            )
        }
    }
}

/** Keeps only characters that can be part of a coordinate: digits, one leading minus, a dot. */
private fun String.coordinateChars(): String =
    filter { it.isDigit() || it == '.' || it == '-' }.take(12)

// region Map

/**
 * Creates the osmdroid [MapView] once, showing satellite tiles with place names on top, and
 * pauses/resumes it with the screen so it stops downloading tiles in the background.
 */
@Composable
private fun rememberSatelliteMapView(center: GeoPoint, zoom: Double): MapView {
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

// region Overlays

@Composable
private fun MapGuidance(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .shadow(28.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x80071008), spotColor = Color(0x80071008))
            .background(AppColors.SurfaceMapControl, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.OutlineStrong, RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Move, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(19.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Position your campsite", style = AppTextStyles.TitleSmall, color = AppColors.TextOnPrimary)
            Text("Move the map until the pin is exact", style = AppTextStyles.Caption.copy(fontSize = AppTextStyles.Overline.fontSize), color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun MapControls(
    locating: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val controlModifier = Modifier
        .shadow(28.dp, shape, ambientColor = Color(0x80071008), spotColor = Color(0x80071008))
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
                "Use my location",
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

/** The pin from the Figma "Pin anchor": radius and halo rings, the green pin, a stem, and a tip dot. */
@Composable
private fun SelectedPin(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 92.dp, height = 106.dp)) {
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
                .background(Color.White, CircleShape)
        )
        Box(
            Modifier
                .offset(40.dp, 84.dp)
                .size(12.dp)
                .background(AppColors.Primary, CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(26.dp, 26.dp)
                .size(40.dp)
                .shadow(14.dp, CircleShape)
                .background(AppColors.Primary, CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.MapPin, contentDescription = "Campsite pin", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** Bottom sheet: campsite name, the address under the pin, its coordinates, and Confirm. */
@Composable
private fun LocationSheet(
    campsiteName: String,
    address: Address?,
    lookingUp: Boolean,
    editingCoordinates: Boolean,
    latitudeText: String,
    longitudeText: String,
    coordinateError: String?,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onLatitudeFocus: (Boolean) -> Unit,
    onLongitudeFocus: (Boolean) -> Unit,
    onLatitudeNext: () -> Unit,
    onGo: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .shadow(40.dp, shape, ambientColor = Color(0x99071008), spotColor = Color(0x99071008))
            .background(AppColors.SurfaceOverlay, shape)
            .border(1.dp, AppColors.OutlineStrong, shape)
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
    ) {
        val buttonWidth = maxWidth.value.toInt()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 42.dp, height = 4.dp)
                    .background(AppColors.OutlineStrong, CircleShape)
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = campsiteName.ifBlank { "Your campsite" },
                        style = AppTextStyles.HeadingMedium.copy(lineHeight = AppTextStyles.Amount.lineHeight),
                        color = AppColors.TextOnPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (editingCoordinates) "Type coordinates, then press Go" else "Drag the map to refine the pin",
                        style = AppTextStyles.Caption,
                        color = AppColors.TextSecondary,
                    )
                }
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .background(Color(0x33437657), CircleShape)
                        .border(1.dp, AppColors.Primary, CircleShape)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(AppIcons.Tick, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(13.dp))
                    Text("Pin placed", style = AppTextStyles.Badge, color = AppColors.Secondary)
                }
            }

            // Selected address
            val (line1, line2) = addressLines(address, lookingUp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(AppColors.FillSubtle, RoundedCornerShape(14.dp))
                    .border(1.dp, AppColors.OutlineStrong, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Icon(AppIcons.MapPin, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(19.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(line1, style = AppTextStyles.TitleSmall, color = AppColors.TextOnPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(line2, style = AppTextStyles.Badge.copy(fontWeight = AppTextStyles.Overline.fontWeight), color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // Coordinates
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("COORDINATES", style = AppTextStyles.SectionLabel, color = AppColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CoordinateField(
                        label = "Latitude",
                        value = latitudeText,
                        onValueChange = onLatitudeChange,
                        onFocusChange = onLatitudeFocus,
                        imeAction = ImeAction.Next,
                        onAction = onLatitudeNext,
                        isError = coordinateError?.startsWith("Latitude") == true,
                        modifier = Modifier.weight(1f),
                    )
                    CoordinateField(
                        label = "Longitude",
                        value = longitudeText,
                        onValueChange = onLongitudeChange,
                        onFocusChange = onLongitudeFocus,
                        imeAction = ImeAction.Go,
                        onAction = onGo,
                        isError = coordinateError?.startsWith("Longitude") == true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (coordinateError != null) {
                    Text(coordinateError, style = AppTextStyles.Caption, color = AppColors.Danger)
                }
            }

            GreenButton(
                onClick = onConfirm,
                icon = AppIcons.Tick,
                buttonText = "Confirm location",
                customHeight = 58,
                customWidth = buttonWidth,
            )
        }
    }
}

/**
 * Editable latitude or longitude box. Shows the pin's coordinate until tapped; typing a value
 * and pressing Go (on the longitude keyboard) moves the map there.
 *
 * Uses the text keyboard because Android's number keypads often have no minus key, which
 * every longitude in the Americas needs. Other characters are filtered out by the caller.
 */
@Composable
private fun CoordinateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    imeAction: ImeAction,
    onAction: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> AppColors.Danger
        focused -> AppColors.Secondary
        else -> Color(0x24FFFFFF)
    }
    Column(
        modifier = modifier
            .height(60.dp)
            .background(Color(0x12FFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(label.uppercase(), style = AppTextStyles.Overline, color = AppColors.Secondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppTextStyles.TitleSmall.copy(color = AppColors.TextOnPrimary),
            cursorBrush = SolidColor(AppColors.Secondary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = imeAction, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { onAction() }, onGo = { onAction() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    onFocusChange(it.isFocused)
                },
        )
    }
}

/** Splits an address into a street line and a "city, region postcode" line for the sheet. */
private fun addressLines(address: Address?, lookingUp: Boolean): Pair<String, String> {
    if (address == null) {
        return if (lookingUp) "Finding address…" to "Hold the map still for a moment"
        else "No address found here" to "You can type it on the form"
    }
    val parts = address.getAddressLine(0).orEmpty().split(", ")
    return if (parts.size > 2) parts.take(2).joinToString(", ") to parts.drop(2).joinToString(", ")
    else parts.first() to parts.drop(1).joinToString(", ")
}

// endregion

// region Location services

/**
 * Looks up the address at a point with Android's built-in [Geocoder] (no API key). Returns null
 * if there is none, the device has no geocoder, or there is no connection.
 */
private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): Address? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.getDefault())
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        Log.w(TAG, "Reverse geocoding failed: $errorMessage")
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }
    } catch (e: IOException) {
        Log.w(TAG, "Reverse geocoding failed", e)
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Gets a fresh fix from the device (fused provider when available, then GPS, then network),
 * or null after 10 seconds. Only call after location permission has been granted.
 */
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Location? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val providers = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }
    val provider = providers.firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        ?: return null
    return withTimeoutOrNull(10_000) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                if (cont.isActive) cont.resume(location)
            }
        }
    } ?: manager.getLastKnownLocation(provider)
}

private const val TAG = "SelectLocationScreen"

// endregion
