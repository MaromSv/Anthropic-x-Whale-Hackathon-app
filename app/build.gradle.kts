import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ─── Offline data plane (auto-fetched before every build) ────────────────────
//
// The map ships with three big binary blobs that are too large for git:
//   * app/libs/brouter.jar
//   * app/src/main/assets/brouter/segments/*.rd5
//   * app/src/main/assets/tiles/nl.mbtiles
//
// On a fresh checkout these don't exist, and `mergeDebugNativeLibs` fails
// with "File/directory does not exist: app/libs/brouter.jar". The tasks below
// fetch / build them automatically the first time anyone runs ./gradlew, so
// new contributors don't have to remember to run scripts/setup_offline_data.sh
// before their first build. Already-present files are skipped — re-running is
// instant.

val brouterVersion = "1.7.9"
val brouterZipUrl =
    "https://github.com/abrensch/brouter/releases/download/v$brouterVersion/" +
        "brouter-$brouterVersion.zip"

// Each segment is 5°×5°; together these cover the Netherlands (with some
// spill into BE/DE/UK that's harmless).
val brouterSegments = listOf("E0_N50.rd5", "E5_N50.rd5")
val brouterSegmentBaseUrl = "https://brouter.de/brouter/segments4"

val brouterJar = file("libs/brouter.jar")
val brouterProfilesDir = file("src/main/assets/brouter/profiles")
val brouterSegmentsDir = file("src/main/assets/brouter/segments")
val mbtilesFile = file("src/main/assets/tiles/nl.mbtiles")
val brouterProfileFiles = listOf("trekking.brf", "fastbike.brf", "car-fast.brf", "lookups.dat")

fun download(url: String, dest: File) {
    dest.parentFile.mkdirs()
    val tmp = File(dest.parentFile, "${dest.name}.partial")
    logger.lifecycle("    downloading ${dest.name} ← $url")
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 30_000
    conn.readTimeout = 30 * 60 * 1000
    conn.instanceFollowRedirects = true
    if (conn.responseCode !in 200..299) {
        error("Download failed (${conn.responseCode} ${conn.responseMessage}): $url")
    }
    conn.inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
    if (dest.exists()) dest.delete()
    check(tmp.renameTo(dest)) { "Failed to rename ${tmp.name} → ${dest.name}" }
}

val fetchBrouterDist by tasks.registering {
    description = "Downloads the BRouter all-jar + routing profiles."
    group = "offline-data"
    val needs = !brouterJar.exists() ||
        brouterProfileFiles.any { !File(brouterProfilesDir, it).exists() }
    onlyIf { needs }
    doLast {
        val tmpDir = layout.buildDirectory.dir("brouter-dist").get().asFile.also {
            it.deleteRecursively(); it.mkdirs()
        }
        val zipFile = File(tmpDir, "brouter.zip")
        download(brouterZipUrl, zipFile)
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { e ->
                if (e.isDirectory) return@forEach
                val out = File(tmpDir, e.name)
                out.parentFile.mkdirs()
                zip.getInputStream(e).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
        }
        val unpacked = File(tmpDir, "brouter-$brouterVersion")
        brouterJar.parentFile.mkdirs()
        File(unpacked, "brouter-$brouterVersion-all.jar").copyTo(brouterJar, overwrite = true)
        brouterProfilesDir.mkdirs()
        for (name in brouterProfileFiles) {
            File(unpacked, "profiles2/$name").copyTo(File(brouterProfilesDir, name), overwrite = true)
        }
        logger.lifecycle("    BRouter $brouterVersion staged.")
    }
}

val fetchBrouterSegments by tasks.registering {
    description = "Downloads the BRouter routing segments (.rd5) covering NL."
    group = "offline-data"
    val needs = brouterSegments.any { !File(brouterSegmentsDir, it).exists() }
    onlyIf { needs }
    doLast {
        brouterSegmentsDir.mkdirs()
        for (name in brouterSegments) {
            val out = File(brouterSegmentsDir, name)
            if (out.exists() && out.length() > 0) continue
            download("$brouterSegmentBaseUrl/$name", out)
        }
    }
}

val buildNlMbtiles by tasks.registering {
    description = "Builds the NL MBTiles raster pyramid via scripts/build_nl_mbtiles.py."
    group = "offline-data"
    onlyIf { !mbtilesFile.exists() || mbtilesFile.length() == 0L }
    doLast {
        val script = rootProject.file("scripts/build_nl_mbtiles.py")
        check(script.exists()) { "Missing $script" }
        // Python is required only on the very first build (or after the
        // mbtiles file is deleted). Most devs have python3 installed; pick
        // whichever interpreter is on PATH.
        val python = listOf("python3", "python").firstOrNull { exe ->
            runCatching {
                ProcessBuilder(exe, "--version")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor() == 0
            }.getOrDefault(false)
        } ?: error(
            "Python is required to build the offline tile pack the first time, " +
                "but neither 'python3' nor 'python' is on PATH. Either install Python " +
                "or download a prebuilt nl.mbtiles to ${mbtilesFile}."
        )
        logger.lifecycle("    building $mbtilesFile via $python (this can take 5–15 min)…")
        val proc = ProcessBuilder(python, script.absolutePath)
            .directory(rootProject.projectDir)
            .redirectErrorStream(true)
            .start()
        proc.inputStream.bufferedReader().forEachLine { logger.lifecycle("      $it") }
        val rc = proc.waitFor()
        check(rc == 0) { "build_nl_mbtiles.py exited with status $rc" }
    }
}

val setupOfflineData by tasks.registering {
    description = "Ensures all bundled offline assets (BRouter + MBTiles) are present."
    group = "offline-data"
    dependsOn(fetchBrouterDist, fetchBrouterSegments, buildNlMbtiles)
}

// Wire into preBuild so any gradle build task runs setupOfflineData first.
// AGP creates `preBuild` lazily, so we hook on afterEvaluate.
afterEvaluate {
    tasks.named("preBuild").configure { dependsOn(setupOfflineData) }
}

android {
    namespace = "com.example.emergency"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.emergency"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    androidResources {
        // Routing segments and the MBTiles pack are already binary/SQLite —
        // AAPT compression buys nothing and forces an unnecessary decompress
        // step before we can copy them out to filesDir. .brf/.dat profiles
        // are tiny but kept as-is for the same reason. Keeping them
        // uncompressed also lets OfflineAssets fast-path the copy via
        // AssetManager.openFd() + FileChannel.transferTo() (sendfile(2)).
        noCompress += listOf("rd5", "mbtiles", "brf", "dat")
    }

    packaging {
        resources {
            // BRouter's all-jar bundles protobuf which carries license files
            // that collide with other AARs on Maven Central if we ever pull
            // one in. Excluding them up-front avoids future merge errors.
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/proguard/**",
                "META-INF/io.netty.versions.properties",
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.services.location)
    // Gemma LLM
    implementation(libs.litertlm.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // Image loading
    implementation(libs.coil.compose)
    // Markdown rendering
    implementation(libs.compose.markdown)
    // Interactive map (MapLibre — uses Mapbox SDK 6 namespace)
    implementation(libs.maplibre.android.sdk)
    // Offline routing engine (BRouter, vendored as a JAR fetched at build
    // time by the setupOfflineData task). The all-jar bundles protobuf +
    // osmosis-osm-binary so no transitive deps are needed.
    implementation(files("libs/brouter.jar"))
    // Localhost tile server: serves /{z}/{x}/{y}.png from the bundled
    // MBTiles SQLite to MapLibre, which doesn't ship an mbtiles:// scheme
    // handler.
    implementation(libs.nanohttpd)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}