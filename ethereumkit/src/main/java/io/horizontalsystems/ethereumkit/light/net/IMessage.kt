package io.horizontalsystems.ethereumkit.light.net

interface IMessage {
    var code: Int
    fun encoded(): ByteArray
}

interface IP2PMessage : IMessage

interface ILESMessage : IMessage