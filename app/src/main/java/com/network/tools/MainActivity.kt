package com.network.tools

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.module.network.tools.ARPInfo.getIPAddressFromMAC
import com.module.network.tools.ARPInfo.getMACFromIPAddress
import com.module.network.tools.IPTools
import com.module.network.tools.Ping
import com.module.network.tools.Ping.PingListener
import com.module.network.tools.PortScan
import com.module.network.tools.PortScan.PortListener
import com.module.network.tools.SubnetDevices
import com.module.network.tools.SubnetDevices.OnSubnetDeviceFound
import com.module.network.tools.WakeOnLan
import com.module.network.tools.ping.PingResult
import com.module.network.tools.ping.PingStats
import com.module.network.tools.subnet.Device
import com.network.tools.databinding.ActivityMainBinding
import java.io.IOException
import java.net.UnknownHostException
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ipAddress = IPTools.localIPv4Address
        if (ipAddress != null) {
            binding.editIpAddress.setText(ipAddress.hostAddress)
        }

        binding.pingButton.setOnClickListener {
            Thread {
                try {
                    doPing()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        binding.wolButton.setOnClickListener {
            Thread {
                try {
                    doWakeOnLan()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        binding.portScanButton.setOnClickListener {
            Thread {
                try {
                    doPortScan()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        binding.subnetDevicesButton.setOnClickListener {
            Thread {
                try {
                    findSubnetDevices()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun appendResultsText(text: String?) {
        runOnUiThread {
            binding.resultText.append(text + "\n")
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun setEnabled(view: View?, enabled: Boolean) {
        runOnUiThread {
            if (view != null) {
                view.isEnabled = enabled
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun doPing() {
        val ipAddress: String = binding.editIpAddress.getText().toString()

        if (TextUtils.isEmpty(ipAddress)) {
            appendResultsText("Invalid Ip Address")
            return
        }

        setEnabled(binding.pingButton, false)

        // Perform a single synchronous ping
        val pingResult: PingResult?
        try {
            pingResult = Ping.onAddress(ipAddress).setTimeOutMillis(1000).doPing()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            appendResultsText(e.message)
            setEnabled(binding.pingButton, true)
            return
        }


        appendResultsText("Pinging Address: " + pingResult.address?.hostAddress)
        appendResultsText("HostName: " + pingResult.address?.getHostName())
        appendResultsText(String.format(Locale.US, "%.2f ms", pingResult.timeTaken))


        // Perform an asynchronous ping
        Ping.onAddress(ipAddress).setTimeOutMillis(1000).setTimes(5).doPing(object : PingListener {
            override fun onResult(pingResult: PingResult?) {
                if (pingResult!!.isReachable) {
                    appendResultsText(String.format(Locale.US, "%.2f ms", pingResult.timeTaken))
                } else {
                    appendResultsText(getString(R.string.timeout))
                }
            }

            override fun onFinished(pingStats: PingStats?) {
                appendResultsText(
                    String.format(
                        Locale.US, "Pings: %d, Packets lost: %d", pingStats?.getNoPings(), pingStats?.getPacketsLost()
                    )
                )
                appendResultsText(
                    java.lang.String.format(
                        Locale.US,
                        "Min, Avg, Max Time: %.2f, %.2f, %.2f ms",
                        pingStats?.minTimeTakenMillis?.toFloat() ?: -1f,
                        pingStats?.averageTimeTakenMillis?.toFloat() ?: -1f,
                        pingStats?.maxTimeTakenMillis?.toFloat() ?: -1f
                    )

                )
                setEnabled(binding.pingButton, true)
            }

            override fun onError(e: java.lang.Exception?) {
                setEnabled(binding.pingButton, true)
            }
        })
    }

    @Throws(IllegalArgumentException::class)
    private fun doWakeOnLan() {
        val ipAddress: String = binding.editIpAddress.getText().toString()

        if (TextUtils.isEmpty(ipAddress)) {
            appendResultsText("Invalid Ip Address")
            return
        }

        setEnabled(binding.wolButton, false)

        appendResultsText("IP address: $ipAddress")

        // Get mac address from IP (using arp cache)
        val macAddress = getMACFromIPAddress(ipAddress)

        if (macAddress == null) {
            appendResultsText("Could not fromIPAddress MAC address, cannot send WOL packet without it.")
            setEnabled(binding.wolButton, true)
            return
        }

        appendResultsText("MAC address: $macAddress")
        appendResultsText("IP address2: " + getIPAddressFromMAC(macAddress))

        // Send Wake on lan packed to ip/mac
        try {
            WakeOnLan.sendWakeOnLan(ipAddress, macAddress)
            appendResultsText("WOL Packet sent")
        } catch (e: IOException) {
            appendResultsText(e.message)
            e.printStackTrace()
        } finally {
            setEnabled(binding.wolButton, true)
        }
    }

    @Throws(java.lang.Exception::class)
    private fun doPortScan() {
        val ipAddress: String = binding.editIpAddress.getText().toString()

        if (TextUtils.isEmpty(ipAddress)) {
            appendResultsText("Invalid Ip Address")
            setEnabled(binding.portScanButton, true)
            return
        }

        setEnabled(binding.portScanButton, false)

        // Perform synchronous port scan
        appendResultsText("PortScanning IP: $ipAddress")
        PortScan.onAddress(ipAddress).setPort(21).setMethodTCP().doScan()

        val startTimeMillis = System.currentTimeMillis()

        // Perform an asynchronous port scan
        PortScan.onAddress(ipAddress).setPortsAll().setMethodTCP().doScan(object : PortListener {
            override fun onResult(portNo: Int, open: Boolean) {
                if (open) appendResultsText("Open: $portNo")
            }

            override fun onFinished(openPorts: ArrayList<Int>?) {
                appendResultsText("Open Ports: " + openPorts!!.size)
                appendResultsText("Time Taken: " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0f))
                setEnabled(binding.portScanButton, true)
            }
        })

        // Below is example of how to cancel a running scan
        // portScan.cancel();
    }


    private fun findSubnetDevices() {
        setEnabled(binding.subnetDevicesButton, false)

        val startTimeMillis = System.currentTimeMillis()

        SubnetDevices.fromLocalAddress().findDevices(object : OnSubnetDeviceFound {
            override fun onDeviceFound(device: Device) {
                appendResultsText(("Device: " + device.hostAddress) + " " + device.hostname)
            }

            override fun onFinished(devicesFound: ArrayList<Device>) {
                val timeTaken = (System.currentTimeMillis() - startTimeMillis) / 1000.0f
                appendResultsText("Devices Found: " + devicesFound.size)
                appendResultsText("Finished $timeTaken s")
                setEnabled(binding.subnetDevicesButton, true)
            }
        })

        // Below is example of how to cancel a running scan
        // subnetDevices.cancel();
    }
}