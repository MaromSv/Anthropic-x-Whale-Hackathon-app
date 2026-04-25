package com.alex.emergencymap

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.alex.emergencymap.ui.theme.EmergencyMapTheme
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
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "EmergencyMap"
private val DAM_SQUARE = LatLng(52.3731, 4.8926)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, null, WellKnownTileServer.MapLibre)
        setContent {
            EmergencyMapTheme {
                MapScreen()
            }
        }
    }
}

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

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

    var routeSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(52.3676, 4.9041))
                .zoom(13.0)
                .build()
            map.setStyle(Style.Builder().fromJson(PDOK_BRT_STYLE)) { style ->
                Log.d(TAG, "Style loaded")
                addPoiLayer(style)
                routeSource = addRouteLayer(style)
            }

            map.addOnMapClickListener { latLng ->
                val pt = map.projection.toScreenLocation(latLng)
                val touchRect = RectF(pt.x - 30, pt.y - 30, pt.x + 30, pt.y + 30)
                val hits = map.queryRenderedFeatures(touchRect, "pois-layer")
                if (hits.isNotEmpty()) {
                    val f = hits[0]
                    val name = f.getStringProperty("name") ?: "POI"
                    val coord = f.geometry() as Point
                    Log.d(TAG, "Tapped POI: $name")
                    scope.launch {
                        snackbar.showSnackbar("Routing to: $name")
                        val route = osrmWalkRoute(
                            DAM_SQUARE,
                            LatLng(coord.latitude(), coord.longitude())
                        )
                        if (route != null && route.size > 1) {
                            val pts = route.map { Point.fromLngLat(it.longitude, it.latitude) }
                            routeSource?.setGeoJson(LineString.fromLngLats(pts))
                        } else {
                            snackbar.showSnackbar("Routing failed (check internet)")
                        }
                    }
                    true
                } else false
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        }
    }
}

private data class Poi(val name: String, val category: String, val lat: Double, val lon: Double)

private val POIS = listOf(
    Poi("OLVG Oost",       "hospital", 52.3559, 4.9264),
    Poi("AMC Amsterdam",   "hospital", 52.2944, 4.9583),
    Poi("VUmc",            "hospital", 52.3344, 4.8525),
    Poi("AED Centraal",    "aed",      52.3791, 4.9003),
    Poi("AED Dam Square",  "aed",      52.3731, 4.8926),
    Poi("AED Leidseplein", "aed",      52.3641, 4.8830),
)

private fun addPoiLayer(style: Style) {
    val features = POIS.map { p ->
        Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).apply {
            addStringProperty("name", p.name)
            addStringProperty("category", p.category)
        }
    }
    style.addSource(GeoJsonSource("pois-source", FeatureCollection.fromFeatures(features)))
    style.addLayer(
        CircleLayer("pois-layer", "pois-source").withProperties(
            PropertyFactory.circleRadius(10f),
            PropertyFactory.circleStrokeWidth(3f),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleColor(
                Expression.match(
                    Expression.get("category"),
                    Expression.literal("#9E9E9E"),
                    Expression.stop("hospital", "#E53935"),
                    Expression.stop("aed", "#FB8C00"),
                )
            )
        )
    )
}

private fun addRouteLayer(style: Style): GeoJsonSource {
    val source = GeoJsonSource("route-source")
    style.addSource(source)
    // Insert the route line BELOW the POI dots so the dots stay on top.
    style.addLayerBelow(
        LineLayer("route-layer", "route-source").withProperties(
            PropertyFactory.lineColor("#1E88E5"),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineOpacity(0.85f),
        ),
        "pois-layer"
    )
    return source
}

private suspend fun osrmWalkRoute(from: LatLng, to: LatLng): List<LatLng>? =
    withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://router.project-osrm.org/route/v1/foot/" +
                "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
                "?overview=full&geometries=geojson"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val coords = JSONObject(body)
                .getJSONArray("routes").getJSONObject(0)
                .getJSONObject("geometry").getJSONArray("coordinates")
            (0 until coords.length()).map {
                val pt = coords.getJSONArray(it)
                LatLng(pt.getDouble(1), pt.getDouble(0))
            }
        } catch (e: Exception) {
            Log.e(TAG, "OSRM failed", e)
            null
        }
    }

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
