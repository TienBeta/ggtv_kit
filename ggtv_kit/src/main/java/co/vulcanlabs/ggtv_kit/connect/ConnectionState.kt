package co.vulcanlabs.ggtv_kit.connect

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

/**
 * Represents the connection state of GGTV
 */
@Keep
sealed interface ConnectionState : Parcelable {
    @Parcelize
    object Disconnected : ConnectionState

    @Parcelize
    object Connecting : ConnectionState

    @Parcelize
    data class Connected(val deviceInfo: GGTVDevice? = null) : ConnectionState
    @Parcelize
    object PairingRequired : ConnectionState

    @Parcelize
    data class ConnectError(val exception: GGTVException) : ConnectionState
}
