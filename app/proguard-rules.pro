# ProGuard / R8 Configuration for FabiDownloader
# Optimized for stability, performance and successful obfuscation

# Preserve essential debugging attributes
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# JNI & Native Methods (Essential for C/C++ libraries and Python bridge)
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# Video Downloader Engine (YoutubeDL-Android & FFmpeg)
# ============================================================================
-keep class com.yausername.** { *; }
-keep interface com.yausername.** { *; }
-dontwarn com.yausername.**

# Apache Commons Compress & XZ (Essential for unpacking native Python & yt-dlp assets)
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
-keep class org.tukaani.xz.** { *; }
-dontwarn org.tukaani.xz.**

# ============================================================================
# Networking & HTTP Clients (OkHttp & Retrofit)
# ============================================================================
-dontwarn retrofit2.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn java.beans.**

# ============================================================================
# JSON Serialization (Moshi & Kotlinx.serialization)
# ============================================================================
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class * {
    @com.squareup.moshi.JsonClass public <init>(...);
}
-keep class * extends com.squareup.moshi.JsonAdapter {
    public <init>(com.squareup.moshi.Moshi, ...);
    public <init>(com.squareup.moshi.Moshi);
}

-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    *** Companion;
}

# ============================================================================
# Room Database (Reflection & Schema instantiation)
# ============================================================================
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
    *;
}
-keep class * extends androidx.room.RoomOpenHelper
-keep class com.fabian.downloader.database.** { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# ============================================================================
# Jetpack ViewModel & Lifecycle
# ============================================================================
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    public <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(...);
    public <init>();
}
-keep class com.fabian.downloader.ui.viewmodels.** { *; }

# ============================================================================
# Coil Image Loader
# ============================================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================================
# Kotlin Coroutines
# ============================================================================
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ============================================================================
# Application Services, Managers, Configs & Pipelines
# ============================================================================
-keep class com.fabian.downloader.configs.** { *; }
-keep class com.fabian.downloader.managers.** { *; }
-keep class com.fabian.downloader.pipeline.** { *; }
-keep class com.fabian.downloader.services.sites.** { *; }
-keep class com.fabian.downloader.services.** { *; }
-keep class com.fabian.downloader.receivers.** { *; }
-keep class com.fabian.downloader.workers.** { *; }
-keep class com.fabian.downloader.ui.AppSettings { *; }
