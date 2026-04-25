package com.example.emergency.ui.screen.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.util.Log
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.emergency.ui.theme.EmergencyShapes
import com.example.emergency.ui.theme.EmergencyTheme
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

private const val TAG = "InteractiveMap"
internal val DAM_SQUARE = LatLng(52.3731, 4.8926)

// PDOK basemap only covers the Netherlands; outside this bbox we keep the
// camera anchored to Dam Square instead of flying into a tile-less void
// (most commonly the emulator's default Mountain View location).
internal val NL_BBOX_SW = LatLng(50.7, 3.3)
internal val NL_BBOX_NE = LatLng(53.6, 7.3)

internal fun LatLng.isInNL(): Boolean =
    latitude in NL_BBOX_SW.latitude..NL_BBOX_NE.latitude &&
        longitude in NL_BBOX_SW.longitude..NL_BBOX_NE.longitude

// All POI categories produced by data-pipeline/extract_pois.py. Listed once so
// icon registration, color stops and bottom-card mappings stay in sync.
private val POI_CATEGORIES = listOf(
    "hospital", "doctor", "first_aid", "aed", "pharmacy", "police", "fire",
    "shelter", "water", "toilet", "metro", "parking_underground", "bunker",
    "fuel", "supermarket", "atm", "phone", "school", "community", "worship",
)

private data class Poi(val name: String, val category: String, val lat: Double, val lon: Double)

internal data class RouteResult(
    val polyline: List<LatLng>,
    val distanceM: Double,
    val durationS: Double,
)

private enum class Mode(val brouterProfile: String, val label: String, val icon: ImageVector) {
    Walk("trekking", "Walk", Icons.Default.DirectionsWalk),
    Bike("fastbike", "Bike", Icons.Default.DirectionsBike),
    Drive("car-fast", "Drive", Icons.Default.DirectionsCar),
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "hospital"            -> Icons.Default.LocalHospital
    "aed"                 -> Icons.Default.Favorite
    "pharmacy"            -> Icons.Default.LocalPharmacy
    "police"              -> Icons.Default.LocalPolice
    "fire"                -> Icons.Default.LocalFireDepartment
    "shelter"             -> Icons.Default.Home
    "metro"               -> Icons.Default.DirectionsSubway
    "fuel"                -> Icons.Default.LocalGasStation
    "supermarket"         -> Icons.Default.LocalGroceryStore
    "atm"                 -> Icons.Default.LocalAtm
    "phone"               -> Icons.Default.Phone
    "school"              -> Icons.Default.School
    "parking_underground" -> Icons.Default.LocalParking
    else                  -> Icons.Default.Place
}

private fun categoryColor(category: String): Color = when (category) {
    "hospital"            -> Color(0xFFE53935)
    "doctor"              -> Color(0xFFEC407A)
    "first_aid"           -> Color(0xFFD32F2F)
    "aed"                 -> Color(0xFFFB8C00)
    "pharmacy"            -> Color(0xFF43A047)
    "police"              -> Color(0xFF1E40AF)
    "fire"                -> Color(0xFFB71C1C)
    "shelter"             -> Color(0xFF00897B)
    "water"               -> Color(0xFF29B6F6)
    "toilet"              -> Color(0xFF6D4C41)
    "metro"               -> Color(0xFF5E35B1)
    "parking_underground" -> Color(0xFF455A64)
    "bunker"              -> Color(0xFF424242)
    "fuel"                -> Color(0xFFF9A825)
    "supermarket"         -> Color(0xFF7CB342)
    "atm"                 -> Color(0xFF00ACC1)
    "phone"               -> Color(0xFF8E24AA)
    "school"              -> Color(0xFFFFB300)
    "community"           -> Color(0xFF3949AB)
    "worship"             -> Color(0xFF6A1B9A)
    else                  -> Color(0xFF757575)
}

/**
 * Full-bleed interactive Compose map. Drop it inside any Box/Column and it
 * will fill the available space with a MapLibre view, GPS fix, POI clusters
 * and tap-to-route. Callers are responsible for the surrounding chrome
 * (top bar, status bar padding, etc.).
 *
 * [Mapbox.getInstance] is a singleton initializer and safe to call repeatedly,
 * so we run it on first composition without coordinating with the host
 * Activity.
 */
