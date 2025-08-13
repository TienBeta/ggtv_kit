package co.vulcanlabs.ggtv_kit.connect

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class GGTVDevice(
    val name: String? = null,
    val model: String? = null,
    val version: String? = null,
    val ipAddress: String? = null
) : Parcelable
