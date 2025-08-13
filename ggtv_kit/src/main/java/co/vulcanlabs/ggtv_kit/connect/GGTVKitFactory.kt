package co.vulcanlabs.ggtv_kit.connect

import android.content.Context
import co.vulcanlabs.ggtv_kit.BuildConfig

object GGTVKitFactory {
    /**
     * Create GGTVKit instance
     * @param context Application context
     * @return GGTVKit instance
     */
    fun create(
        context: Context,
        enableLog: Boolean = BuildConfig.DEBUG,
        clientName: String
    ): GGTVKit {
        return GGTVKitImpl(clientName, context, enableLog)
    }

}