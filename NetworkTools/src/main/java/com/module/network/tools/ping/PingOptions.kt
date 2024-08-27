package com.module.network.tools.ping

import kotlin.math.max

class PingOptions {
    private var timeoutMillis = 1000
    private var timeToLive = 128

    fun getTimeoutMillis(): Int {
        return timeoutMillis
    }

    fun setTimeoutMillis(timeoutMillis: Int) {
        this.timeoutMillis = max(timeoutMillis.toDouble(), 1000.0).toInt()
    }

    fun getTimeToLive(): Int {
        return timeToLive
    }

    fun setTimeToLive(timeToLive: Int) {
        this.timeToLive = max(timeToLive.toDouble(), 1.0).toInt()
    }
}