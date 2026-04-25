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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Place
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
import com.mapbox.geojson.Feature
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

private val POIS = listOf(
    // Hospitals (6)
    Poi("Antoni van Leeuwenhoek", "hospital", 52.35029, 4.82576),
    Poi("Amsterdam UMC - VUmc", "hospital", 52.33454, 4.85993),
    Poi("OLVG Oost", "hospital", 52.35815, 4.91532),
    Poi("BovenIJ Ziekenhuis", "hospital", 52.40465, 4.92192),
    Poi("Reade (JBI)", "hospital", 52.37063, 4.84449),
    Poi("OLVG West", "hospital", 52.37101, 4.83973),
    // Fire stations (16)
    Poi("Kazerne Victor", "fire", 52.36046, 4.92917),
    Poi("Kazerne Nico", "fire", 52.37028, 4.90959),
    Poi("Kazerne Hendrik", "fire", 52.37260, 4.87566),
    Poi("Kazerne Osdorp", "fire", 52.36653, 4.80248),
    Poi("Kazerne Amstelveen", "fire", 52.30165, 4.88184),
    Poi("Kazerne Dirk", "fire", 52.35795, 4.88594),
    Poi("Kazerne Diemen", "fire", 52.34123, 4.96466),
    Poi("Kazerne Willem", "fire", 52.34879, 4.91487),
    Poi("Kazerne IJsbrand", "fire", 52.40633, 4.88952),
    Poi("Kazerne Zebra", "fire", 52.39621, 4.95491),
    Poi("Kazerne Teunis", "fire", 52.38463, 4.86343),
    Poi("Veiligheidscentrum Brandweer", "fire", 52.39603, 4.79901),
    Poi("Kazerne Pieter", "fire", 52.34893, 4.84358),
    Poi("Kazerne Duivendrecht", "fire", 52.33231, 4.94038),
    Poi("Kazerne Noord", "fire", 52.40435, 4.86179),
    Poi("Kazerne Anton", "fire", 52.30736, 4.97325),
    // Police (22)
    Poi("Politie Lijnbaansgracht", "police", 52.36517, 4.88140),
    Poi("Politie IJ-tunnel", "police", 52.37000, 4.90916),
    Poi("Politie Amstelveen", "police", 52.30250, 4.86361),
    Poi("Politie Meer en Vaart", "police", 52.36260, 4.80510),
    Poi("Politie Van Leijenberghlaan", "police", 52.33097, 4.87980),
    Poi("Politie Amsterdam", "police", 52.34954, 4.92015),
    Poi("Luchtvaartpolitie", "police", 52.31398, 4.80771),
    Poi("Politie Watergraafsmeer", "police", 52.35747, 4.92729),
    Poi("Politie Overtoomsesluis", "police", 52.37674, 4.84636),
    Poi("Politie Flierbosdreef", "police", 52.31470, 4.95765),
    Poi("Politie Houtmankade", "police", 52.38909, 4.88419),
    Poi("Politie Zuid De Pijp", "police", 52.33998, 4.90222),
    Poi("Politie Boven-IJ", "police", 52.39503, 4.92783),
    Poi("Politie Nieuwezijds Voorburg", "police", 52.37645, 4.89437),
    Poi("Politie Postjesweg", "police", 52.36375, 4.83869),
    Poi("Politie Sloter", "police", 52.34635, 4.82814),
    Poi("Politie Amstelland-Oost", "police", 52.34124, 4.96559),
    Poi("Politie Bijlmer", "police", 52.31336, 4.95817),
    Poi("Politie Zeeburg", "police", 52.36898, 4.97141),
    Poi("Hoofdbureau van Politie", "police", 52.36839, 4.87801),
    Poi("Marechaussee Schiphol", "police", 52.33166, 4.78013),
    Poi("Marechaussee Noord", "police", 52.39820, 4.83527),
    // Pharmacies (18)
    Poi("Transvaal Apotheek", "pharmacy", 52.35356, 4.91938),
    Poi("Sumatra Apotheek", "pharmacy", 52.36170, 4.93606),
    Poi("Apotheek Dr de Haan", "pharmacy", 52.35188, 4.85739),
    Poi("Ferdinand Bol Apotheek", "pharmacy", 52.35321, 4.89118),
    Poi("Linnaeus Apotheek", "pharmacy", 52.35982, 4.92584),
    Poi("Mediq Apotheek Badhoevedorp", "pharmacy", 52.33701, 4.78138),
    Poi("Park Apotheek", "pharmacy", 52.35765, 4.91645),
    Poi("Benu Apotheek", "pharmacy", 52.37895, 4.80070),
    Poi("Zuider Apotheek", "pharmacy", 52.34792, 4.91025),
    Poi("Kamperfoelie Apotheek", "pharmacy", 52.39002, 4.91239),
    Poi("Lloyd Apotheek", "pharmacy", 52.35944, 4.80372),
    Poi("De Vogel Apotheek", "pharmacy", 52.37084, 4.91645),
    Poi("Slotervaart Apotheek", "pharmacy", 52.35706, 4.82687),
    Poi("Plesman Apotheek", "pharmacy", 52.35588, 4.82183),
    Poi("Stadion Apotheek", "pharmacy", 52.34604, 4.86539),
    Poi("Apotheek Koning", "pharmacy", 52.37984, 4.82035),
    Poi("Apotheek Nieuw Sloten", "pharmacy", 52.34583, 4.80999),
    Poi("BENU Apotheek Zuidoost", "pharmacy", 52.32575, 4.87533),
    // Shelters (15)
    Poi("Schuilplaats Noord", "shelter", 52.38534, 4.85157),
    Poi("Schuilplaats Oranje Loper", "shelter", 52.42212, 4.94770),
    Poi("Schuilplaats Centraal A", "shelter", 52.38029, 4.89714),
    Poi("Schuilplaats Centraal B", "shelter", 52.38044, 4.89723),
    Poi("Schuilplaats Centraal C", "shelter", 52.38047, 4.89772),
    Poi("Schuilplaats Centraal D", "shelter", 52.38061, 4.89721),
    Poi("Schuilplaats Centraal E", "shelter", 52.38020, 4.89749),
    Poi("Schuilplaats Centraal F", "shelter", 52.38017, 4.89598),
    Poi("Schuilplaats Centraal G", "shelter", 52.38025, 4.89576),
    Poi("Schuilplaats Centraal H", "shelter", 52.37995, 4.89654),
    Poi("Schuilplaats Centraal I", "shelter", 52.37950, 4.89777),
    Poi("Schuilplaats Watergraafsmeer", "shelter", 52.33055, 4.95640),
    Poi("Schuilplaats Pijp A", "shelter", 52.33908, 4.87403),
    Poi("Schuilplaats Pijp B", "shelter", 52.33911, 4.87446),
    Poi("Schuilplaats Pijp C", "shelter", 52.33913, 4.87495),
    // AEDs (25)
    Poi("AED ProRail/NS", "aed", 52.38259, 4.89357),
    Poi("AED Noord", "aed", 52.38956, 4.83942),
    Poi("AED Tuindorp", "aed", 52.39475, 4.89936),
    Poi("AED Centraal", "aed", 52.37848, 4.90016),
    Poi("AED Oosterdokseiland", "aed", 52.37994, 4.90024),
    Poi("AED Watergraafsmeer", "aed", 52.33850, 4.97613),
    Poi("AED De Pijp", "aed", 52.33767, 4.89617),
    Poi("AED Vondelpark", "aed", 52.36386, 4.85129),
    Poi("AED Sloterdijk", "aed", 52.35663, 4.79346),
    Poi("AED Indische Buurt", "aed", 52.36980, 4.92482),
    Poi("AED Oost", "aed", 52.37139, 4.92167),
    Poi("AED Centrum", "aed", 52.35181, 4.87824),
    Poi("AED Westerpark", "aed", 52.38014, 4.85427),
    Poi("AED Singel", "aed", 52.36847, 4.87794),
    Poi("AED Plantage", "aed", 52.36868, 4.90382),
    Poi("AED Centraal-Plein", "aed", 52.37026, 4.90797),
    Poi("AED Westerdok", "aed", 52.37963, 4.87201),
    Poi("AED Leidsegracht A", "aed", 52.36690, 4.86888),
    Poi("AED Leidsegracht B", "aed", 52.36682, 4.86859),
    Poi("AED Leidsegracht C", "aed", 52.36731, 4.86892),
    Poi("AED Plantage Oost", "aed", 52.36673, 4.90427),
    Poi("AED Leidseplein", "aed", 52.36705, 4.86824),
    Poi("AED Spiegelgracht", "aed", 52.36248, 4.87014),
    Poi("AED Vondelpark Oost", "aed", 52.35929, 4.86346),
    Poi("AED Stationsplein", "aed", 52.37809, 4.90107),
)

