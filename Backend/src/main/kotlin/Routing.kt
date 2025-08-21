package com.example.routes

import com.example.models.Bump
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.bson.Document
import com.mongodb.client.MongoCollection
import io.ktor.http.HttpStatusCode

fun Application.registerBumpsRoutes(collection: MongoCollection<Document>) {
    routing {
        // GET all bumps
        get("/bumps") {
            val bumps = collection.find().map { doc ->
                Bump(
                    latitude = doc.getDouble("latitude"),
                    longitude = doc.getDouble("longitude"),
                    signal = doc.getString("signal"),
                    timestamp = doc.getString("timestamp") ?: java.time.Instant.now().toString()
                )
            }.toList()
            call.respond(bumps)
        }

        get("/") {
            call.respondText("Hello from RoadConditions API")
        }

        post("/bumps") {
            try {
                println("Attempting to receive bump...")
                val bump = call.receive<Bump>()
                println("Received bump: $bump")
                collection.insertOne(
                    Document().apply {
                        append("latitude", bump.latitude)
                        append("longitude", bump.longitude)
                        append("signal", bump.signal)
                        append("timestamp", bump.timestamp)
                    }
                )
                println("Inserted bump into MongoDB")
                call.respond(mapOf("status" to "success"))
            } catch (e: Exception) {
                println("Error inserting bump: ${e.message}")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to save bump")
                )
            }
        }
    }
}