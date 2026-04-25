package com.example.emergency.offline

import android.util.Log
import btools.router.OsmNodeNamed
import btools.router.RoutingContext
import btools.router.RoutingEngine
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thin Kotlin wrapper around [btools.router.RoutingEngine] that produces a
 * route entirely from on-device data — no network involved. Replaces the
 * previous online call to brouter.de.
 *
 * Each call instantiates a fresh [RoutingEngine] (BRouter is not designed to
 * be reused across queries — it owns mutable caches keyed off the active
 * request). The engine reads the routing graph from `.rd5` segment files
 * already staged into [segmentsDir] by [OfflineAssets].
 *
 * Profile names map 1:1 to the Mode.brouterProfile values in InteractiveMap
 * (`trekking`, `fastbike`, `car-fast`).
 */
object OfflineRouter {

    private const val TAG = "OfflineRouter"

    data class Result(
        val polyline: List<LatLng>,
        val distanceM: Double,
        val durationS: Double,
    )

    suspend fun route(
        from: LatLng,
        to: LatLng,
        profileName: String,
        segmentsDir: File,
        profilesDir: File,
    ): Result? = withContext(Dispatchers.IO) {
        val profileFile = File(profilesDir, "$profileName.brf")
        if (!profileFile.exists()) {
            Log.e(TAG, "Profile missing: ${profileFile.absolutePath}")
            return@withContext null
        }
        if (!segmentsDir.exists()) {
            Log.e(TAG, "Segments dir missing: ${segmentsDir.absolutePath}")
            return@withContext null
        }

        try {
            // RoutingContext.localFunction is the absolute path to the .brf
            // profile. readGlobalConfig() also expects a global config file
            // alongside it; we don't ship one, so we can't call it. The
            // 5-arg engine constructor invokes profile parsing internally
            // when first needed.
            val rc = RoutingContext().apply {
                localFunction = profileFile.absolutePath
            }

            val waypoints = listOf(
                makeNode("from", from.latitude, from.longitude),
                makeNode("to", to.latitude, to.longitude),
            )

            val engine = RoutingEngine(
                /* outfileBase  = */ null,
                /* logfileBase  = */ null,
                /* segmentDir   = */ segmentsDir,
                /* waypoints    = */ waypoints,
                /* routingCtx   = */ rc,
            ).apply {
                // BRouter's typo, not ours — this silences the (very chatty)
                // System.out logging during routing.
                quite = true
            }

            // 0 = no wall-clock timeout. Routing within NL completes in
            // sub-second for walking distances; long-haul car routes can
            // take a few seconds on cold caches.
            engine.doRun(0L)

            val track = engine.foundTrack
            if (track == null) {
                Log.e(TAG, "No track: ${engine.errorMessage}")
                return@withContext null
            }

            // BRouter stores coordinates as int microdegrees offset by
            // (180, 90) so they fit unsigned ranges. Using the Java
            // accessors (getILat/getILon) explicitly because Kotlin's
            // bean-property heuristic for all-caps prefixes is fragile.
            val polyline = track.nodes.map { el ->
                LatLng(
                    el.getILat() / 1_000_000.0 - 90.0,
                    el.getILon() / 1_000_000.0 - 180.0,
                )
            }

            Result(
                polyline = polyline,
                distanceM = track.distance.toDouble(),
                durationS = track.totalSeconds.toDouble(),
            )
        } catch (e: Throwable) {
            Log.e(TAG, "BRouter failed for profile=$profileName", e)
            null
        }
    }

    private fun makeNode(name: String, lat: Double, lon: Double): OsmNodeNamed =
        OsmNodeNamed().apply {
            this.name = name
            ilon = ((lon + 180.0) * 1_000_000 + 0.5).toInt()
            ilat = ((lat + 90.0) * 1_000_000 + 0.5).toInt()
        }
}
