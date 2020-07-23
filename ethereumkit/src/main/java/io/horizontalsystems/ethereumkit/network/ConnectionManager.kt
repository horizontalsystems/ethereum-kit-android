package io.horizontalsystems.ethereumkit.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.util.concurrent.Executors

class ConnectionManager(context: Context) {

    interface Listener {
        fun onConnectionChange(isConnected: Boolean)
    }

    private val executorService = Executors.newSingleThreadExecutor()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var listener: Listener? = null
    var isConnected = getInitialConnectionStatus()
    private var callback = ConnectionStatusCallback()

    init {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            //was not registered, or already unregistered
        }
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    private fun getInitialConnectionStatus(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    inner class ConnectionStatusCallback : ConnectivityManager.NetworkCallback() {

        private val activeNetworks: MutableList<Network> = mutableListOf()

        override fun onLost(network: Network) {
            super.onLost(network)
            activeNetworks.removeAll { activeNetwork -> activeNetwork == network }
            isConnected = activeNetworks.isNotEmpty()
            listener?.onConnectionChange(isConnected)
        }

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (activeNetworks.none { activeNetwork -> activeNetwork == network }) {
                activeNetworks.add(network)
            }
            isConnected = activeNetworks.isNotEmpty()
            listener?.onConnectionChange(isConnected)
        }
    }
}
