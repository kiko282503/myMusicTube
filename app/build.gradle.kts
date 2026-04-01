plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.sqldelight")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

            // Navigation (JetBrains CMP fork)
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")

            // Lifecycle / ViewModel
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.4")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

            // Koin – core + Compose extensions
            api("io.insert-koin:koin-core:4.0.0")
            implementation("io.insert-koin:koin-compose:4.0.0")
            implementation("io.insert-koin:koin-compose-viewmodel:4.0.0")

            // SQLDelight – coroutines extensions (driver is in platform source sets)
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")

            // Ktor – HTTP client (engine in platform source sets)
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

            // kotlinx
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

            // Image loading – Coil 3 multiplatform
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation("io.coil-kt.coil3:coil-network-ktor2:3.0.4")
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.core:core-ktx:1.13.1")

            // ExoPlayer / Media3
            implementation("androidx.media3:media3-exoplayer:1.4.1")
            implementation("androidx.media3:media3-session:1.4.1")
            implementation("androidx.media3:media3-ui:1.4.1")
            implementation("androidx.media:media:1.7.0")

            // Koin Android
            implementation("io.insert-koin:koin-android:4.0.0")

            // SQLDelight Android driver
            implementation("app.cash.sqldelight:android-driver:2.0.2")

            // Ktor engine for Android
            implementation("io.ktor:ktor-client-okhttp:2.3.12")

            // OkHttp (direct downloads + NewPipe)
            implementation("com.squareup.okhttp3:okhttp:4.12.0")

            // NewPipe extractor
            implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.0")

            // Accompanist permissions
            implementation("com.google.accompanist:accompanist-permissions:0.36.0")

            // Coroutines Android
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

            // Coil OkHttp network
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
        }

        iosMain.dependencies {
            // SQLDelight native driver
            implementation("app.cash.sqldelight:native-driver:2.0.2")
            // Ktor Darwin engine
            implementation("io.ktor:ktor-client-darwin:2.3.12")
        }
    }
}

android {
    namespace = "com.musictube.player"
    compileSdk = 35

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.setSrcDirs(listOf("src/androidMain/res"))
    sourceSets["main"].java.setSrcDirs(emptyList<File>())  // KMP handles Java/Kotlin sources

    defaultConfig {
        applicationId = "com.musictube.player"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "mozilla/public-suffix-list.txt"
        }
    }
}

sqldelight {
    databases {
        create("MusicDatabase") {
            packageName.set("com.musictube.player.data.database")
        }
    }
}

// All dependencies are declared in kotlin {} source sets above.
// Legacy dependencies{} block removed in KMP migration.