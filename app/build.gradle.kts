plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.ani"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "app.ani"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "2.0"
        // Optional remote resolver URL. Empty means: cached copy, then the bundled resolver.js.
        val resolverUrl = (project.findProperty("resolverUrl") as String?)
            ?: System.getenv("RESOLVER_URL")
            ?: ""
        buildConfigField("String", "RESOLVER_URL", "\"" + resolverUrl + "\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The bundled resolver is the repo-root resolver.js (single source of truth), copied into assets at build time.
val copyResolver = tasks.register<Copy>("copyResolver") {
    from(rootProject.file("resolver.js"))
    into(layout.buildDirectory.dir("generated/resolverAssets"))
}
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/resolverAssets").get().asFile)
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(copyResolver) }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    implementation(libs.quickjs.kt)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.database)
    implementation(libs.media3.datasource)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
