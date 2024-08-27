package com.module.network.tools.ping

import com.module.network.tools.IPTools.isIPv4Address
import com.module.network.tools.IPTools.isIPv6Address
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress


object PingNative {
    @Throws(IOException::class, InterruptedException::class)
    fun ping(host: InetAddress?, pingOptions: PingOptions?): PingResult {
        val pingResult = PingResult(host)

        if (host == null || pingOptions == null) {
            pingResult.isReachable = false
            return pingResult
        }

        val echo = StringBuilder()
        val runtime = Runtime.getRuntime()

        val timeoutSeconds = (pingOptions.getTimeoutMillis() / 1000).coerceAtLeast(1)
        val ttl = pingOptions.getTimeToLive().coerceAtLeast(1)

        var address = host.hostAddress
        var pingCommand = "ping"

        if (address != null) {
            if (isIPv6Address(address)) {
                // If we detect this is a ipv6 address, change the to the ping6 binary
                pingCommand = "ping6"
            } else if (!isIPv4Address(address)) {
                // Address doesn't look to be ipv4 or ipv6, but we could be mistaken
            }
        } else {
            // Not sure if getHostAddress ever returns null, but if it does, use the hostname as a fallback
            address = host.hostName
        }

        val proc = runtime.exec("$pingCommand -c 1 -W $timeoutSeconds -t $ttl $address")
        proc.waitFor()
        val exit = proc.exitValue()
        val pingError: String
        when (exit) {
            0 -> {
                val reader = InputStreamReader(proc.inputStream)
                val buffer = BufferedReader(reader)
                var line: String?
                while ((buffer.readLine().also { line = it }) != null) {
                    echo.append(line).append("\n")
                }
                return getPingStats(pingResult, echo.toString())
            }

            1 -> pingError = "failed, exit = 1"
            else -> pingError = "error, exit = 2"
        }
        pingResult.error = pingError
        proc.destroy()
        return pingResult
    }

    /**
     * getPingStats interprets the text result of a Linux activity_ping command
     *
     * Set pingError on error and return null
     *
     * http://en.wikipedia.org/wiki/Ping
     *
     * PING 127.0.0.1 (127.0.0.1) 56(84) bytes of data.
     * 64 bytes from 127.0.0.1: icmp_seq=1 ttl=64 time=0.251 ms
     * 64 bytes from 127.0.0.1: icmp_seq=2 ttl=64 time=0.294 ms
     * 64 bytes from 127.0.0.1: icmp_seq=3 ttl=64 time=0.295 ms
     * 64 bytes from 127.0.0.1: icmp_seq=4 ttl=64 time=0.300 ms
     *
     * --- 127.0.0.1 activity_ping statistics ---
     * 4 packets transmitted, 4 received, 0% packet loss, time 0ms
     * rtt min/avg/max/mdev = 0.251/0.285/0.300/0.019 ms
     *
     * PING 192.168.0.2 (192.168.0.2) 56(84) bytes of data.
     *
     * --- 192.168.0.2 activity_ping statistics ---
     * 1 packets transmitted, 0 received, 100% packet loss, time 0ms
     *
     * # activity_ping 321321.
     * activity_ping: unknown host 321321.
     *
     * 1. Check if output contains 0% packet loss : Branch to success - Get stats
     * 2. Check if output contains 100% packet loss : Branch to fail - No stats
     * 3. Check if output contains 25% packet loss : Branch to partial success - Get stats
     * 4. Check if output contains "unknown host"
     *
     * @param pingResult - the current ping result
     * @param s - result from ping command
     *
     * @return The ping result
     */
    private fun getPingStats(pingResult: PingResult, str: String): PingResult {
        var s = str
        val pingError: String
        if (s.contains("0% packet loss")) {
            val start = s.indexOf("/mdev = ")
            val end = s.indexOf(" ms\n", start)
            pingResult.fullString = s
            if (start == -1 || end == -1) {
                pingError = "Error: $s"
            } else {
                s = s.substring(start + 8, end)
                val stats = s.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                pingResult.isReachable = true
                pingResult.result = s
                pingResult.timeTaken = stats[1].toFloat()
                return pingResult
            }
        } else if (s.contains("100% packet loss")) {
            pingError = "100% packet loss"
        } else if (s.contains("% packet loss")) {
            pingError = "partial packet loss"
        } else if (s.contains("unknown host")) {
            pingError = "unknown host"
        } else {
            pingError = "unknown error in getPingStats"
        }
        pingResult.error = pingError
        return pingResult
    }
}