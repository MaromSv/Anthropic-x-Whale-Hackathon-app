plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        // are tiny but kept as-is for the same reason.
        noCompress += listOf("rd5", "mbtiles", "brf", "dat")
    }

    packaging {
        resources {
            // BRouter's all-jar bundles protobuf which carries a license file
            // that collides with other AARs on Maven Central if we ever pull
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
    // Offline routing engine (BRouter, vendored as a JAR). The all-jar
    // bundles protobuf + osmosis-osm-binary so no transitive deps are needed.
    implementation(files("libs/brouter.jar"))
    // Localhost tile server: serves /{z}/{x}/{y}.png from the bundled MBTiles
    // SQLite to MapLibre, which doesn't ship an mbtiles:// scheme handler.
    implementation(libs.nanohttpd)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}