private fun categoryIcon(category: String): ImageVector = when (category) {
    "hospital" -> Icons.Default.LocalHospital
    "aed"      -> Icons.Default.Favorite
    "pharmacy" -> Icons.Default.LocalPharmacy
    "police"   -> Icons.Default.LocalPolice
    "fire"     -> Icons.Default.LocalFireDepartment
    "shelter"  -> Icons.Default.Home
    else       -> Icons.Default.Place
}

private fun categoryColor(category: String): Color = when (category) {
    "hospital" -> Color(0xFFE53935)
    "aed"      -> Color(0xFFFB8C00)
    "pharmacy" -> Color(0xFF43A047)
    "police"   -> Color(0xFF1E40AF)
    "fire"     -> Color(0xFFB71C1C)
    "shelter"  -> Color(0xFF00897B)
    else       -> Color(0xFF757575)
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
            // Fit camera to all POIs at startup.
            val bounds = com.mapbox.mapboxsdk.geometry.LatLngBounds.Builder().apply {
                POIS.forEach { include(LatLng(it.lat, it.lon)) }
            }.build()
            map.cameraPosition = map.getCameraForLatLngBounds(
                bounds,
                intArrayOf(120, 120, 280, 120),  // L, T, R, B padding for top chips & bottom card
            ) ?: CameraPosition.Builder()
                .target(LatLng(52.3676, 4.9041))
                .zoom(11.0)
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

private fun addPoiLayer(context: Context, style: Style) {
    // Register category icons (white glyphs on transparent, sit on top of colored circles).
    listOf("hospital", "aed", "pharmacy", "police", "fire", "shelter").forEach { name ->
        val resId = context.resources.getIdentifier("ic_poi_$name", "drawable", context.packageName)
        if (resId != 0) {
            BitmapFactory.decodeResource(context.resources, resId)?.let { bmp ->
                style.addImage("$name-icon", bmp)
            }
        }
    }

    val features = POIS.map { p ->
        Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).apply {
            addStringProperty("name", p.name)
            addStringProperty("category", p.category)
        }
    }
    style.addSource(GeoJsonSource("pois-source", FeatureCollection.fromFeatures(features)))

    // Colored circle background.
    style.addLayer(
        CircleLayer("pois-layer", "pois-source").withProperties(
            PropertyFactory.circleRadius(13f),
            PropertyFactory.circleStrokeWidth(2.5f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleColor(
                Expression.match(
                    Expression.get("category"),
                    Expression.literal("#9E9E9E"),
                    Expression.stop("hospital", "#E53935"),
                    Expression.stop("aed",      "#FB8C00"),
                    Expression.stop("pharmacy", "#43A047"),
                    Expression.stop("police",   "#1E40AF"),
                    Expression.stop("fire",     "#B71C1C"),
                    Expression.stop("shelter",  "#00897B"),
                )
            )
        )
    )

    // White category icon on top of the circle.
    style.addLayer(
        SymbolLayer("pois-icons-layer", "pois-source").withProperties(
            PropertyFactory.iconImage(
                Expression.concat(Expression.get("category"), Expression.literal("-icon"))
            ),
            PropertyFactory.iconSize(0.22f),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
        )
    )
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
