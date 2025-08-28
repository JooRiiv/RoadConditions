package com.example.routes

import com.example.models.Bump
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.bson.Document
import com.mongodb.client.MongoCollection
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

fun Routing.registerBumpsRoutes(collection: MongoCollection<Document>) {

    get("/bumps") {
        println("Got all bumps")
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
            try {
                println("Received POST /bumps")
                val raw = call.receiveText()
                println("Raw body: $raw")

                val bump = try {
                    Json.decodeFromString<Bump>(raw)
                } catch (e: Exception) {
                    println("Deserialization failed: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid bump format"))
                    return@post
                }

                val document = Document().apply {
                    append("latitude", bump.latitude)
                    append("longitude", bump.longitude)
                    append("signal", bump.signal)
                    append("timestamp", bump.timestamp)
                }

                println("Prepared document: $document")

                try {
                    collection.insertOne(document)
                    println("Inserted bump into MongoDB")
                    call.respond(mapOf("status" to "success"))
                } catch (e: Exception) {
                    println("MongoDB insert failed: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Database error"))
                }

            } catch (e: Exception) {
                println("Unhandled error in /bumps route: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Unhandled server error"))
            }
        }

    }