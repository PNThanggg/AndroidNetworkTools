package com.module.network.tools.subnet

import java.net.InetAddress


class Device(ip: InetAddress) {
    var hostAddress: String? = ip.hostAddress
    var hostname: String = ip.canonicalHostName
    var mac: String? = null
    var time: Float = 0f

    override fun toString(): String {
        return "Device{" +
                "hostAddress='" + hostAddress + '\'' +
                ", hostname='" + hostname + '\'' +
                ", mac='" + mac + '\'' +
                ", time=" + time +
                '}'
    }
}
