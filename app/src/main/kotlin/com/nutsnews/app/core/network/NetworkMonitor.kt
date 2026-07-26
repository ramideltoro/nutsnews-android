package com.nutsnews.app.core.network

import kotlinx.coroutines.flow.StateFlow

enum class NetworkStatus {
    Available,
    Unavailable,
}

interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>
}
