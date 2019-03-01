package io.horizontalsystems.ethereumkit.spv.net

interface IMessage {
    var code: Int
    fun encoded(): ByteArray
}

interface IP2PMessage : IMessage

interface ILESMessage : IMessage