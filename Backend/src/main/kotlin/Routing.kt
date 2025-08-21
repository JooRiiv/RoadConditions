package com.example.routes

import com.example.models.Bump
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.bson.Document
import com.mongodb.client.MongoCollection

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

        post("/bumps") {
            val bump = call.receive<Bump>()
            collection.insertOne(
                Document().apply {
                    append("latitude", bump.latitude)
                    append("longitude", bump.longitude)
                    append("signal", bump.signal)
                    append("timestamp", java.time.Instant.now().toString())
                }
            )
            call.respond(mapOf("status" to "success"))
        }
    }
}