package com.xmobile.project2digitalwellbeing.domain.network

interface NetworkStatusProvider {
    fun isNetworkAvailable(): Boolean
}
