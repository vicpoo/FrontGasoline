package org.vicpoo.gasolineproject.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Escucha la red por defecto del teléfono (requiere ACCESS_NETWORK_STATE, ya está en el manifest).
 * Se usa solo NET_CAPABILITY_INTERNET (no VALIDATED): algunas redes de empresa bloquean la
 * verificación de Google y marcarían "sin red" aunque la API sí responda.
 */
class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(currentlyOnline())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        manager.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }

                override fun onUnavailable() {
                    _isOnline.value = false
                }
            },
        )
    }

    private fun currentlyOnline(): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
