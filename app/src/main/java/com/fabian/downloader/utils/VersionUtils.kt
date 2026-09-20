package com.fabian.downloader.utils

/**
 * Utilidades para comparar versiones semánticas o de fecha (YYYY.MM.DD).
 *
 * Unifica las dos implementaciones duplicadas que existían antes:
 *  - `UpdateManager.isNewerVersion` (simple, numérica).
 *  - `YtdlpUpdateManager.isNewerVersion` (con soporte para segmentos alfanuméricos).
 *
 * La implementación soporta:
 *  - Versiones semánticas: `1.2.3`, `3.48.1`, `0.9.5-beta`.
 *  - Versiones de fecha: `2025.01.15`, `2024.12.30`.
 *  - Segmentos numéricos y alfanuméricos mezclados.
 *  - Comparación lexicográfica de fallback para segmentos no numéricos.
 */
object VersionUtils {

    /**
     * Compara dos versiones.
     *
     * @param latest versión candidata a "más nueva".
     * @param current versión actual.
     * @return `true` si `latest` > `current`, `false` en caso contrario.
     */
    fun isNewer(latest: String, current: String): Boolean {
        if (current.isBlank() || current.equals("Unknown", ignoreCase = true)) {
            return latest.isNotBlank()
        }
        if (latest.isBlank()) return false
        if (latest == current) return false

        val lSegs = latest.trim().split(Regex("[.-]"))
        val cSegs = current.trim().split(Regex("[.-]"))

        for (i in 0 until maxOf(lSegs.size, cSegs.size)) {
            val l = lSegs.getOrNull(i) ?: return false // latest más corto → no es mayor
            val c = cSegs.getOrNull(i) ?: return true  // current más corto → latest es mayor

            val lNum = l.toLongOrNull()
            val cNum = c.toLongOrNull()

            if (lNum != null && cNum != null) {
                if (lNum > cNum) return true
                if (lNum < cNum) return false
            } else {
                // Al menos uno no es numérico: comparar lexicográficamente.
                val cmp = l.compareTo(c)
                if (cmp > 0) return true
                if (cmp < 0) return false
            }
        }
        return false
    }
}
