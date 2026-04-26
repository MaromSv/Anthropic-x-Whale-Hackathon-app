package com.example.emergency.offline

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Tiny localhost HTTP server that streams raster tiles out of a single
 * MBTiles SQLite file. Bridges the gap between MapLibre Android 10.x
 * (which only speaks `http(s)://` for raster sources) and our bundled
 * offline tile pack.
 *
 * Bound to 127.0.0.1 with an OS-assigned port. The active port is exposed
 * via [tileUrlTemplate] for the style JSON.
 *
 * Lifecycle: callers should [start] before the map style is loaded and
 * [stop] when the map view is destroyed. The underlying SQLite handle is
 * shared across handler threads; reads are safe under SQLite's default
 * isolation, but the database is opened read-only just to be sure no
 * accidental write ever fires.
 */
class MbtilesServer(
    private val mbtilesFile: File,
) : NanoHTTPD("127.0.0.1", /* port = */ 0) {

    private var db: SQLiteDatabase? = null

    override fun start() {
        if (!mbtilesFile.exists()) {
            error("MBTiles file missing: ${mbtilesFile.absolutePath}")
        }
        // OPEN_READONLY also prevents SQLite from creating a -journal file
        // next to the asset, which is important when the file lives in a
        // shared filesDir we expect to be append-only between version bumps.
        db = SQLiteDatabase.openDatabase(
            mbtilesFile.absolutePath,
            /* factory = */ null,
            SQLiteDatabase.OPEN_READONLY,
        )
        // NanoHTTPD's two-arg start(timeout, daemon). The constant is declared
        // on the superclass; qualify it explicitly because Kotlin doesn't
        // hoist Java statics into subclass scope automatically.
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, /* daemon = */ true)
        Log.d(TAG, "MBTiles server listening on $tileUrlTemplate")
    }

    override fun stop() {
        super.stop()
        db?.close()
        db = null
    }

    val tileUrlTemplate: String
        get() = "http://127.0.0.1:$listeningPort/{z}/{x}/{y}.png"

    override fun serve(session: IHTTPSession): Response {
        Log.d(TAG, "Request: ${session.uri}")
        val match = TILE_PATH.matchEntire(session.uri)
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "bad path",
            )
        val (zStr, xStr, yStr) = match.destructured
        val z = zStr.toInt()
        val x = xStr.toInt()
        val xyzY = yStr.toInt()
        // MBTiles stores tiles with a flipped y axis (TMS scheme). MapLibre
        // requests in slippy/XYZ; convert.
        val tmsY = (1 shl z) - 1 - xyzY

        val data = readTile(z, x, tmsY)
        if (data == null) {
            Log.w(TAG, "Tile MISS z=$z x=$x tmsY=$tmsY (xyzY=$xyzY)")
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "image/png", "",
            )
        }
        Log.d(TAG, "Tile HIT z=$z x=$x tmsY=$tmsY size=${data.size}")

        return newFixedLengthResponse(
            Response.Status.OK,
            "image/png",
            ByteArrayInputStream(data),
            data.size.toLong(),
        ).apply {
            // Map tiles never change at a given (z,x,y); let MapLibre cache
            // aggressively for the lifetime of the process.
            addHeader("Cache-Control", "public, max-age=31536000, immutable")
        }
    }

    private fun readTile(z: Int, x: Int, tmsY: Int): ByteArray? {
        val database = db ?: return null
        return database.rawQuery(
            "SELECT tile_data FROM tiles " +
                "WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1",
            arrayOf(z.toString(), x.toString(), tmsY.toString()),
        ).use { c ->
            if (c.moveToFirst()) c.getBlob(0) else null
        }
    }

    companion object {
        private const val TAG = "MbtilesServer"
        private val TILE_PATH = Regex("""^/(\d+)/(\d+)/(\d+)\.png$""")
    }
}
