package com.ren42377.bimalaunch.core

object NativeBridge {
    external fun nativeInitialize(configJson: String): String
    external fun nativeGetManifest(): String
    external fun nativeDispatch(action: String, payloadJson: String): String
    external fun nativeShutdown()

    fun initialize(configJson: String): String = nativeInitialize(configJson)
    fun getManifest(): String = nativeGetManifest()
    fun dispatch(action: String, payloadJson: String = "{}"): String = nativeDispatch(action, payloadJson)
    fun shutdown() = nativeShutdown()
}
