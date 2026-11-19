// AI use: some functions in this file were written with help from Anthropic, Claude Code,
// "Implement the campsite details, policies and map screens from our Figma design", 2026-11-02.
// The design came from our team; we reviewed, edited and tested the code.

package com.example.daemon_expedite.frontend

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.daemon_expedite.ui_components.AppColors
import com.example.daemon_expedite.ui_components.AppTextStyles
import com.example.daemon_expedite.ui_components.MAP_PIN_TIP_X
import com.example.daemon_expedite.ui_components.MAP_PIN_TIP_Y
import com.example.daemon_expedite.ui_components.MapControls
import com.example.daemon_expedite.ui_components.MapPinMarker
import com.example.daemon_expedite.ui_components.SATELLITE_MAP_ATTRIBUTION
import com.example.daemon_expedite.ui_components.ScreenHeader
import com.example.daemon_expedite.ui_components.rememberSatelliteMapView
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint

/**
 * Read-only satellite map with a pin on the campsite. Opened from "View on map" on Campsite Details.
 *
 * Built from the "Map view" frame in the Figma file. The map can be dragged and zoomed, and the
 * pin stays on the campsite. The locate button brings the campsite back to the middle.
 *
 * @param latitude Campsite latitude.
 * @param longitude Campsite longitude.
 * @param onBackClick Called when the back button is tapped.
 */
@Composable
fun CampsiteMapScreen(
    latitude: Double,
    longitude: Double,
    onBackClick: () -> Unit,
) {
    val density = LocalDensity.current
    val campsite = remember(latitude, longitude) { GeoPoint(latitude, longitude) }
    val mapView = rememberSatelliteMapView(campsite, CAMPSITE_ZOOM)

    // Where the campsite is on screen, in pixels inside the map. Null until the map is laid out.
    var pinPosition by remember { mutableStateOf<IntOffset?>(null) }

    // Move the pin whenever the map is dragged, zoomed or resized.
    DisposableEffect(mapView, campsite) {
        fun updatePin() {
            if (mapView.width == 0) return
            val point = mapView.projection.toPixels(campsite, null)
            pinPosition = IntOffset(point.x, point.y)
        }
        val mapListener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                updatePin()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean = onScroll(null)
        }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updatePin() }
        mapView.addMapListener(mapListener)
        mapView.addOnLayoutChangeListener(layoutListener)
        mapView.post { updatePin() }
        onDispose {
            mapView.removeMapListener(mapListener)
            mapView.removeOnLayoutChangeListener(layoutListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = "Map view", onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // osmdroid draws outside its bounds unless clipped, which would cover the header.
                .clipToBounds(),
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            pinPosition?.let { position ->
                // Line the pin's tip up with the campsite.
                val tipX = with(density) { MAP_PIN_TIP_X.roundToPx() }
                val tipY = with(density) { MAP_PIN_TIP_Y.roundToPx() }
                MapPinMarker(Modifier.offset { IntOffset(position.x - tipX, position.y - tipY) })
            }

            MapControls(
                onZoomIn = { mapView.controller.zoomIn() },
                onZoomOut = { mapView.controller.zoomOut() },
                onLocate = { mapView.controller.animateTo(campsite, CAMPSITE_ZOOM, RECENTER_MS) },
                locateDescription = "Centre on campsite",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 104.dp, end = 24.dp),
            )

            Text(
                text = SATELLITE_MAP_ATTRIBUTION,
                style = AppTextStyles.FieldLabel,
                color = AppColors.MapLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 8.dp),
            )
        }
    }
}

private const val CAMPSITE_ZOOM = 15.0
private const val RECENTER_MS = 600L
