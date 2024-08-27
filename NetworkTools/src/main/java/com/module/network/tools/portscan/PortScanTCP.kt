package com.module.network.tools.portscan

import android.util.Log
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


object PortScanTCP {
    /**
     * Check if a port is open with TCP
     *
     * @param ia            - address to scan
     * @param portNo        - port to scan
     * @param timeoutMillis - timeout
     * @return - true if port is open, false if not or unknown
     */
    fun scanAddress(ia: InetAddress?, portNo: Int, timeoutMillis: Int): Boolean {
        val socket = Socket()
        try {
            socket.connect(InetSocketAddress(ia, portNo), timeoutMillis)
            return true
        } catch (ioException: IOException) {
            Log.e("PortScanTCP", ioException.message, ioException)
        } finally {
            try {
                socket.close()
            } catch (ioException: IOException) {
                Log.e("PortScanTCP", ioException.message, ioException)
            }
        }
        return false
    }
}