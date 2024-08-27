package com.module.network.tools.ping

import java.net.InetAddress


class PingResult(val address: InetAddress?) {
    var isReachable: Boolean = false
    var error: String? = null
    var timeTaken: Float = 0f
    var fullString: String? = null
    var result: String? = null

    fun hasError(): Boolean {
        return error != null
    }

    override fun toString(): String {
        return "PingResult{" +
                "ia=" + address +
                ", isReachable=" + isReachable +
                ", error='" + error + '\'' +
                ", timeTaken=" + timeTaken +
                ", fullString='" + fullString + '\'' +
                ", result='" + result + '\'' +
                '}'
    }
}