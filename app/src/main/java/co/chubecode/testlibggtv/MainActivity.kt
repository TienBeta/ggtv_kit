package co.chubecode.testlibggtv

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import co.vulcanlabs.ggtv_kit.connect.ConnectionState
import co.vulcanlabs.ggtv_kit.connect.GGTVKeys
import co.vulcanlabs.ggtv_kit.connect.GGTVKitFactory
import co.vulcanlabs.ggtv_kit.connect.PairingState
import co.vulcanlabs.ggtv_kit.connect.onError
import co.vulcanlabs.ggtv_kit.connect.onPairingFailed
import co.vulcanlabs.ggtv_kit.connect.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var etTvIpAddress: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnPower: Button
    private lateinit var btnHome: Button
    private lateinit var btnBack: Button
    private lateinit var btnVolumeUp: Button
    private lateinit var btnVolumeDown: Button
    private lateinit var btnOk: Button
    private lateinit var btnUp: Button
    private lateinit var btnDown: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnMenu: Button
    private lateinit var btnCast: Button
    private lateinit var btnYoutube: Button

    private var isConnected = false
    private var ggtvKit = GGTVKitFactory.create(
        clientName = "GGTVKitTestApp",
        context = this@MainActivity, enableLog = true
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializePython()
        initializeViews()
        setupClickListeners()
    }

    private fun initializePython() {
        lifecycleScope.launch {
            ggtvKit.init()
        }
    }

    private fun initializeViews() {
        etTvIpAddress = findViewById(R.id.etTvIpAddress)
        btnConnect = findViewById(R.id.btnConnect)
        btnPower = findViewById(R.id.btnPower)
        btnHome = findViewById(R.id.btnHome)
        btnBack = findViewById(R.id.btnBack)
        btnVolumeUp = findViewById(R.id.btnVolumeUp)
        btnVolumeDown = findViewById(R.id.btnVolumeDown)
        btnOk = findViewById(R.id.btnOk)
        btnUp = findViewById(R.id.btnUp)
        btnDown = findViewById(R.id.btnDown)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnMenu = findViewById(R.id.btnMenu)
        btnCast = findViewById(R.id.btnCast)
        btnYoutube = findViewById(R.id.btnYoutube)
    }

    private fun setupClickListeners() {
        btnConnect.setOnClickListener {
            val ipAddress = etTvIpAddress.text.toString().trim()
            if (ipAddress.isNotEmpty()) {
                if (!isConnected) {
                    connectToTV(ipAddress)
                } else {
                    disconnectFromTV()
                }
            } else {
                Toast.makeText(this, "Please input TV ip address", Toast.LENGTH_SHORT).show()
            }
        }

        // Control buttons
        btnPower.setOnClickListener { sendKey(GGTVKeys.POWER) }
        btnHome.setOnClickListener { sendKey(GGTVKeys.HOME) }
        btnBack.setOnClickListener { sendKey(GGTVKeys.BACK) }
        btnMenu.setOnClickListener { sendKey(GGTVKeys.MENU) }

        val appLink = "market://launch?id=org.xbmc.kodi"

        btnCast.setOnClickListener { openApp(appLink) }

        // D-Pad buttons
        btnUp.setOnClickListener { sendKey(GGTVKeys.UP) }
        btnDown.setOnClickListener { sendKey(GGTVKeys.DOWN) }
        btnLeft.setOnClickListener { sendKey(GGTVKeys.LEFT) }
        btnRight.setOnClickListener { sendKey(GGTVKeys.RIGHT) }
        btnOk.setOnClickListener { sendKey(GGTVKeys.CENTER) }

        // Volume buttons
        btnVolumeUp.setOnClickListener { sendKey(GGTVKeys.VOLUME_UP) }
        btnVolumeDown.setOnClickListener { sendKey(GGTVKeys.VOLUME_DOWN) }
//        btnMute.setOnClickListener { sendKey("KEYCODE_MUTE") }
//
//         App buttons
        btnYoutube.setOnClickListener { openApp("org.videolan.vlc") }
//        btnNetflix.setOnClickListener { openApp("Netflix") }
    }

    private fun connectToTV(ipAddress: String) {
        btnConnect.isEnabled = false
        btnConnect.text = "Connecting..."
        lifecycleScope.launch {
            ggtvKit.connect(ipAddress).onSuccess { device ->
                isConnected = true
                btnConnect.text = "Disconnect"
                Toast.makeText(
                    this@MainActivity,
                    "Connect successful! ${device.name}",
                    Toast.LENGTH_SHORT
                ).show()

            }.onError {}
            ggtvKit.observeConnectionState().collect { state ->
                when (state) {
                    ConnectionState.PairingRequired -> {
                        checkPairingRequired()
                    }

                    else -> {}
                }
            }
            ggtvKit.observePairingState().collect { pairingState ->
                when (pairingState) {
                    PairingState.None -> {}
                    is PairingState.PairingFailed -> {
                        checkPairingRequired()
                    }

                    PairingState.PairingSuccess -> {}
                }
            }
        }
    }

    private fun checkPairingRequired() {
        lifecycleScope.launch(Dispatchers.Main) {          // Hiển thị dialog để nhập pairing code
            val builder = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Need pairing with TV")
            builder.setMessage("Check on TV screen to get pairing code, and put in here:")

            val input = EditText(this@MainActivity)
            input.inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            input.hint = "Input pairing code here"
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, _ ->
                val pairingCode = input.text.toString().trim()
                if (pairingCode.isNotEmpty()) {
                    performPairingWithCode(pairingCode)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Enter pairing code show on TV",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                btnConnect.text = "Connect"
                btnConnect.isEnabled = true
            }

            builder.setCancelable(false)
            builder.show()
        }
    }

    private fun performPairingWithCode(pairingCode: String) {
        lifecycleScope.launch {
            btnConnect.text = "pairing..."

            withContext(Dispatchers.IO) {
                val result = ggtvKit.sendPairing(pairingCode)
                result.onSuccess { device ->
                    withContext(Dispatchers.Main) {
                        isConnected = true
                        btnConnect.text = "Disconnect ${device.name}"
                        Toast.makeText(
                            this@MainActivity,
                            "Pairing and connect successful ${device.name}!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onPairingFailed {
                    Toast.makeText(this@MainActivity, "Pairing failed.", Toast.LENGTH_LONG)
                        .show()

                }

            }

        }
    }

    private fun disconnectFromTV() {
        ggtvKit.close()
        isConnected = false
        btnConnect.text = "Connect"
        Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()

    }

    private fun sendKey(keyCode: String) {
        lifecycleScope.launch {
            ggtvKit.sendKey(keyCode)
        }
    }

    private fun openApp(appName: String) {
        lifecycleScope.launch {
            ggtvKit.openAppByName(appName)
        }
    }
    private fun openAppLink(appLink: String) {
        lifecycleScope.launch {
            ggtvKit.sendAppLink(appLink)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (isConnected) {
            try {
                ggtvKit.close()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}