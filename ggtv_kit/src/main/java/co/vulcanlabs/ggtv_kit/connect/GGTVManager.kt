// internal/DeviceManager.kt
package co.vulcanlabs.ggtv_kit.connect

import android.content.Context
import android.util.Log
import co.vulcanlabs.ggtv_kit.connect.getOrThrow
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.random.Random

internal class GGTVManager(
    private val context: Context
) {
    private var connectionState = ConnectionState.Disconnected
    private var tempIpAddress = ""
    private val pythonManager = PythonManager()

    private fun extractJsonValue(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    fun getDevice(ipAddress: String): GGTVDevice =
        pythonManager.executeCommand("get_device_info")
            .fold(
                onSuccess = { data ->
                    val jsonString = data.toString()
                    return GGTVDevice(
                        name = extractJsonValue(jsonString, "name"),
                        model = extractJsonValue(jsonString, "model"),
                        version = extractJsonValue(jsonString, "version"),
                        ipAddress = ipAddress
                    )
                },
                onFailure = {
                    throw it
                }
            )

    suspend fun init(): Boolean {
        pythonManager.initialize(context)
            .fold(
                onSuccess = {
                    return true
                },
                onFailure = {
                    throw it
                }
            )
    }

    fun connect(ip: String, appName: String): GGTVDevice {
        if (!pythonManager.isInitialized()) {
            throw GGTVException.NotInitializedException()
        }
        pythonManager.executeCommand("connect_to_tv", ip, appName)
            .fold(
                onSuccess = { pyResult ->
                    when {
                        pyResult.toString().toBoolean() -> {
                            val deviceInfo = getDevice(ip)
                            tempIpAddress = ip
                            return deviceInfo
                        }

                        else -> {
                            val message = "Unknown error !"
                            throw GGTVException.ConnectionFailedException(
                                message
                            )
                        }
                    }
                },
                onFailure = { exception ->
                    throw exception
                }
            )
    }

    fun sendPairing(code: String): GGTVDevice {
        if (!pythonManager.isInitialized()) {
            throw GGTVException.NotInitializedException()
        }
        pythonManager.executeCommand("finish_pairing", code)
            .fold(
                onSuccess = { pyResult ->
                    when {
                        pyResult.toString().toBoolean() -> {
                            val connectResult = pythonManager.executeCommand("retry_connection")
                                .getOrThrow()
                            if (connectResult.toString().toBoolean()) {
                                val deviceInfo = getDevice(tempIpAddress)
                                return deviceInfo
                            } else {
                                val message = "reconnect failed !"
                                throw GGTVException.ConnectionFailedException(
                                    message
                                )
                            }
                        }

                        else -> {
                            val message = "Pairing failed !"
                            throw GGTVException.ConnectionFailedException(
                                message
                            )
                        }
                    }
                },
                onFailure = { exception ->
                    throw exception
                }
            )
    }

    fun transmitKey(key: String): Boolean {
        if (!pythonManager.isInitialized()) {
            throw GGTVException.NotInitializedException()
        }
        pythonManager.executeCommand("send_key", key)
            .fold(
                onSuccess = {
                    Timber.d("Key sent: $key")
                    return true
                },
                onFailure = { exception ->
                    throw exception
                }
            )
    }
    fun sendText(text: String): Boolean {
        if (!pythonManager.isInitialized()) {
            throw GGTVException.NotInitializedException()
        }
        pythonManager.executeCommand("send_text", text)
            .fold(
                onSuccess = {
                    Timber.d("Text sent: $text")
                    return true
                },
                onFailure = { exception ->
                    throw exception
                }
            )
    }


    fun transmitAppLink(appLink: String): Boolean {
        if (!pythonManager.isInitialized()) {
            throw GGTVException.NotInitializedException()
        }
        pythonManager.executeCommand("send_app_link", appLink)
            .fold(
                onSuccess = {
                    Timber.d("AppLink sent: $appLink")
                    return true
                },
                onFailure = { exception ->
                    throw exception
                }
            )
    }

    fun openApp(appName: String): Boolean {
        if (!pythonManager.isInitialized()) {
            throw GGTVException.NotInitializedException()
        }
        pythonManager.executeCommand("open_app", appName)
            .fold(
                onSuccess = {
                    Timber.d("open App sent: $appName")
                    return true
                },
                onFailure = { exception ->
                    throw exception
                }
            )
    }

    fun disconnect() {

    }

    companion object {
        private const val TAG = "DeviceManager"
    }
}