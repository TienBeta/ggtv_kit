package co.vulcanlabs.ggtv_kit.connect

import androidx.annotation.Keep

/**
 * Constants for Android TV key codes
 */
@Keep
object GGTVKeys {
    // Power & Navigation
    const val POWER = "KEYCODE_POWER"
    const val HOME = "KEYCODE_HOME"
    const val BACK = "KEYCODE_BACK"
    const val MENU = "KEYCODE_MENU"
    const val SETTINGS = "KEYCODE_SETTINGS"

    // D-Pad Navigation
    const val UP = "KEYCODE_DPAD_UP"
    const val DOWN = "KEYCODE_DPAD_DOWN"
    const val LEFT = "KEYCODE_DPAD_LEFT"
    const val RIGHT = "KEYCODE_DPAD_RIGHT"
    const val CENTER = "KEYCODE_DPAD_CENTER"
    const val OK = CENTER // Alias

    // Volume Control
    const val VOLUME_UP = "KEYCODE_VOLUME_UP"
    const val VOLUME_DOWN = "KEYCODE_VOLUME_DOWN"
    const val MUTE = "KEYCODE_MUTE"

    // Media Control
    const val PLAY_PAUSE = "KEYCODE_MEDIA_PLAY_PAUSE"
    const val PLAY = "KEYCODE_MEDIA_PLAY"
    const val PAUSE = "KEYCODE_MEDIA_PAUSE"
    const val STOP = "KEYCODE_MEDIA_STOP"
    const val NEXT = "KEYCODE_MEDIA_NEXT"
    const val PREVIOUS = "KEYCODE_MEDIA_PREVIOUS"
    const val FAST_FORWARD = "KEYCODE_MEDIA_FAST_FORWARD"
    const val REWIND = "KEYCODE_MEDIA_REWIND"

    // Numbers
    const val NUM_0 = "KEYCODE_0"
    const val NUM_1 = "KEYCODE_1"
    const val NUM_2 = "KEYCODE_2"
    const val NUM_3 = "KEYCODE_3"
    const val NUM_4 = "KEYCODE_4"
    const val NUM_5 = "KEYCODE_5"
    const val NUM_6 = "KEYCODE_6"
    const val NUM_7 = "KEYCODE_7"
    const val NUM_8 = "KEYCODE_8"
    const val NUM_9 = "KEYCODE_9"

    // Common Apps (Android TV specific)
    const val TV_INPUT = "KEYCODE_TV_INPUT"
    const val CHANNEL_UP = "KEYCODE_CHANNEL_UP"
    const val CHANNEL_DOWN = "KEYCODE_CHANNEL_DOWN"
    const val GUIDE = "KEYCODE_GUIDE"
    const val INFO = "KEYCODE_INFO"
    const val DEL = "KEYCODE_DEL"
}