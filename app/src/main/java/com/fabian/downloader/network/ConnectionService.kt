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

                    val hasInternetCap =
                        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            == true
                    val isValidated =
                        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                            == true

                    val reallyOnline = when {
                        // Sistema ya validó: listo
                        isValidated -> true
                        // Sin capacidad INTERNET: definitivamente offline
                        !hasInternetCap -> false
                        // Capacidad INTERNET pero sin validación: probar reachability
                        else -> {
                            val reachable = isReachable()
                            if (reachable) true
                            // Modo no estricto: dejar que la descarga intente (firewall raro)
                            else if (!strict) {
                                Log.w(
                                    TAG,
                                    "Reachability falló pero el sistema reporta INTERNET. " +
                                        "Permitiendo intento (modo no estricto)."
                                )
                                true
                            } else false
                        }
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
         * Prueba reachability contra dos DNS públicos. Devuelve `true`
         * si al menos uno responde en menos de 2s.
         */
        private fun isReachable(): Boolean {
            val hosts = listOf("1.1.1.1", "8.8.8.8")
            for (host in hosts) {
                try {
                    val addr = java.net.InetAddress.getByName(host)
                    if (addr.isReachable(2000)) return true
                } catch (_: Exception) {
                    // Intentar siguiente host
                }
            }
            return false
        }
    }
}
