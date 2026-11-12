import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

fun signingValue(property: String, env: String): String? {
    val fromFile = keystoreProperties.getProperty(property)?.takeIf { it.isNotBlank() }
    val fromEnv = System.getenv(env)?.takeIf { it.isNotBlank() }
    return fromFile ?: fromEnv
}

val releaseStorePath = signingValue("storeFile", "AQUALUX_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "AQUALUX_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "AQUALUX_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "AQUALUX_KEY_PASSWORD")
val releaseStore = releaseStorePath?.let { path ->
    // Project.file() always returns an absolute File, so a relative storeFile
    // must be tested with File.isAbsolute before resolving from the repo root.
    val raw = File(path)
    val resolved = if (raw.isAbsolute) raw else rootProject.file(path)
    resolved.takeIf { it.isFile }
}
val hasReleaseKeystore = releaseStore != null &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.aqualux.browser"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.aqualux.browser"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}"
        }
    }

    lint {
        abortOnError = true
        disable += setOf("UsesCleartextTraffic", "GradleDependency")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}

base {
    archivesName.set("aqualux")
}
