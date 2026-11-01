import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Google's official AdMob test ids — always used by debug builds.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBanner = "ca-app-pub-3940256099942544/9214589741"
val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
val testRewarded = "ca-app-pub-3940256099942544/5224354917"

fun prop(name: String, fallback: String): String =
    (System.getenv(name) ?: project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback

// Release ids: real AdMob ids from secrets; a release with test ids would earn nothing, so it fails.
val releaseAdIds = listOf("ADMOB_APP_ID", "ADMOB_BANNER_ID", "ADMOB_INTERSTITIAL_ID", "ADMOB_REWARDED_ID")
    .associateWith { prop(it, "") }
val missingAdIds = releaseAdIds.filterValues { it.isBlank() || it.startsWith("ca-app-pub-3940256099942544") }.keys

// Release signing comes from environment variables (GitHub Actions secrets).
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.farias.prismaroll"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.farias.prismaroll"
        minSdk = 24
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.1"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBanner\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitial\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$testRewarded\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = releaseAdIds.getValue("ADMOB_APP_ID")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${releaseAdIds.getValue("ADMOB_BANNER_ID")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${releaseAdIds.getValue("ADMOB_INTERSTITIAL_ID")}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${releaseAdIds.getValue("ADMOB_REWARDED_ID")}\"")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val missing = missingAdIds.toList()
    doFirst {
        if (missing.isNotEmpty()) {
            throw GradleException("Release sem IDs reais do AdMob: ${missing.joinToString()} (crie os secrets no GitHub)")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.google.android.play:review:2.0.2")
}
