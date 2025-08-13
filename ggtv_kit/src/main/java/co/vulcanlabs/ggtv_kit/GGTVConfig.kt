package co.vulcanlabs.ggtv_kit

/**
 * Configuration for GGTV
 */
data class GGTVConfig(
    val connectionTimeoutMs: Long = 5_000L,
    val commandTimeoutMs: Long = 3_000L,
    val enableLogging: Boolean = true
) {
    companion object {
        val DEFAULT = GGTVConfig()
    }
}
