package com.alex.emergencymap

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.alex.emergencymap.ui.theme.EmergencyMapTheme
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.Style

private const val TAG = "EmergencyMap"

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
        Log.d(TAG, "LaunchedEffect: requesting map")
        mapView.getMapAsync { map ->
            Log.d(TAG, "getMapAsync callback: map ready, setting camera + style")
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(52.3676, 4.9041))
                .zoom(13.0)
                .build()
            map.setStyle(Style.Builder().fromJson(PDOK_BRT_STYLE)) { style ->
                Log.d(TAG, "Style loaded, layers=${style.layers.size}, sources=${style.sources.size}")
            }
        }
        mapView.addOnDidFailLoadingMapListener { msg ->
            Log.e(TAG, "Map failed to load: $msg")
        }
        mapView.addOnDidFinishLoadingMapListener {
            Log.d(TAG, "Map FULLY loaded (style + visible tiles all done)")
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
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
