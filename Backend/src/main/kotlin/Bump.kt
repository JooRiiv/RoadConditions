package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Bump(
    val latitude: Double,
    val longitude: Double,
    val signal: String,
    val timestamp: String
)