package co.vulcanlabs.ggtv_kit.connect

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Internal Python manager for GGTV
 */
internal class PythonManager {
    private val initMutex = Mutex()
    private var python: Python? = null
    private var isInitialized = false

    suspend fun initialize(context: Context): Result<Python> = initMutex.withLock {
        try {
            if (!isInitialized) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context.applicationContext))
                    Timber.d("Python started successfully")
                }
                python = Python.getInstance()
                isInitialized = true
                Timber.d("PythonManager initialized")
            }
            Result.success(python!!)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Python")
            Result.failure(GGTVException.PythonException("Failed to initialize Python", e))
        }
    }

    fun isInitialized(): Boolean = isInitialized

    fun executeCommand(functionName: String, vararg args: Any?): Result<Any?> {
        val moduleName = "ggtv"
        return try {
            val pyModule = python?.getModule(moduleName)
                ?: return Result.failure(GGTVException.NotInitializedException())

            val result = when (args.size) {
                0 -> pyModule.callAttr(functionName)
                1 -> pyModule.callAttr(functionName, args[0])
                2 -> pyModule.callAttr(functionName, args[0], args[1])
                else -> pyModule.callAttr(functionName, *args)
            }

            Timber.d("Python command executed: $moduleName.$functionName")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Python command failed: $moduleName.$functionName")
            if (e.message?.contains("PAIRING_REQUIRED") == true) {
                Result.failure(GGTVException.PairingRequiredException())
            } else {
                Result.failure(e)
            }
        }
    }
}
