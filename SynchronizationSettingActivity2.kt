
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

class SynchronizationSettingActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySynchronizationSetting2Binding

    private val PORT = 12345
    private val MASTER_IP = "192.168.0.230" // replace with Master device's IP address if known

    // Master related
    private val clientSockets = mutableListOf<Socket>()
    private var serverSocket: ServerSocket? = null
    private var isMasterRunning = false

    // Slave related
    private var clientSocket: Socket? = null
    private var isSlaveRunning = false
    private var changeColor = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySynchronizationSetting2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.masterBtn.setOnClickListener {
            if (isMasterRunning) {
                stopMaster()
                updateUIForMasterStopped()
            } else {
                startMaster()
                updateUIForMasterStarted()
            }
        }

        binding.slaveBtn.setOnClickListener {
            if (isSlaveRunning) {
                stopSlave()
                updateUIForSlaveDisconnected()
            } else {
                connectToMaster()
                updateUIForSlaveConnecting()
            }
        }

        binding.sendBtn.setOnClickListener {
            println("Senddddd")
            sendNextImageSignal();
        }
    }

    private fun updateUIForMasterStarted() {
        binding.masterBtn.text = "Stop Master"
        binding.slaveBtn.isEnabled = false
        binding.feedbackText.text = "Master mode active. Waiting for connections..."
    }

    private fun updateUIForMasterStopped() {
        binding.masterBtn.text = "Start Master"
        binding.slaveBtn.isEnabled = true
        binding.feedbackText.text = "Master mode stopped."
    }

    private fun updateUIForSlaveConnecting() {
        binding.slaveBtn.text = "Disconnect"
        binding.masterBtn.isEnabled = false
        binding.feedbackText.text = "Trying to connect to Master..."
    }


    private fun updateUIForSlaveDisconnected() {
        binding.slaveBtn.text = "Connect to Master"
        binding.masterBtn.isEnabled = true
        binding.feedbackText.text = "Disconnected from Master."
    }

    private fun startMaster() {
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                isMasterRunning = true
                while (isMasterRunning) {
                    val clientSocket = serverSocket!!.accept()
                    clientSockets.add(clientSocket)
                    runOnUiThread {
                        binding.feedbackText.text = ""
                        clientSockets.forEach {
                            binding.feedbackText.text =
                                "${binding.feedbackText.text}\n ${it.inetAddress}"
                        }
                    }


                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun stopMaster() {
        isMasterRunning = false
        serverSocket?.close()
        clientSockets.forEach { it.close() }
        clientSockets.clear()
    }

    private fun sendNextImageSignal() {
        Thread {
            for (socket in clientSockets) {
                try {
                    val output = OutputStreamWriter(socket.getOutputStream())
                    output.write("NEXT_IMAGE\n")
                    output.flush()
                    updateColor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    private fun updateColor(){
        runOnUiThread {
            if (changeColor)
                binding.mainLL.setBackgroundResource(R.color.dialog_primary)
            else
                binding.mainLL.setBackgroundResource(R.color.color1)
            changeColor = !changeColor
        }
    }
    private fun connectToMaster() {
        Thread {
            try {
                clientSocket = Socket(MASTER_IP, PORT)
                val input = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                isSlaveRunning = true

                runOnUiThread {  // <-- Add this block after successfully connecting
                    binding.feedbackText.text = "Connected to Master.\n"
                }

                var message: String?
                while (isSlaveRunning && clientSocket!!.isConnected) {
                    message = input.readLine()
                    println("messageeee $message")
                    if ("NEXT_IMAGE" == message) {
                        // Handle the image change on this device

                        // Also, update the UI to show a message
                        updateColor()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    updateUIForSlaveDisconnected()  // handle unexpected disconnections
                    binding.feedbackText.text = "Error occurred: ${e.message}"
                }
            }
        }.start()
    }


    private fun stopSlave() {
        isSlaveRunning = false
        clientSocket?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMaster()
        stopSlave()
    }
}
