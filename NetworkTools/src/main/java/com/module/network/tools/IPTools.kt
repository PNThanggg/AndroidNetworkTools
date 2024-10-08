package com.module.network.tools

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Enumeration
import java.util.regex.Pattern


object IPTools {
    /**
     * Ip matching patterns from
     * https://examples.javacodegeeks.com/core-java/util/regex/regular-expressions-for-ip-v4-and-ip-v6-addresses/
     * note that these patterns will match most but not all valid ips
     */
    private val IPV4_PATTERN: Pattern = Pattern.compile(
        "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$"
    )

    private val IPV6_STD_PATTERN: Pattern = Pattern.compile(
        "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    )

    private val IPV6_HEX_COMPRESSED_PATTERN: Pattern = Pattern.compile(
        "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$"
    )

    fun isIPv4Address(address: String?): Boolean {
        return address != null && IPV4_PATTERN.matcher(address).matches()
    }

    fun isIPv6StdAddress(address: String?): Boolean {
        return address != null && IPV6_STD_PATTERN.matcher(address).matches()
    }

    fun isIPv6HexCompressedAddress(address: String?): Boolean {
        return address != null && IPV6_HEX_COMPRESSED_PATTERN.matcher(address).matches()
    }

    fun isIPv6Address(address: String?): Boolean {
        return address != null && (isIPv6StdAddress(address) || isIPv6HexCompressedAddress(address))
    }

    val localIPv4Address: InetAddress?
        /**
         * @return The first local IPv4 address, or null
         */
        get() {
            val localAddresses = localIPv4Addresses
            return if (localAddresses.size > 0) localAddresses[0] else null
        }

    private val localIPv4Addresses: ArrayList<InetAddress>
        /**
         * @return The list of all IPv4 addresses found
         */
        get() {
            val foundAddresses = ArrayList<InetAddress>()

            val ifaces: Enumeration<NetworkInterface>
            try {
                ifaces = NetworkInterface.getNetworkInterfaces()

                while (ifaces.hasMoreElements()) {
                    val iface = ifaces.nextElement()
                    val addresses = iface.inetAddresses

                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress()) {
                            foundAddresses.add(addr)
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return foundAddresses
        }


    /**
     * Check if the provided ip address refers to the localhost
     *
     * https://stackoverflow.com/a/2406819/315998
     *
     * @param addr - address to check
     * @return - true if ip address is self
     */
    fun isIpAddressLocalhost(addr: InetAddress?): Boolean {
        if (addr == null) return false

        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress || addr.isLoopbackAddress) return true

        // Check if the address is defined on any interface
        return try {
            NetworkInterface.getByInetAddress(addr) != null
        } catch (e: SocketException) {
            false
        }
    }

    /**
     * Check if the provided ip address refers to the localhost
     *
     * https://stackoverflow.com/a/2406819/315998
     *
     * @param address - address to check
     * @return - true if ip address is self
     */
    fun isIpAddressLocalNetwork(address: InetAddress?): Boolean {
        return address != null && address.isSiteLocalAddress
    }
}