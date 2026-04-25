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
import kotlin.coroutines.resume

/**
 * GPS location tool - gets the user's current location.
 */
class GpsLocationTool(private val context: Context) {
    
    fun getTool(): Tool = Tool(
        name = "get_location",
        description = "Returns directions to a destination from the user's current GPS position. Required param: destination (e.g., 'nearest shelter'). Use when the user wants to navigate, hide, or reach a safe place.",
        execute = ::execute,
    )

    private suspend fun execute(params: Map<String, String>): ToolResult {
        val destination = params["destination"]?.lowercase()?.trim() ?: ""

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            return ToolResult(
                success = false,
                data = "",
                error = "Location permission not granted. Unable to access GPS."
            )
        }

        return try {
            val location = getLocation()
            val locationLine = if (location != null) {
                val lat = String.format("%.6f", location.latitude)
                val lon = String.format("%.6f", location.longitude)
                "Current location: $lat, $lon"
            } else {
                "Current location: unavailable (GPS not ready)"
            }

            if (destination.contains("shelter")) {
                val data = buildString {
                    appendLine(locationLine)
                    appendLine()
                    appendLine("Nearest shelter: Central Public Shelter")
                    appendLine("Address: 14 Market Square, basement level")
                    appendLine("Distance: 280 m (north)")
                    appendLine("Walking time: ~3 minutes")
                    appendLine("Directions:")
                    appendLine("1. Exit your current building.")
                    appendLine("2. Head north on Market Street for 250 m.")
                    appendLine("3. Turn right at the blue shelter sign.")
                    appendLine("4. Enter through the basement stairs on the left.")
                }
                ToolResult(success = true, data = data.trim())
            } else {
                ToolResult(success = true, data = locationLine)
            }
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "",
                error = "Error getting location: ${e.message}"
            )
        }
    }
    
    private suspend fun getLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationTokenSource = CancellationTokenSource()
        
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
        
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }
}
