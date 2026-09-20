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
  val versionLines = if (versionFile.exists()) versionFile.readLines() else listOf("3.48.1", "131")
  val parsedVersionName = versionLines.getOrNull(0)?.trim() ?: "3.48.0"
  val parsedVersionCode = versionLines.getOrNull(1)?.trim()?.toIntOrNull() ?: 130

  defaultConfig {
    applicationId = "com.fabian.downloader"
    minSdk = 24
    targetSdk = 37
    versionCode = parsedVersionCode
    versionName = parsedVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk {
        // x86/x86_64 incluidos: la librería youtubedl-android 0.18.1 los empaqueta y son
        // necesarios para probar en emuladores. Sin la libpython nativa de la ABI del
        // dispositivo, YoutubeDL.init falla y la app no descarga nada.
        abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
    }
  }

  androidResources {
    localeFilters += listOf("es", "en", "de", "fr", "ja", "ru")
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
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

  // ============================================================================
  // Issue 3.6 — Signing configs seguras (sin fallback a debug keystore en release)
  // ============================================================================
  //
  // Antes: si KEYSTORE_PATH no estaba definido o el archivo no existía, la build
  // de release se firmaba con el DEBUG keystore (público y conocido). Cualquiera
  // podía firmar una APK con el mismo certificado y hacer que se actualice sobre
  // tu app.
  //
  // Ahora: el signing config de release SÓLO se configura si las variables de
  // entorno necesarias están presentes. Si no lo están:
  //   - En builds locales (-PdebugBuild): se usa el debug keystore explícitamente.
  //   - En builds de release CI: falla con error claro en lugar de usar debug.
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
      val keystorePath = System.getenv("KEYSTORE_PATH")
      val storePwd = System.getenv("STORE_PASSWORD")
      val alias = System.getenv("KEY_ALIAS")
      val keyPwd = System.getenv("KEY_PASSWORD")

      if (keystorePath != null && storePwd != null && alias != null && keyPwd != null) {
        val keystoreFile = file(keystorePath)
        if (!keystoreFile.exists()) {
          throw GradleException(
            "KEYSTORE_PATH apunta a un archivo inexistente: ${keystoreFile.absolutePath}"
          )
        }
        storeFile = keystoreFile
        storePassword = storePwd
        keyAlias = alias
        keyPassword = keyPwd
      } else {
        // No hay variables de entorno para release. El buildType release fallará
        // explícitamente salvo que se use -PdebugBuild.
        project.logger.warn(
          "SignningConfig release: variables de entorno incompletas. " +
          "Las builds de release fallarán salvo -PdebugBuild."
        )
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseSigning = signingConfigs.getByName("release")
      val debugSigning = signingConfigs.getByName("debug")
      val useDebugBuild = project.hasProperty("debugBuild")
      when {
        releaseSigning.storeFile != null -> signingConfig = releaseSigning
        useDebugBuild -> {
          project.logger.warn("Build de release firmado con DEBUG keystore (-PdebugBuild). " +
            "NO publicar esta APK en producción.")
          signingConfig = debugSigning
        }
        else -> throw GradleException(
          "Build de release requiere configurar KEYSTORE_PATH, STORE_PASSWORD, " +
          "KEY_ALIAS y KEY_PASSWORD (env vars), o pasar -PdebugBuild para builds locales."
        )
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

  // ============================================================================
  // Issue 3.5 — Lint rehabilitado
  // ============================================================================
  //
  // Antes: abortOnError=false, checkReleaseBuilds=false, ignoreWarnings=true.
  // Esto silenciaba TODOS los warnings de lint, incluyendo bugs reales.
  //
  // Ahora: lint aborta en errores, checkea builds de release, y usa un baseline
  // para permitir fix gradual. Genera el baseline con:
  //   ./gradlew updateLintBaseline
  lint {
    abortOnError = true
    checkReleaseBuilds = true
    warningsAsErrors = false
    // Exclusiones explícitas y justificadas. NO silencies warnings sin razón.
    disable += setOf(
      "MissingTranslation",       // traducciones llegan incrementalmente
      "ExtraTranslation"          // claves legacy que aún no se borran
    )
    // Baseline: snapshot de issues existentes. Solo NUEVOS issues bloquean el build.
    // Si no existe el archivo, lint reporta todo (útil para detectar regresiones).
    baseline = file("lint-baseline.xml")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
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
  // ProcessLifecycleOwner para detección fiable de foreground/background (Issue 5.2)
  implementation("androidx.lifecycle:lifecycle-process:2.8.7")
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
  // "ksp"(libs.moshi.kotlin.codegen)
}
