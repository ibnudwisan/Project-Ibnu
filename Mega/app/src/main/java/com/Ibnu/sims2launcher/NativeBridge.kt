package com.ibnu.sims2launcher

class NativeBridge {
    companion object {
        @Volatile private var loaded = false

        @Synchronized
        fun ensureLoaded(): Boolean {
            if (loaded) return true
            return try {
                System.loadLibrary("sims2_core_bridge")
                loaded = true
                true
            } catch (_: UnsatisfiedLinkError) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }

    fun initialize(): Boolean =
        ensureLoaded() && runCatching { nativeInitialize() }.getOrDefault(false)

    fun status(): String =
        if (ensureLoaded()) runCatching { nativeGetStatus() }.getOrDefault("Native bridge unavailable.")
        else "Native library unavailable."

    fun startGame(isoPath: String): Boolean =
        ensureLoaded() && runCatching { nativeStartGame(isoPath) }.getOrDefault(false)

    private external fun nativeInitialize(): Boolean
    private external fun nativeGetStatus(): String
    private external fun nativeStartGame(path: String): Boolean
}
