package co.vulcanlabs.ggtv_kit.connect

import androidx.annotation.Keep
import com.chaquo.python.PyException

/**
 * Custom exceptions for GGTV operations
 */
@Keep
sealed class GGTVException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotInitializedException : GGTVException("GGTV chưa được khởi tạo. Gọi init() trước.")
    class NotConnectedException : GGTVException("Chưa kết nối đến TV. Gọi connect() trước.")
    class ConnectionFailedException(message: String, cause: Throwable? = null) :
        GGTVException("Kết nối thất bại: $message", cause)

    class TimeOutException : GGTVException("timeout 3s")
    class PairingRequiredException : GGTVException("Yêu cầu pairing với TV")
    class PairingFailedException(message: String) : GGTVException("Pairing thất bại: $message")
    class PythonException(message: String, cause: Throwable? = null) :
        GGTVException("Python error: $message", cause)
}

fun Exception.mapToGGTVException(): GGTVException = when (this) {
    is GGTVException -> this
    else -> GGTVException.PythonException(this.message ?: "PyException", this)
}
