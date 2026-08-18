# ProGuard / R8 Configuration for FabiDownloader
# Optimized for performance, code shrinking and strict obfuscation

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
# Essential keep rules for native execution, process management and callbacks
# ============================================================================
-keep class com.yausername.youtubedl_android.** { *; }
-keep interface com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

-keep class com.yausername.ffmpeg.** { *; }
-keep interface com.yausername.ffmpeg.** { *; }
-dontwarn com.yausername.ffmpeg.**

# Archive decompression used by native extractor
-dontwarn org.apache.commons.compress.**
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
# Keeps only annotated fields/constructors to allow maximum class obfuscation
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
# Room Database & Image Loader (Coil)
# ============================================================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-dontwarn coil.**

# ============================================================================
# Kotlin Coroutines
# ============================================================================
-dontwarn kotlinx.coroutines.**



