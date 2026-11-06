pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    // Maven Central limita con 429 y un 429 aborta la resolución entera. El
    // portal (plugins.gradle.org) además redirige a Maven Central, así que se
    // quitan ambos y se usa el mirror completo de Central (aliyun). Los ids de
    // plugins se mapean a artefactos reales más abajo con resolutionStrategy.
    maven { url = uri("https://maven.aliyun.com/repository/public") } // mirror de Maven Central sin rate-limit
  }
  // Resolución offline: los "plugin markers" no estaban en la caché y Maven
  // Central devolvía 429. Se mapea cada id de plugin directamente al artefacto
  // real (sí presente en ~/.gradle), evitando por completo los markers.
  resolutionStrategy {
    eachPlugin {
      when (requested.id.id) {
        "com.android.application" ->
          useModule("com.android.tools.build:gradle:9.1.1")
        "org.jetbrains.kotlin.plugin.compose" ->
          useModule("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
        "com.google.devtools.ksp" ->
          useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.10-2.0.2")
        "io.github.takahirom.roborazzi" ->
          useModule("io.github.takahirom.roborazzi:roborazzi-gradle-plugin:1.59.0")
        "com.google.android.libraries.mapsplatform.secrets-gradle-plugin" ->
          useModule("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
      }
    }
  }
}

// foojay-resolver desactivado: sólo sirve para auto-descargar JDKs;
// el JDK local es suficiente y Maven Central estaba devolviendo 429.
// plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    maven { url = uri("https://maven.aliyun.com/repository/public") } // mirror de Maven Central sin rate-limit
    maven { url = uri("https://jitpack.io") }
  }
}

rootProject.name = "FabiDownloader"

include(":app")
