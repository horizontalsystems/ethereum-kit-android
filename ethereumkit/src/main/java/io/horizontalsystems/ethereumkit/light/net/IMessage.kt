package io.horizontalsystems.ethereumkit.light.net

interface IMessage {
    var code: Int
    fun encoded(): ByteArray
}