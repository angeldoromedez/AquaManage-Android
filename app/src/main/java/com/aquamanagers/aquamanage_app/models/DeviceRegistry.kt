package com.aquamanagers.aquamanage_app.models

data class DeviceRegistry(
    val connected: Boolean? = false,
    val customDevId: String? = "",
    val deviceName: String? = "Device 1",
    val deviceColor: String? = "#FFFFFF")

