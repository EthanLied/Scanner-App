package com.example.data.model

data class PrinterDevice(
    val model: String,
    val ip: String,
    val port: Int = 80,
    val discoveryMethod: String = "mDNS",
    val isOnline: Boolean = true,
    val details: String = ""
)
