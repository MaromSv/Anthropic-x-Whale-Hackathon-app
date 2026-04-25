package com.example.emergency.agent.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.emergency.agent.Tool
import com.example.emergency.agent.ToolResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

class FindNearestTool(private val context: Context) {

    private val supportedCategories = listOf(
        "hospital", "doctor", "first_aid", "aed", "pharmacy", "police", "fire",
        "shelter", "water", "toilet", "metro", "parking_underground", "bunker",
        "fuel", "supermarket", "atm", "phone", "school", "community", "worship",
    )

    fun getTool(): Tool = Tool(
        name = "find_nearest",
        description = "Finds the nearest place to the user's current location for a given category. " +
            "Use when the user asks where the nearest hospital, pharmacy, AED, police station, shelter, " +
            "water point, toilet, fuel station, supermarket, ATM, etc. is. " +
            "Required param: category (one of: ${supportedCategories.joinToString()}). " +
            "Returns JSON with name, category, lat, lon.",
        execute = ::execute,
    )

    private suspend fun execute(params: Map<String, String>): ToolResult {
        val rawCategory = params["category"] ?: params["query"]
            ?: return ToolResult(false, "", "Missing required param 'category'.")
        val category = normalizeCategory(rawCategory)
            ?: return ToolResult(
                false, "",
                "Unknown category '$rawCategory'. Supported: ${supportedCategories.joinToString()}",
            )

        val location = getLocation()
            ?: return ToolResult(false, "", "Could not determine current GPS location.")

        val poi = PoiRepository.findNearest(context, category, location.latitude, location.longitude)
            ?: return ToolResult(false, "", "No '$category' found in the dataset.")

        val distanceM = PoiRepository.haversineMeters(
            location.latitude, location.longitude, poi.lat, poi.lon,
        ).toInt()

        val json = JSONObject().apply {
            put("name", poi.name)
            put("category", poi.category)
            put("lat", poi.lat)
            put("lon", poi.lon)
            put("distance_m", distanceM)
        }
        return ToolResult(success = true, data = json.toString())
    }

    private fun normalizeCategory(input: String): String? {
        val cleaned = input.trim().lowercase().replace(' ', '_')
        if (cleaned in supportedCategories) return cleaned
        return aliases[cleaned]
    }

    private val aliases: Map<String, String> = mapOf(
        "defibrillator" to "aed",
        "defibrillators" to "aed",
        "automatic_external_defibrillator" to "aed",
        "hospitals" to "hospital",
        "emergency_room" to "hospital",
        "er" to "hospital",
        "drugstore" to "pharmacy",
        "chemist" to "pharmacy",
        "pharmacies" to "pharmacy",
        "doctors" to "doctor",
        "gp" to "doctor",
        "clinic" to "doctor",
        "medical_post" to "first_aid",
        "first_aid_post" to "first_aid",
        "police_station" to "police",
        "fire_station" to "fire",
        "fire_brigade" to "fire",
        "gas_station" to "fuel",
        "petrol_station" to "fuel",
        "grocery" to "supermarket",
        "groceries" to "supermarket",
        "store" to "supermarket",
        "subway" to "metro",
        "tube" to "metro",
        "train" to "metro",
        "wc" to "toilet",
        "restroom" to "toilet",
        "bathroom" to "toilet",
        "drinking_water" to "water",
        "fountain" to "water",
        "shelters" to "shelter",
        "church" to "worship",
        "mosque" to "worship",
        "synagogue" to "worship",
        "temple" to "worship",
    )

    private suspend fun getLocation(): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null
        return suspendCancellableCoroutine { continuation ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resume(null) }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
            continuation.invokeOnCancellation { cts.cancel() }
        }
    }
}
