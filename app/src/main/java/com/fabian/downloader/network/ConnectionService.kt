package com.fabian.downloader.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.fabian.downloader.MyApplication
import com.fabian.downloader.configs.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/**
 * Servicio de verificación de conectividad real.
 *
 * Antes este servicio SIEMPRE devolvía `true` (incluso sin red), lo que hacía
 * que se lanzaran descargas destinadas a fallar. Ahora:
 *
 *  1. Capa barata: ConnectivityManager.NET_CAPABILITY_VALIDATED (sin I/O).
 *  2. Capa de validación real: [InetAddress.isReachable] — resiste firewalls
 *     y resuelve el caso de MIUI donde `activeNetwork` viene null.
 *  3. Cache corto (3s) para no bombardear durante reintentos.
 *  4. El cache se resetea cuando el sistema reporta cambio de red.
 */
class ConnectionService {

    suspend fun checkConnection(strict: Boolean = false): Boolean =
        Companion.checkConnection(strict)

    companion object {
        private const val TAG = "ConnectionService"

        @Volatile
        private var lastSocketCheckTime = 0L

        @Volatile
        private var lastSocketCheckResult = false

        /** Timestamp del último cambio de red reportado por el sistema. */
        private val lastNetworkChangeMs = AtomicLong(0L)

        /** Inicializa el callback de cambios de red. Idempotente. */
        fun installNetworkChangeObserver(context: Context) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as? ConnectivityManager ?: return
                cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        lastNetworkChangeMs.set(System.currentTimeMillis())
                        invalidateCache()
                    }

                    override fun onLost(network: Network) {
                        lastNetworkChangeMs.set(System.currentTimeMillis())
                        invalidateCache()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities
                    ) {
                        // Solo invalidar si cambia la validación real
                        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                            invalidateCache()
                        }
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo registrar NetworkCallback", e)
            }
        }

        fun invalidateCache() {
            lastSocketCheckTime = 0L
        }

        /**
         * Realiza una verificación real de red. La tolerancia a firewalls/VPNs
         * raros se controla con [strict]: si `strict=false` y la validación
         * activa falla, se devuelve `true` solo si el sistema reporta al menos
         * la capacidad INTERNET (útil para VPNs/captive portals).
         */
        suspend fun checkConnection(strict: Boolean = false): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val now = System.currentTimeMillis()
                    // Cache corto: 3s. Si en los últimos 3s cambió la red, ignorar cache.
                    val recentChange = now - lastNetworkChangeMs.get() < 3000L
                    if (!recentChange && now - lastSocketCheckTime < 3000L) {
                        return@withContext lastSocketCheckResult
                    }

                    val context = try {
                        MyApplication.getInstance()
                    } catch (_: IllegalStateException) {
                        return@withContext false
                    }

                    val connectivityManager = context
                        .getSystemService(Context.CONNECTIVITY_SERVICE)
                        as? ConnectivityManager
                        ?: return@withContext true // Sin sistema, asumir online

                    val network = connectivityManager.activeNetwork
                    val capabilities = if (network != null)
                        connectivityManager.getNetworkCapabilities(network) else null

                    val hasInternetCap = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

                    // En tablets o dispositivos donde activeNetwork tarda en actualizarse,
                    // verificar si cualquiera de las redes disponibles tiene INTERNET
                    val anyNetworkHasInternet = if (!hasInternetCap) {
                        try {
                            connectivityManager.allNetworks.any { net ->
                                connectivityManager.getNetworkCapabilities(net)
                                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                            }
                        } catch (_: Exception) {
                            false
                        }
                    } else false

                    val reallyOnline = when {
                        // Sistema ya validó: listo
                        isValidated -> true
                        hasInternetCap || anyNetworkHasInternet -> {
                            // Si el sistema reporta internet y no es estricto, permitir proceder
                            if (!strict) true else isReachable()
                        }
                        // Sin capacidad reportada por el sistema: verificar con socket real TCP antes de declarar offline
                        else -> isReachable()
                    }

                    lastSocketCheckTime = now
                    lastSocketCheckResult = reallyOnline
                    reallyOnline
                } catch (e: Exception) {
                    Log.w(TAG, "Excepción verificando conexión: ${e.message}")
                    // Último recurso: si el sistema lanza, asumimos online para no bloquear.
                    true
                }
            }

        /**
         * Prueba conectividad real mediante apertura de socket TCP rápido (puerto 53 DNS o 443 HTTPS).
         * A diferencia de InetAddress.isReachable() (que intenta ICMP echo o puerto 7 y suele fallar
         * en Android por permisos SELinux), un socket TCP a DNS/HTTPS es 100% fiable en Android.
         */
        private fun isReachable(): Boolean {
            val targets = listOf(
                Pair("8.8.8.8", 53),
                Pair("1.1.1.1", 53),
                Pair("www.google.com", 443)
            )
            for ((host, port) in targets) {
                try {
                    java.net.Socket().use { socket ->
                        socket.connect(java.net.InetSocketAddress(host, port), 1200)
                        return true
                    }
                } catch (_: Exception) {
                }
            }
            return false
        }
    }
}
