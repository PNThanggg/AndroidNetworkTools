package com.module.network.tools.ping

import java.net.InetAddress


class PingStats(
    private val address: InetAddress?,
    private val noPings: Long,
    private val packetsLost: Long,
    totalTimeTaken: Float,
    private val minTimeTaken: Float,
    private val maxTimeTaken: Float,
) {
    private val averageTimeTaken: Float = totalTimeTaken / noPings
    val isReachable: Boolean = noPings - packetsLost > 0

    fun getNoPings(): Long {
        return noPings
    }

    fun getPacketsLost(): Long {
        return packetsLost
    }

    val averageTimeTakenMillis: Long
        get() = (averageTimeTaken * 1000).toLong()

    val minTimeTakenMillis: Long
        get() = (minTimeTaken * 1000).toLong()

    val maxTimeTakenMillis: Long
        get() = (maxTimeTaken * 1000).toLong()

    override fun toString(): String {
        return "PingStats{ia=$address, noPings=$noPings, packetsLost=$packetsLost, averageTimeTaken=$averageTimeTaken, minTimeTaken=$minTimeTaken, maxTimeTaken=$maxTimeTaken}"
    }
}