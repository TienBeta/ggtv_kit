// DeviceLibrary.kt
package co.vulcanlabs.ggtv_kit.connect

import kotlinx.coroutines.flow.Flow

interface GGTVKit {
    /**
     * Initialize lib with python
     * @return Result with ConnectionResult (Success or PairingRequired)
     */
    suspend fun init(): GGTVResult<Unit>
    /**
     * Connect to device
     * @return Result with GGTVDevice (Success or PairingRequired)
     */
    suspend fun connect(ipAddress: String): GGTVResult<GGTVDevice>
    
    /**
     * Send pairing code to device
     * @param code Pairing code from user
     * @return Result with GGTVDevice (Success or PairingFailed)
     */
    suspend fun sendPairing(code: String): GGTVResult<GGTVDevice>

    /**
     * Send key command to device (fire and forget)
     * @param key Key command (e.g., "VOLUME_UP", "VOLUME_DOWN", "MUTE")
     */
    suspend fun sendKey(key: String): GGTVResult<String>

    /**
     * Send app link command to device (fire and forget)
     * @param appLink appLink command "https://www.youtube.com,..."
     */
    suspend fun sendAppLink(appLink: String): GGTVResult<String>

    /**
     * Send text to device (fire and forget)
     * @param text text input from app
     */
    suspend fun sendText(text: String): GGTVResult<String>

    /**
     * Open app by name (fire and forget)
     * @param appName appName command "Youtube,NetFlix..."
     */
    suspend fun openAppByName(appName: String): GGTVResult<String>
    
    /**
     * Observe connection state changes
     * @return Flow of ConnectionState
     */
    fun observeConnectionState(): Flow<ConnectionState>

    /**
     * Observe pairing state changes
     * @return Flow of PairingState
     */
    fun observePairingState(): Flow<PairingState>
    
    /**
     * Close connection and cleanup resources
     */
    fun close()
}