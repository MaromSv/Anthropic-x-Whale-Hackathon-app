package com.example.emergency.agent.tools

import android.content.Context
import android.util.JsonReader
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader

data class PoiEntry(
    val name: String,
    val category: String,
    val lat: Double,
    val lon: Double,
)

object PoiRepository {
    private const val TAG = "PoiRepository"
    private const val ASSET_NAME = "pois-nl.geojson"

    private val mutex = Mutex()
    @Volatile private var byCategory: Map<String, List<PoiEntry>>? = null

    suspend fun load(context: Context): Map<String, List<PoiEntry>> {
        byCategory?.let { return it }
        return mutex.withLock {
            byCategory ?: parse(context).also { byCategory = it }
        }
    }

    suspend fun findNearest(
        context: Context,
        category: String,
        userLat: Double,
        userLon: Double,
    ): PoiEntry? {
        val entries = load(context)[category].orEmpty()
        return entries.minByOrNull { haversineMeters(userLat, userLon, it.lat, it.lon) }
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(a))
    }

    private suspend fun parse(context: Context): Map<String, List<PoiEntry>> = withContext(Dispatchers.IO) {
        val staged = File(context.filesDir, ASSET_NAME)
        val source: () -> java.io.InputStream = if (staged.exists()) {
            { staged.inputStream() }
        } else {
            { context.assets.open(ASSET_NAME) }
        }
        val grouped = HashMap<String, ArrayList<PoiEntry>>()
        source().use { input ->
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "features") {
                        reader.beginArray()
                        while (reader.hasNext()) readFeature(reader, grouped)
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        }
        Log.d(TAG, "Loaded ${grouped.values.sumOf { it.size }} POIs across ${grouped.size} categories")
        grouped
    }

    private fun readFeature(reader: JsonReader, into: HashMap<String, ArrayList<PoiEntry>>) {
        var name: String? = null
        var category: String? = null
        var lat: Double? = null
        var lon: Double? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "properties" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "name" -> name = reader.nextString()
                            "category" -> category = reader.nextString()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                "geometry" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "coordinates" -> {
                                reader.beginArray()
                                if (reader.hasNext()) lon = reader.nextDouble()
                                if (reader.hasNext()) lat = reader.nextDouble()
                                while (reader.hasNext()) reader.skipValue()
                                reader.endArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val cat = category ?: return
        val la = lat ?: return
        val lo = lon ?: return
        into.getOrPut(cat) { ArrayList() }.add(
            PoiEntry(name = name ?: cat.replaceFirstChar { it.uppercase() }, category = cat, lat = la, lon = lo)
        )
    }
}
