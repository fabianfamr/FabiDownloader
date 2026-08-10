# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve source file and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, *Annotation*

# JNI & Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# YoutubeDL-Android & FFmpeg
-keep class com.yausername.youtubedl_android.** { *; }
-keep interface com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

-keep class com.yausername.ffmpeg.** { *; }
-keep interface com.yausername.ffmpeg.** { *; }
-dontwarn com.yausername.ffmpeg.**

# Apache Commons Compress (used by YoutubeDL-Android)
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# Retrofit 2
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp 3 & Okio
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn java.beans.**
-dontwarn org.tukaani.xz.**

# Moshi rules to prevent breaking JSON serialization/deserialization
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class * {
    @com.squareup.moshi.JsonClass public <init>(...);
}

# kotlinx.serialization rules
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepnames class kotlinx.serialization.internal.**
-keepclassmembers class kotlinx.serialization.internal.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class com.fabian.downloader.database.** { *; }
-dontwarn androidx.room.**

# Coil Image & Video Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Application specific classes, models & pipelines to preserve JSON/reflection integrity
-keep class com.fabian.downloader.ui.**ViewModel { *; }
-keep class com.fabian.downloader.services.sites.** { *; }
-keep class com.fabian.downloader.pipeline.** { *; }
-keep class com.fabian.downloader.configs.** { *; }
-keep class com.fabian.downloader.managers.** { *; }


