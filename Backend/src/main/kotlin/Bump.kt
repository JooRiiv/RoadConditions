package com.example.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

@Serializable
data class Bump(
    val latitude: Double,
    val longitude: Double,
    val signal: String,
    val timestamp: String = Clock.System.now().toString()
)