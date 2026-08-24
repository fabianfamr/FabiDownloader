plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.fabian.downloader"
  compileSdk = 37

  val versionFile = rootProject.file("VERSION")
  val versionLines = if (versionFile.exists()) versionFile.readLines() else listOf("2.22.2", "62")
  val parsedVersionName = versionLines.getOrNull(0)?.trim() ?: "2.22.2"
  val parsedVersionCode = versionLines.getOrNull(1)?.trim()?.toIntOrNull() ?: 62

  defaultConfig {
    applicationId = "com.fabian.downloader"
    minSdk = 24
    targetSdk = 37
    versionCode = parsedVersionCode
    versionName = parsedVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
        abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
    }
  }

  androidResources {
    localeFilters += listOf("es", "en", "de", "fr", "ja", "ru")
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = true
    }
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
      keepDebugSymbols += listOf(
        "**/libpython.zip.so",
        "**/libffmpeg.zip.so",
        "**/*.zip.so"
      )
    }
    resources {
      pickFirsts.add("lib/**/libc++_shared.so")
      pickFirsts.add("lib/armeabi-v7a/libpython.zip.so")
      pickFirsts.add("lib/arm64-v8a/libpython.zip.so")
      pickFirsts.add("lib/armeabi-v7a/libFFmpeg.so")
      pickFirsts.add("lib/arm64-v8a/libFFmpeg.so")
      excludes.add("META-INF/DEPENDENCIES")
      excludes.add("META-INF/LICENSE")
      excludes.add("META-INF/LICENSE.txt")
      excludes.add("META-INF/license.txt")
      excludes.add("META-INF/NOTICE")
      excludes.add("META-INF/NOTICE.txt")
      excludes.add("META-INF/notice.txt")
      excludes.add("META-INF/ASL2.0")
      excludes.add("META-INF/AL2.0")
      excludes.add("META-INF/LGPL2.1")
      excludes.add("META-INF/*.kotlin_module")
      excludes.add("META-INF/licenses/**")
    }
  }

  signingConfigs {
    getByName("debug") {
      val debugKeystore = file("${rootDir}/debug.keystore")
      if (debugKeystore.exists()) {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.keystore"
      val keystoreFile = file(keystorePath)
      val debugKeystore = file("${rootDir}/debug.keystore")
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD") ?: "android"
        keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
      } else if (debugKeystore.exists()) {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (signingConfigs.getByName("release").storeFile != null) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        signingConfig = signingConfigs.getByName("debug")
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
    ignoreWarnings = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.

secrets {
  propertiesFileName = ".env"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.converter.kotlinx.serialization)
  implementation(libs.youtubedl.android)
  implementation(libs.youtubedl.ffmpeg)
  implementation(libs.coil.compose)
  implementation(libs.coil.video)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.robolectric:robolectric:4.14.1")
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
