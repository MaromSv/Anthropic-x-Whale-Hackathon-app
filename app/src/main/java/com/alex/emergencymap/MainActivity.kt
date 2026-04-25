package com.alex.emergencymap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.alex.emergencymap.ui.theme.EmergencyMapTheme
import com.google.android.gms.location.LocationServices
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
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

private const val TAG = "EmergencyMap"
private val DAM_SQUARE = LatLng(52.3731, 4.8926)

class MainActivity : ComponentActivity() {
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> Log.d(TAG, "Location permission granted: $granted") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, null, WellKnownTileServer.MapLibre)
        permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        setContent {
            EmergencyMapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MapScreen()
                }
            }
        }
    }
}

// ─── Data ────────────────────────────────────────────────────────────────────

private data class Poi(val name: String, val category: String, val lat: Double, val lon: Double)

private data class RouteResult(
    val polyline: List<LatLng>,
    val distanceM: Double,
    val durationS: Double,
)

private enum class Mode(val brouterProfile: String, val label: String, val icon: ImageVector) {
    Walk("trekking", "Walk", Icons.Default.DirectionsWalk),
    Bike("fastbike", "Bike", Icons.Default.DirectionsBike),
    Drive("car-fast", "Drive", Icons.Default.DirectionsCar),
}

// Bbox covering all of NL (south, west, north, east). Used to frame the
// initial camera now that POIs are loaded from assets/pois-nl.geojson.
private val NL_BBOX_SW = LatLng(50.7, 3.3)
private val NL_BBOX_NE = LatLng(53.6, 7.3)

// All POI categories produced by data-pipeline/extract_pois.py. Listed once so
// icon registration, color stops and bottom-card mappings stay in sync.
private val POI_CATEGORIES = listOf(
    "hospital", "doctor", "first_aid", "aed", "pharmacy", "police", "fire",
    "shelter", "water", "toilet", "metro", "parking_underground", "bunker",
    "fuel", "supermarket", "atm", "phone", "school", "community", "worship",
)

// Maps a POI category to a Compose icon used in the bottom card preview.
// Categories without a perfect Material match fall back to Place — the OSM
// PNG on the map pin still represents them correctly.
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

// One color per category — kept identical to the map circle stops in
// addPoiLayer so the bottom-card chip and the map pin always agree.
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

// ─── Composable UI ───────────────────────────────────────────────────────────

@Composable
fun MapScreen() {
    val context = LocalContext.current

    var mode by remember { mutableStateOf(Mode.Walk) }
    var selectedPoi by remember { mutableStateOf<Poi?>(null) }
    var routeResult by remember { mutableStateOf<RouteResult?>(null) }
    var routeLoading by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf(DAM_SQUARE) }
    var routeSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    LaunchedEffect(Unit) {
        getUserLocation(context)?.let {
            userLocation = it
            Log.d(TAG, "User location: ${it.latitude}, ${it.longitude}")
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
        mapView.getMapAsync { map ->
            // Fit camera to the whole NL bbox so the user can immediately see
            // pins/clusters across the country before zooming in.
            val bounds = com.mapbox.mapboxsdk.geometry.LatLngBounds.Builder()
                .include(NL_BBOX_SW)
                .include(NL_BBOX_NE)
                .build()
            map.cameraPosition = map.getCameraForLatLngBounds(
                bounds,
                intArrayOf(60, 100, 60, 200),
            ) ?: CameraPosition.Builder()
                .target(LatLng(52.1326, 5.2913))
                .zoom(7.0)
                .build()
            map.setStyle(Style.Builder().fromJson(PDOK_BRT_STYLE)) { style ->
                Log.d(TAG, "Style loaded")
                addPoiLayer(context, style)
                routeSource = addRouteLayer(style)
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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Top: floating mode chips
        ModeSelector(
            current = mode,
            onSelect = { mode = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
        )

        // Bottom: route info card
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            Mode.entries.forEach { m ->
                val selected = m == current
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onSelect(m) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            m.icon,
                            contentDescription = m.label,
                            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            m.label,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(categoryColor(poi.category)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    categoryIcon(poi.category),
                    contentDescription = poi.category,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    poi.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111111),
                    maxLines = 2,
                )
                Spacer(Modifier.size(4.dp))
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Calculating route…", fontSize = 13.sp, color = Color.Gray)
                    }
                    route != null -> Text(
                        "${formatDistance(route.distanceM)}  ·  ${formatDuration(route.durationS)} ${mode.label.lowercase()}",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                    )
                    else -> Text("Routing failed", fontSize = 13.sp, color = Color(0xFFB71C1C))
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
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

    // Reading 30+ MB off disk on the UI thread would freeze the launch frame,
    // so we offload to a worker and push the data back onto the map thread.
    Thread {
        try {
            val raw = context.assets.open("pois-nl.geojson")
                .bufferedReader()
                .use { it.readText() }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                source.setGeoJson(raw)
            }
            Log.d(TAG, "Loaded pois-nl.geojson (${raw.length / 1024} KB)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pois-nl.geojson", e)
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

// ─── Network ─────────────────────────────────────────────────────────────────

private suspend fun brouterRoute(from: LatLng, to: LatLng, profile: String): RouteResult? =
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
private suspend fun getUserLocation(context: Context): LatLng? {
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

private const val PDOK_BRT_STYLE = """
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
