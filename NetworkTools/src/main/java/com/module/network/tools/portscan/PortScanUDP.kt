package com.module.network.tools.portscan

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException


object PortScanUDP {
    /**
     * Check if a port is open with UDP, note that this isn't reliable
     * as UDP will does not send ACKs
     *
     * @param ia            - address to scan
     * @param portNo        - port to scan
     * @param timeoutMillis - timeout
     * @return - true if port is open, false if not or unknown
     */
    fun scanAddress(ia: InetAddress?, portNo: Int, timeoutMillis: Int): Boolean {
        try {
            val bytes = ByteArray(128)
            val datagramPacket = DatagramPacket(bytes, bytes.size)

            val ds = DatagramSocket()
            ds.soTimeout = timeoutMillis
            ds.connect(ia, portNo)
            ds.send(datagramPacket)
            ds.isConnected
            ds.receive(datagramPacket)
            ds.close()
        } catch (socketTimeoutException: SocketTimeoutException) {
            Log.e("PortScanUDP", socketTimeoutException.message, socketTimeoutException)
            return true
        } catch (ignore: Exception) {
        }

        return false
    }
}