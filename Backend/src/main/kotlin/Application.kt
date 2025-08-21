package com.example

import com.example.routes.registerBumpsRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import org.bson.Document
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase

fun main() {
    try {
        val connectionString = System.getenv("MONGO_URI")
            ?: throw IllegalStateException("MONGO_URI environment variable is not set")
        val mongoClient = MongoClients.create(connectionString)
        val database: MongoDatabase = mongoClient.getDatabase("roadconditions")
        val bumpsCollection: MongoCollection<Document> = database.getCollection("bumps")
        val port = System.getenv("PORT")?.toInt() ?: 8080

        println("Connected to MongoDB: ${bumpsCollection.namespace.fullName}")

        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json()
            }
            registerBumpsRoutes(bumpsCollection)
        }.start(wait = true)
    } catch (e: Exception) {
        println("Application failed to start: ${e.message}")
        e.printStackTrace()
    }
}

