package co.vulcanlabs.ggtv_kit.connect

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber

internal class GGTVKitImpl(
    private val clientName: String,
    context: Context,
    private val enableLog: Boolean,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : GGTVKit {
    private val ggtvManager = GGTVManager(context)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.None)
    override suspend fun init(): GGTVResult<Unit> = withContext(dispatcher) {
        try {
            if (enableLog) {
                Timber.plant(Timber.DebugTree())
            }
            ggtvManager.init()
            Timber.i("GGTV initialized successfully")
            GGTVResult.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "GGTV initialization failed")
            GGTVResult.error(e.mapToGGTVException())
        }
    }

    override suspend fun connect(ipAddress: String): GGTVResult<GGTVDevice> =
        withContext(dispatcher) {
            try {
                val device = ggtvManager.connect(ipAddress,clientName)
                Timber.i("GGTV connect successfully")
                GGTVResult.success(device)
            } catch (exception: Exception) {
                when (exception) {
                    is GGTVException.PairingRequiredException -> {
                        val pairingState = ConnectionState.PairingRequired
                        _connectionState.value = pairingState
                        Timber.e(exception, "GGTV connect failed")
                        GGTVResult.error(exception)
                    }

                    else -> {
                        val exception = GGTVException.ConnectionFailedException(
                            "Unexpected error: ${exception.message}",
                            exception
                        )
                        Timber.e(exception, "GGTV connect failed")
                        _connectionState.value = ConnectionState.ConnectError(exception)
                        GGTVResult.error(exception)
                    }
                }
            }

        }

    override suspend fun sendPairing(code: String): GGTVResult<GGTVDevice> =
        withContext(dispatcher) {
            try {
                val device = withTimeout(3000) { ggtvManager.sendPairing(code) }
                Timber.i("GGTV pairing successfully")
                GGTVResult.success(device)
            } catch (exception: TimeoutCancellationException) {
                Timber.e(exception, "GGTV pairing timeout")
                val timeOutException = GGTVException.TimeOutException()
                _connectionState.value = ConnectionState.ConnectError(
                    timeOutException
                )
                GGTVResult.error(timeOutException)
            } catch (exception: Exception) {
                Timber.e(exception, "GGTV pairing failed")
                _connectionState.value =
                    ConnectionState.ConnectError(exception.mapToGGTVException())
                GGTVResult.error(exception)
            }
        }

    override suspend fun sendKey(key: String) = withContext(dispatcher) {
        if (_connectionState.value != ConnectionState.Connected()) {
            GGTVResult.error(GGTVException.NotConnectedException())
        }
        try {
            ggtvManager.transmitKey(key)
            Timber.i("GGTV sendkey successfully")
            GGTVResult.success(key)
        } catch (exception: Exception) {
            Timber.e(exception, "GGTV sendkey failed")
            _connectionState.value = ConnectionState.ConnectError(exception.mapToGGTVException())
            GGTVResult.error(exception)
        }

    }
    override suspend fun sendText(text: String) = withContext(dispatcher) {
        if (_connectionState.value != ConnectionState.Connected()) {
            GGTVResult.error(GGTVException.NotConnectedException())
        }
        try {
            ggtvManager.sendText(text)
            Timber.i("GGTV sendText successfully")
            GGTVResult.success(text)
        } catch (exception: Exception) {
            Timber.e(exception, "GGTV sendText failed")
            _connectionState.value = ConnectionState.ConnectError(exception.mapToGGTVException())
            GGTVResult.error(exception)
        }

    }

    override suspend fun sendAppLink(appLink: String): GGTVResult<String> = withContext(dispatcher) {
        if (_connectionState.value != ConnectionState.Connected()) {
            GGTVResult.error(GGTVException.NotConnectedException())
        }
        try {
            ggtvManager.transmitAppLink(appLink)
            Timber.i("GGTV transmitAppLink successfully")
            GGTVResult.success(appLink)
        } catch (exception: Exception) {
            Timber.e(exception, "GGTV transmitAppLink failed")
            _connectionState.value = ConnectionState.ConnectError(exception.mapToGGTVException())
            GGTVResult.error(exception)
        }

    }

    override suspend fun openAppByName(appName: String): GGTVResult<String> = withContext(dispatcher) {
        if (_connectionState.value != ConnectionState.Connected()) {
            GGTVResult.error(GGTVException.NotConnectedException())
        }
        try {
            ggtvManager.openApp(appName)
            Timber.i("GGTV openApp successfully")
            GGTVResult.success(appName)
        } catch (exception: Exception) {
            Timber.e(exception, "GGTV openApp failed")
            _connectionState.value = ConnectionState.ConnectError(exception.mapToGGTVException())
            GGTVResult.error(exception)
        }

    }

    override fun observeConnectionState(): Flow<ConnectionState> {
        return _connectionState.asStateFlow()
    }


    override fun observePairingState(): Flow<PairingState> {
        return _pairingState.asStateFlow()
    }

    override fun close() {
        ggtvManager.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        _pairingState.value = PairingState.None
    }

}