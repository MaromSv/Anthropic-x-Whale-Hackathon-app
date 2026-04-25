package com.example.emergency.offline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Stages the large bundled blobs (BRouter segments + profiles + the NL
 * MBTiles tile pack) from APK assets into [Context.getFilesDir] so they
 * can be opened as regular files.
 *
 * Required because:
 *   * BRouter's RoutingEngine reads .rd5 segment files via random-access
 *     [java.io.RandomAccessFile] — which only works against real files,
 *     not [android.content.res.AssetManager] streams.
 *   * The MBTiles tile pack is a SQLite database and must be opened by
 *     path; AAPT-stored assets aren't directly addressable by SQLite.
 *
 * Idempotent — bumping [VERSION] forces a re-copy, otherwise the
 * existing staged files are reused. The version file lives in [filesDir]
 * so a fresh install always starts with VERSION=-1 and triggers a copy.
 */
object OfflineAssets {

    private const val TAG = "OfflineAssets"

    // Bump when the bundled assets change so devices already updated to
    // a newer APK re-copy on next launch.
    private const val VERSION = 1
    private const val VERSION_FILE = "offline-assets.version"

    private const val ASSET_ROOT_BROUTER = "brouter"
    private const val ASSET_ROOT_TILES = "tiles"

    // Files we expect to find under each asset root. Listed explicitly so we
    // don't depend on AssetManager.list() ordering and so missing files fail
    // loudly instead of silently leaving the stage half-populated.
    private val PROFILE_FILES = listOf(
        "trekking.brf",
        "fastbike.brf",
        "car-fast.brf",
        "lookups.dat",
    )
    private val SEGMENT_FILES = listOf(
        "E0_N50.rd5",
        "E5_N50.rd5",
    )
    private const val MBTILES_FILE = "nl.mbtiles"

    data class Paths(
        val profilesDir: File,
        val segmentsDir: File,
        val mbtilesFile: File,
    )

    /**
     * Returns where each asset *will* live once staged, regardless of whether
     * the copy has happened yet. Cheap; safe to call from the main thread.
     */
    fun pathsFor(context: Context): Paths {
        val base = context.filesDir
        return Paths(
            profilesDir = File(base, "brouter/profiles"),
            segmentsDir = File(base, "brouter/segments"),
            mbtilesFile = File(base, "tiles/$MBTILES_FILE"),
        )
    }

    /**
     * True when every expected file exists at its staged location *and* the
     * version stamp matches the current build.
     */
    fun isStaged(context: Context): Boolean {
        if (readVersion(context) != VERSION) return false
        val paths = pathsFor(context)
        if (!paths.mbtilesFile.exists()) return false
        if (PROFILE_FILES.any { !File(paths.profilesDir, it).exists() }) return false
        if (SEGMENT_FILES.any { !File(paths.segmentsDir, it).exists() }) return false
        return true
    }

    /**
     * Copies the bundled assets to [filesDir] if they aren't already present
     * for the current [VERSION]. Safe to call repeatedly — short-circuits on
     * subsequent launches.
     *
     * Runs on [Dispatchers.IO]; total payload is ~500 MB so callers should
     * gate UI on the returned paths instead of blocking the main thread.
     */
    suspend fun ensureStaged(
        context: Context,
        onProgress: (stagedFiles: Int, totalFiles: Int) -> Unit = { _, _ -> },
    ): Paths = withContext(Dispatchers.IO) {
        val paths = pathsFor(context)

        if (isStaged(context)) {
            Log.d(TAG, "Already staged at v$VERSION — skipping copy")
            onProgress(totalFiles(), totalFiles())
            return@withContext paths
        }

        Log.d(TAG, "Staging offline assets to ${context.filesDir}")
        paths.profilesDir.mkdirs()
        paths.segmentsDir.mkdirs()
        paths.mbtilesFile.parentFile?.mkdirs()

        val total = totalFiles()
        var done = 0
        onProgress(done, total)

        // Profiles: tiny text/binary files, copy in one shot.
        for (name in PROFILE_FILES) {
            copyAsset(
                context,
                "$ASSET_ROOT_BROUTER/profiles/$name",
                File(paths.profilesDir, name),
            )
            done++; onProgress(done, total)
        }

        // Segments: 70-180 MB each. The asset stream is uncompressed thanks
        // to noCompress("rd5"), so this is a straight byte copy.
        for (name in SEGMENT_FILES) {
            copyAsset(
                context,
                "$ASSET_ROOT_BROUTER/segments/$name",
                File(paths.segmentsDir, name),
            )
            done++; onProgress(done, total)
        }

        // MBTiles SQLite — single large file (~300 MB).
        copyAsset(
            context,
            "$ASSET_ROOT_TILES/$MBTILES_FILE",
            paths.mbtilesFile,
        )
        done++; onProgress(done, total)

        writeVersion(context, VERSION)
        Log.d(TAG, "Stage complete: v$VERSION")
        paths
    }

    private fun totalFiles(): Int =
        PROFILE_FILES.size + SEGMENT_FILES.size + /* mbtiles */ 1

    private fun copyAsset(context: Context, assetPath: String, dest: File) {
        val tmp = File(dest.parentFile, "${dest.name}.partial")
        // Fast path: openFd() works because every large asset is in the
        // noCompress list (.rd5/.mbtiles/.brf/.dat). The returned descriptor
        // points at a byte range *inside* the APK file itself, which lets us
        // ask the kernel to do the copy via FileChannel.transferTo() →
        // sendfile(2) on Linux. Skips the JVM-side 1 MB bounce buffer and
        // halves staging time on flash storage.
        val afd = context.assets.openFd(assetPath)
        afd.use {
            FileInputStream(it.fileDescriptor).channel.use { src ->
                FileOutputStream(tmp).channel.use { dst ->
                    val length = it.length
                    val baseOffset = it.startOffset
                    var copied = 0L
                    while (copied < length) {
                        val n = src.transferTo(baseOffset + copied, length - copied, dst)
                        if (n <= 0) break
                        copied += n
                    }
                    if (copied != length) {
                        error("Short read on $assetPath: $copied/$length bytes")
                    }
                }
            }
        }
        // Atomic rename so a half-written file can never be mistaken for a
        // staged one if the process is killed mid-copy.
        if (dest.exists()) dest.delete()
        check(tmp.renameTo(dest)) { "Failed to rename ${tmp.name} → ${dest.name}" }
        Log.d(TAG, "Staged ${dest.name} (${dest.length() / 1024} KB)")
    }

    private fun readVersion(context: Context): Int =
        runCatching {
            File(context.filesDir, VERSION_FILE).readText().trim().toInt()
        }.getOrDefault(-1)

    private fun writeVersion(context: Context, v: Int) {
        File(context.filesDir, VERSION_FILE).writeText(v.toString())
    }
}
