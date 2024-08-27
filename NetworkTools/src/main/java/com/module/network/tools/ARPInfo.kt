package com.module.network.tools

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

/**
 * Looks at the file at /proc/net/arp to fromIPAddress ip/mac addresses from the cache
 * We assume that the file has this structure:
 *
 * IP address       HW type     Flags       HW address            Mask     Device
 * 192.168.18.11    0x1         0x2         00:04:20:06:55:1a     *        eth0
 * 192.168.18.36    0x1         0x2         00:22:43:ab:2a:5b     *        eth0
 *
 * Also looks at the output from `ip sleigh show` command
 *
 */
object ARPInfo {
    /**
     * Try to extract a hardware MAC address from a given IP address
     *
     * @param ip - IP address to search for
     * @return the MAC from the ARP cache or null in format "01:23:45:67:89:ab"
     */
    fun getMACFromIPAddress(ip: String?): String? {
        if (ip == null) {
            return null
        }

        val cache = allIPAndMACAddressesInARPCache
        return cache[ip]
    }


    /**
     * Try to extract a IP address from the given MAC address
     *
     * @param macAddress in format "01:23:45:67:89:ab" to search for
     * @return the IP address found or null in format "192.168.0.1"
     */
    fun getIPAddressFromMAC(macAddress: String?): String? {
        if (macAddress == null) {
            return null
        }

        require(macAddress.matches("..:..:..:..:..:..".toRegex())) { "Invalid MAC Address" }

        val cache = allIPAndMACAddressesInARPCache
        for (ip in cache.keys) {
            if (cache[ip].equals(macAddress, ignoreCase = true)) {
                return ip
            }
        }
        return null
    }


    val allIPAddressesInARPCache: ArrayList<String>
        /**
         * Returns all the ip addresses currently in the ARP cache (/proc/net/arp).
         *
         * @return list of IP addresses found
         */
        get() = ArrayList(allIPAndMACAddressesInARPCache.keys)

    val allMACAddressesInARPCache: ArrayList<String>
        /**
         * Returns all the MAC addresses currently in the ARP cache (/proc/net/arp).
         *
         * @return list of MAC addresses found
         */
        get() = ArrayList(allIPAndMACAddressesInARPCache.values)


    val allIPAndMACAddressesInARPCache: HashMap<String, String>
        /**
         * Returns all the IP/MAC address pairs currently in the following places
         *
         * 1. ARP cache (/proc/net/arp).
         * 2. `ip neigh show` command
         *
         * @return list of IP/MAC address pairs found
         */
        get() {
            val macList = allIpAndMACAddressesFromIPSleigh
            for (line in linesInARPCache) {
                val splitted = line.split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitted.size >= 4) {
                    // Ignore values with invalid MAC addresses
                    if (splitted[3].matches("..:..:..:..:..:..".toRegex()) && splitted[3] != "00:00:00:00:00:00") {
                        if (!macList.containsKey(splitted[0])) {
                            macList[splitted[0]] = splitted[3]
                        }
                    }
                }
            }
            return macList
        }

    private val linesInARPCache: ArrayList<String>
        /**
         * Method to read lines from the ARP Cache
         *
         * @return the lines of the ARP Cache.
         */
        get() {
            val lines = ArrayList<String>()

            // If we cant read the file just return empty list
            if (!File("/proc/net/arp").canRead()) {
                return lines
            }

            var br: BufferedReader? = null
            try {
                br = BufferedReader(FileReader("/proc/net/arp"))
                var line: String
                while ((br.readLine().also { line = it }) != null) {
                    lines.add(line)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    br?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return lines
        }


    val allIpAndMACAddressesFromIPSleigh: HashMap<String, String>
        /**
         * Get the IP / MAC address pairs from `ip sleigh show` command
         *
         * @return hashmap of ips and mac addresses
         */
        get() {
            val macList = HashMap<String, String>()

            try {
                val runtime = Runtime.getRuntime()
                val proc = runtime.exec("ip neigh show")
                proc.waitFor()
                val exit = proc.exitValue()

                if (exit != 0) {
                    return macList
                }

                proc.inputStream.bufferedReader().use { buffer ->
                    buffer.forEachLine { line ->
                        val splits = line.split("\\s+".toRegex())
                        if (splits.size >= 4) {
                            macList[splits[0]] = splits[4]
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return macList
        }
}