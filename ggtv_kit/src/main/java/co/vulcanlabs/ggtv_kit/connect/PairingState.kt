package co.vulcanlabs.ggtv_kit.connect

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the connection state of GGTV
 */
sealed interface PairingState : Parcelable {
    @Parcelize
    data class PairingFailed(val exception: GGTVException) : PairingState
    @Parcelize
    object PairingSuccess : PairingState
    @Parcelize
    object None : PairingState

}
