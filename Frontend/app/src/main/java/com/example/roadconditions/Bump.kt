package com.example.roadconditions
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class Bump(
    val latitude: Double,
    val longitude: Double,
    val signal: String,
    val timestamp: String = Clock.System.now().toString()
)