# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Rule to keep native libraries and JNI bindings for YoutubeDL and FFmpeg
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Retrofit and OkHttp rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi rules to prevent breaking JSON serialization/deserialization
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class * {
    @com.squareup.moshi.JsonClass public <init>(...);
}

# kotlinx.serialization rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializable classes and their fields
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

# OkHttp rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn java.beans.**
-dontwarn org.tukaani.xz.**

# Keep our Room entities and ViewModel fields to prevent DB column name mismatch
-keep class com.fabian.downloader.database.** { *; }
-keep class com.fabian.downloader.ui.**ViewModel { *; }
-keep class com.fabian.downloader.services.sites.SiteResult { *; }
-keep class com.fabian.downloader.services.sites.DownloadFormat { *; }

# Apache Commons Compress rules for YoutubeDL-Android
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-dontwarn org.apache.commons.compress.**


