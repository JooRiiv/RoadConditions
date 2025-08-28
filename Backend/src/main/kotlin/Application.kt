package com.example

import com.example.routes.registerBumpsRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.calllogging.*
import org.bson.Document
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.ktor.server.routing.routing

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080

    val bumpsCollection: MongoCollection<Document> = try {
        val connectionString = System.getenv("MONGO_URI")
            ?: throw IllegalStateException("MONGO_URI environment variable is not set")

        val mongoClient = MongoClients.create(connectionString)
        val database: MongoDatabase = mongoClient.getDatabase("roadconditions")
        val collection = database.getCollection("bumps")

        println("Connected to MongoDB: ${collection.namespace.fullName}")
        collection
    } catch (e: Exception) {
        println("Failed to connect to MongoDB: ${e.message}")
        e.printStackTrace()
        return
    }

    try {
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) { json() }
            install(CallLogging)
            routing {
                registerBumpsRoutes(bumpsCollection)
            }
        }.start(wait = true)
    } catch (e: Exception) {
        println("Application failed to start: ${e.message}")
        e.printStackTrace()
    }
}