@Composable
fun InteractiveMap(
    modifier: Modifier = Modifier,
    initialDestination: MapDestination? = null,
) {
    val context = LocalContext.current

    // Idempotent — MapLibre guards internally against re-entry. Keeping it
    // here means callers don't need to remember to bootstrap from Application
    // or MainActivity.
    remember { Mapbox.getInstance(context, null, WellKnownTileServer.MapLibre) }

    var mode by remember { mutableStateOf(Mode.Walk) }
    var selectedPoi by remember {
        mutableStateOf<Poi?>(
            initialDestination?.let { Poi(it.name, it.category, it.lat, it.lon) }
        )
    }
    var routeResult by remember { mutableStateOf<RouteResult?>(null) }
    var routeLoading by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf(DAM_SQUARE) }
    var routeSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var userLocationSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    // Captured once the underlying MapboxMap is ready so other effects can
    // animate the camera (e.g., to the user's GPS fix) without re-entering
    // getMapAsync.
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }

    LaunchedEffect(Unit) {
        getUserLocation(context)?.let {
            userLocation = if (it.isInNL()) it else DAM_SQUARE
            Log.d(
                TAG,
                "GPS fix ${it.latitude},${it.longitude} → using " +
                    "${userLocation.latitude},${userLocation.longitude}" +
                    if (!it.isInNL()) " (out of NL — clamped to Dam Square)" else "",
            )
        } ?: Log.d(TAG, "No GPS — falling back to Dam Square")
    }

    val mapView = remember {
        val options = MapboxMapOptions.createFromAttributes(context).textureMode(true)
        MapView(context, options).apply {
            id = View.generateViewId()
            onCreate(null)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        Log.d(TAG, "getMapAsync requested")
        mapView.getMapAsync { map ->
            Log.d(TAG, "MapboxMap ready; setting style")
            mapboxMap = map
            // Open at city-level zoom on whatever location we currently have
            // (Dam Square fallback). The LaunchedEffect below will animate to
            // the real GPS fix as soon as it resolves.
            map.cameraPosition = CameraPosition.Builder()
                .target(userLocation)
                .zoom(14.0)
                .build()
            map.setStyle(Style.Builder().fromJson(PDOK_BRT_STYLE)) { style ->
                Log.d(TAG, "Style loaded; layers=${style.layers.size}, sources=${style.sources.size}")
                try {
                    addPoiLayer(context, style)
                    routeSource = addRouteLayer(style)
                    userLocationSource = addUserLocationLayer(style)
                    Log.d(TAG, "POI + route + user-location layers attached")
                } catch (e: Exception) {
                    Log.e(TAG, "Layer setup failed", e)
                }
            }
            map.addOnMapClickListener { latLng ->
                val pt = map.projection.toScreenLocation(latLng)
                val touchRect = RectF(pt.x - 30, pt.y - 30, pt.x + 30, pt.y + 30)
                val hits = map.queryRenderedFeatures(touchRect, "pois-layer")
                if (hits.isNotEmpty()) {
                    val f = hits[0]
                    val name = f.getStringProperty("name") ?: "POI"
                    val category = f.getStringProperty("category") ?: "place"
                    val coord = f.geometry() as Point
                    selectedPoi = Poi(name, category, coord.latitude(), coord.longitude())
                    true
                } else false
            }
        }
    }

    // Animate the map to the current GPS fix once both the map is ready and
    // a user location has resolved. The PDOK basemap only covers NL, so if
    // the GPS reports somewhere else we stay on Dam Square — otherwise the
    // user sees a black void of un-tiled ocean.
    LaunchedEffect(mapboxMap, userLocation, initialDestination) {
        val map = mapboxMap ?: return@LaunchedEffect
        val target = if (userLocation.isInNL()) userLocation else DAM_SQUARE
        if (target !== userLocation) {
            Log.d(TAG, "GPS ${userLocation.latitude},${userLocation.longitude} outside NL — using Dam Square")
        }
        val update = if (initialDestination != null) {
            val dest = LatLng(initialDestination.lat, initialDestination.lon)
            val bounds = LatLngBounds.Builder().include(target).include(dest).build()
            // 180px padding leaves room for the top mode selector and the
            // bottom route info card without cropping either endpoint.
            CameraUpdateFactory.newLatLngBounds(bounds, 180)
        } else {
            CameraUpdateFactory.newLatLngZoom(target, 15.0)
        }
        map.animateCamera(update, 800)
    }

    LaunchedEffect(userLocationSource, userLocation) {
        val src = userLocationSource ?: return@LaunchedEffect
        val pt = Point.fromLngLat(userLocation.longitude, userLocation.latitude)
        src.setGeoJson(Feature.fromGeometry(pt))
    }

    // Fetch route whenever (selectedPoi, mode, userLocation) changes.
    LaunchedEffect(selectedPoi, mode, userLocation) {
        val poi = selectedPoi ?: run {
            routeResult = null
            routeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            return@LaunchedEffect
        }
        routeLoading = true
        val result = brouterRoute(userLocation, LatLng(poi.lat, poi.lon), mode.brouterProfile)
        routeLoading = false
        if (result != null && result.polyline.size > 1) {
            routeResult = result
            val pts = result.polyline.map { Point.fromLngLat(it.longitude, it.latitude) }
            routeSource?.setGeoJson(LineString.fromLngLats(pts))
        } else {
            routeResult = null
            Log.e(TAG, "Routing failed for $poi via ${mode.brouterProfile}")
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        ModeSelector(
            current = mode,
            onSelect = { mode = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
        )

        AnimatedVisibility(
            visible = selectedPoi != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RouteInfoCard(
                poi = selectedPoi,
                route = routeResult,
                mode = mode,
                loading = routeLoading,
                onDismiss = { selectedPoi = null },
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ModeSelector(
    current: Mode,
    onSelect: (Mode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Row(
        modifier = modifier
            .clip(EmergencyShapes.full)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.full)
            .padding(4.dp),
    ) {
        Mode.entries.forEach { m ->
            val selected = m == current
            val fg = if (selected) colors.accentInk else colors.text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(EmergencyShapes.full)
                    .background(if (selected) colors.accent else Color.Transparent)
                    .clickable { onSelect(m) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    m.icon,
                    contentDescription = m.label,
                    tint = fg,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = m.label,
                    style = typography.listItem,
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun RouteInfoCard(
    poi: Poi?,
    route: RouteResult?,
    mode: Mode,
    loading: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    poi ?: return
    val colors = EmergencyTheme.colors
    val typography = EmergencyTheme.typography

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(EmergencyShapes.hero)
            .background(colors.surface)
            .border(1.dp, colors.line, EmergencyShapes.hero),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.panel),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    categoryIcon(poi.category),
                    contentDescription = poi.category,
                    tint = categoryColor(poi.category),
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    style = typography.listItem,
                    color = colors.text,
                    maxLines = 2,
                )
                Spacer(Modifier.size(4.dp))
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.textDim,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Calculating route…",
                            style = typography.helper,
                            color = colors.textDim,
                        )
                    }
                    route != null -> Text(
                        text = "${formatDistance(route.distanceM)}  ·  ${formatDuration(route.durationS)} ${mode.label.lowercase()}",
                        style = typography.helper,
                        color = colors.textDim,
                    )
                    else -> Text(
                        text = "Routing failed",
                        style = typography.helper,
                        color = colors.danger,
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textFaint,
                )
            }
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)

private fun formatDuration(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    return if (mins < 60) "$mins min" else "${mins / 60} h ${mins % 60} min"
}

// ─── Map layers ──────────────────────────────────────────────────────────────

// Adds the POI source + layers to the style. POIs are loaded from
// app/src/main/assets/pois-nl.geojson (produced by data-pipeline/extract_pois.py)
// and aggregated client-side via MapLibre clustering — without that, rendering
// 128k+ pins at low zoom levels would stutter.
private fun addPoiLayer(context: Context, style: Style) {
    POI_CATEGORIES.forEach { name ->
        val resId = context.resources.getIdentifier("ic_poi_$name", "drawable", context.packageName)
        if (resId != 0) {
            BitmapFactory.decodeResource(context.resources, resId)?.let { bmp ->
                style.addImage("$name-icon", bmp)
            }
        }
    }

    val options = GeoJsonOptions()
        .withCluster(true)
        .withClusterMaxZoom(13)
        .withClusterRadius(60)
    val source = GeoJsonSource("pois-source", options)
    style.addSource(source)

    // The bundled GeoJSON is ~32 MB; reading it as a Kotlin String would
    // allocate ~128 MB on the Java heap (UTF-16 + StringBuilder doubling) and
    // OOM mid-launch. Instead we stream it to internal storage with a bounded
    // buffer and hand MapLibre a file:// URI so the parse happens natively.
    //
    // We copy once per install (existence check). When the bundled asset is
    // updated, the next reinstall replaces filesDir so the copy refreshes
    // automatically; for in-place dev iteration, clear app data.
    Thread {
        try {
            val outFile = java.io.File(context.filesDir, "pois-nl.geojson")
            if (!outFile.exists()) {
                Log.d(TAG, "Copying pois-nl.geojson from assets…")
                context.assets.open("pois-nl.geojson").use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "Copied pois-nl.geojson (${outFile.length() / 1024} KB) to ${outFile.absolutePath}")
            } else {
                Log.d(TAG, "Reusing existing pois-nl.geojson (${outFile.length() / 1024} KB)")
            }
            // MapLibre expects a triple-slash file URI; File.toURI() yields
            // file:/path on some JVMs which the native loader rejects.
            val uri = "file://${outFile.absolutePath}"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Log.d(TAG, "Setting POI source URI: $uri")
                source.setUri(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage pois-nl.geojson", e)
        }
    }.start()

    val categoryColorExpr = Expression.match(
        Expression.get("category"),
        Expression.literal("#9E9E9E"),
        Expression.stop("hospital",            "#E53935"),
        Expression.stop("doctor",              "#EC407A"),
        Expression.stop("first_aid",           "#D32F2F"),
        Expression.stop("aed",                 "#FB8C00"),
        Expression.stop("pharmacy",            "#43A047"),
        Expression.stop("police",              "#1E40AF"),
        Expression.stop("fire",                "#B71C1C"),
        Expression.stop("shelter",             "#00897B"),
        Expression.stop("water",               "#29B6F6"),
        Expression.stop("toilet",              "#6D4C41"),
        Expression.stop("metro",               "#5E35B1"),
        Expression.stop("parking_underground", "#455A64"),
        Expression.stop("bunker",              "#424242"),
        Expression.stop("fuel",                "#F9A825"),
        Expression.stop("supermarket",         "#7CB342"),
        Expression.stop("atm",                 "#00ACC1"),
        Expression.stop("phone",               "#8E24AA"),
        Expression.stop("school",              "#FFB300"),
        Expression.stop("community",           "#3949AB"),
        Expression.stop("worship",             "#6A1B9A"),
    )

    val unclustered = Expression.not(Expression.has("point_count"))
    val clustered = Expression.has("point_count")

    // Cluster bubble: blue circle that grows with the count.
    val clusterCircle = CircleLayer("clusters-layer", "pois-source").withProperties(
        PropertyFactory.circleColor("#1E88E5"),
        PropertyFactory.circleStrokeColor("#FFFFFF"),
        PropertyFactory.circleStrokeWidth(2f),
        PropertyFactory.circleRadius(
            Expression.step(
                Expression.toNumber(Expression.get("point_count")),
                Expression.literal(16f),
                Expression.stop(50, 22),
                Expression.stop(200, 28),
                Expression.stop(1000, 34),
            )
        ),
    )
    clusterCircle.setFilter(clustered)
    style.addLayer(clusterCircle)

    val clusterCount = SymbolLayer("clusters-count-layer", "pois-source").withProperties(
        PropertyFactory.textField("{point_count_abbreviated}"),
        PropertyFactory.textSize(12f),
        PropertyFactory.textColor("#FFFFFF"),
        PropertyFactory.textAllowOverlap(true),
        PropertyFactory.textIgnorePlacement(true),
    )
    clusterCount.setFilter(clustered)
    style.addLayer(clusterCount)

    // Individual POI: colored circle, only when not part of a cluster.
    val poiCircle = CircleLayer("pois-layer", "pois-source").withProperties(
        PropertyFactory.circleRadius(11f),
        PropertyFactory.circleStrokeWidth(2.5f),
        PropertyFactory.circleStrokeColor("#FFFFFF"),
        PropertyFactory.circleColor(categoryColorExpr),
    )
    poiCircle.setFilter(unclustered)
    style.addLayer(poiCircle)

    val poiIcon = SymbolLayer("pois-icons-layer", "pois-source").withProperties(
        PropertyFactory.iconImage(
            Expression.concat(Expression.get("category"), Expression.literal("-icon"))
        ),
        PropertyFactory.iconSize(0.20f),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true),
    )
    poiIcon.setFilter(unclustered)
    style.addLayer(poiIcon)
}

private fun addRouteLayer(style: Style): GeoJsonSource {
    val source = GeoJsonSource("route-source")
    style.addSource(source)
    style.addLayerBelow(
        LineLayer("route-layer", "route-source").withProperties(
            PropertyFactory.lineColor("#1E88E5"),
            PropertyFactory.lineWidth(5.5f),
            PropertyFactory.lineOpacity(0.9f),
        ),
        "pois-layer",
    )
    return source
}

// Two stacked circles: a translucent halo so the dot stays visible over busy
// basemap colors, and the solid blue puck on top with a white ring (matches
// the standard Maps you-are-here treatment).
private fun addUserLocationLayer(style: Style): GeoJsonSource {
    val source = GeoJsonSource("user-location-source")
    style.addSource(source)
    style.addLayer(
        CircleLayer("user-location-halo", "user-location-source").withProperties(
            PropertyFactory.circleColor("#1E88E5"),
            PropertyFactory.circleOpacity(0.18f),
            PropertyFactory.circleRadius(18f),
        ),
    )
    style.addLayer(
        CircleLayer("user-location-dot", "user-location-source").withProperties(
            PropertyFactory.circleColor("#1E88E5"),
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.5f),
        ),
    )
    return source
}

// ─── Network ─────────────────────────────────────────────────────────────────

internal suspend fun brouterRoute(from: LatLng, to: LatLng, profile: String): RouteResult? =
    withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://brouter.de/brouter?lonlats=" +
                    "${from.longitude},${from.latitude}|${to.longitude},${to.latitude}" +
                    "&profile=$profile&alternativeidx=0&format=geojson"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val feature = JSONObject(body).getJSONArray("features").getJSONObject(0)
            val props = feature.getJSONObject("properties")
            val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
            val polyline = (0 until coords.length()).map {
                val pt = coords.getJSONArray(it)
                LatLng(pt.getDouble(1), pt.getDouble(0))
            }
            RouteResult(
                polyline = polyline,
                distanceM = props.getString("track-length").toDouble(),
                durationS = props.getString("total-time").toDouble(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "BRouter failed: $profile", e)
            null
        }
    }

@SuppressLint("MissingPermission")
internal suspend fun getUserLocation(context: Context): LatLng? {
    val granted = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return null
    val client = LocationServices.getFusedLocationProviderClient(context)
    return suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }
}

// ─── PDOK basemap style ──────────────────────────────────────────────────────

internal const val PDOK_BRT_STYLE = """
{
  "version": 8,
  "sources": {
    "pdok-brt": {
      "type": "raster",
      "tiles": ["https://service.pdok.nl/brt/achtergrondkaart/wmts/v2_0/standaard/EPSG:3857/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© Kadaster"
    }
  },
  "layers": [
    {"id": "pdok-brt-layer", "type": "raster", "source": "pdok-brt"}
  ]
}"""
