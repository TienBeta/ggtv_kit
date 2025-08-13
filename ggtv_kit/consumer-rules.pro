# ============= consumer-rules.pro =============

# Keep GGTV public API
-keep public class co.vulcanlabs.ggtv_kit.connect.GGTVManager {
    public *;
}

-keep public class co.vulcanlabs.ggtv_kit.connect.GGTVDevice {
    *;
}

-keep public class co.vulcanlabs.ggtv_kit.connect.GGTVKeys {
    *;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Python/Chaquopy classes
-keep class com.chaquo.python.** { *; }
-keep class python.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep sealed classes
-keep class co.vulcanlabs.ggtv_kit.connect.ConnectionState$* {
    *;
}

-keep class co.vulcanlabs.ggtv_kit.connect.PairingState {
    *;
}


-keep class co.vulcanlabs.ggtv_kit.connect.GGTVResult$* {
    *;
}

# Keep companion objects
-keepclassmembers class * {
    public static **Companion Companion;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep exception classes
-keep public class co.vulcanlabs.ggtv_kit.connect.GGTVException {
    *;
}

-dontwarn org.jetbrains.annotations.**

# Optional Timber support - only keep if present
-dontwarn timber.log.**
-if class timber.log.Timber
-keep class timber.log.** { *; }

