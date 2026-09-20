package com.fabian.downloader.network

import com.fabian.downloader.BuildConfig
import com.fabian.downloader.configs.Config
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP único para la app.
 *
 * Antes el interceptor añadía SIEMPRE el User-Agent móvil, pisando el UA
 * específico de cada petición (Instagram, YouTube Music, GitHub API, etc.).
 * Ahora solo añade un UA por defecto si la petición no trae uno propio.
 *
 * Antes el `HttpLoggingInterceptor` estaba configurado en `NONE`, por lo que
 * era código muerto. Ahora se activa solo en builds de debug.
 */
object NetworkClient {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                // Solo loguear en debug; en release NONE para no exponer datos.
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BASIC
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor { chain ->
                val request = chain.request()
                // Solo añadir UA por defecto si la petición no trae uno propio.
                // Esto permite que cada caller (UpdateManager, YtdlpUpdateManager,
                // site services) use el UA que necesite sin que se sobreescriba.
                val finalRequest = if (request.header("User-Agent") == null) {
                    request.newBuilder()
                        .header("User-Agent", Config.UA_MOBILE)
                        .build()
                } else {
                    request
                }
                chain.proceed(finalRequest)
            }
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
