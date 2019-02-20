package io.horizontalsystems.ethereumkit.light.net.messages

interface IMessage {
    var code: Int
    fun encoded(): ByteArray
}