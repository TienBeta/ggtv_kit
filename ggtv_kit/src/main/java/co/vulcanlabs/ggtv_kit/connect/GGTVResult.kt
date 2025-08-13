package co.vulcanlabs.ggtv_kit.connect

sealed interface GGTVResult<out T> {
    data class Success<T>(val data: T) : GGTVResult<T>
    data class Error(val cause: Throwable? = null) : GGTVResult<Nothing>

    companion object {
        fun <T> success(data: T): GGTVResult<T> = Success(data)
        fun error(cause: Exception? = null): GGTVResult<Nothing> = Error(cause)
    }
}
/**
 * Extension functions for result handling
 */
inline fun <T> GGTVResult<T>.onSuccess(action: (T) -> Unit): GGTVResult<T> {
    if (this is GGTVResult.Success) action(data)
    return this
}

inline fun <T> GGTVResult<T>.onError(action: (Throwable?) -> Unit): GGTVResult<T> {
    if (this is GGTVResult.Error) action(cause)
    return this
}

/**
 * Handle specific GGTV exceptions with type-safe error handling
 */
inline fun <T> GGTVResult<T>.onNotConnected(action: (GGTVException.NotConnectedException) -> Unit): GGTVResult<T> {
    if (this is GGTVResult.Error && cause is GGTVException.NotConnectedException) {
        action(cause)
    }
    return this
}

inline fun <T> GGTVResult<T>.onConnectionFailed(action: (GGTVException.ConnectionFailedException) -> Unit): GGTVResult<T> {
    if (this is GGTVResult.Error && cause is GGTVException.ConnectionFailedException) {
        action(cause)
    }
    return this
}

inline fun <T> GGTVResult<T>.onPairingRequired(action: () -> Unit): GGTVResult<T> {
    if (this is GGTVResult.Error && cause is GGTVException.PairingRequiredException) {
        action.invoke()
    }
    return this
}

inline fun <T> GGTVResult<T>.onPairingFailed(action: () -> Unit): GGTVResult<T> {
    if (this is GGTVResult.Error && cause is GGTVException.PairingFailedException) {
        action.invoke()
    }
    return this
}


inline fun <T, R> GGTVResult<T>.map(transform: (T) -> R): GGTVResult<R> {
    return when (this) {
        is GGTVResult.Success -> GGTVResult.success(transform(data))
        is GGTVResult.Error -> this
    }
}

fun <T> GGTVResult<T>.getOrNull(): T? = when (this) {
    is GGTVResult.Success -> data
    is GGTVResult.Error -> null
}

fun <T> GGTVResult<T>.getOrThrow(): T = when (this) {
    is GGTVResult.Success -> data
    is GGTVResult.Error -> throw cause ?: kotlin.RuntimeException()
}