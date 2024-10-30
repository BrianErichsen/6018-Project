package com.example.plugins

import com.example.DBSettings
import com.example.Models.Drawings
import com.example.Models.Users
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ImageUploadRequest(val userId: String, val imageUrl: String)

@Serializable
data class ImageResponse(val id: Int, val imageUrl: String, val shared: Boolean)
fun Application.configureRouting() {
    install(Resources)
    routing {
        //DBSettings.init()
        get("/") {
            call.respondText("Hello World!")
        }
        // Upload Image Endpoint
        post("/images/upload") {
            val request = call.receive<ImageUploadRequest>()
            println("Received upload request: $request")
            var imageId: Int? = null

            transaction {
                Users.insertIgnore {
                    it[id] = request.userId
                    it[email] = "example_user@gmail.com" // Replace with actual user email
                }

                imageId = Drawings.insertAndGetId {
                    it[userId] = request.userId
                    it[imageUrl] = request.imageUrl
                    it[shared] = false
                }.value
            }

            println("Image uploaded with ID: $imageId")
            call.respond(HttpStatusCode.Created, mapOf("message" to "Image uploaded", "imageId" to imageId))
        }


        // Fetch Shared Images Endpoint
        get("/images/shared") {
            val sharedImages = transaction {
                Drawings.select { Drawings.shared eq true }
                    .map { ImageResponse(it[Drawings.id].value, it[Drawings.imageUrl], it[Drawings.shared]) }
            }
            call.respond(sharedImages)
        }

        // Share Image Endpoint
        post("/images/{id}/share") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                transaction {
                    Drawings.update({ Drawings.id eq id }) {
                        it[shared] = true
                    }
                }
                call.respond(HttpStatusCode.OK, "Image with ID: $id shared successfully.")
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid image ID.")
            }
        }

        // Unshare Image Endpoint
        post("/images/{id}/unshare") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                transaction {
                    Drawings.update({ Drawings.id eq id }) {
                        it[shared] = false
                    }
                }
                call.respond(HttpStatusCode.OK, "Image with ID: $id unshared successfully.")
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid image ID.")
            }
        }

        // Delete Image Endpoint
        delete("/images/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()

            if (id != null) {
                transaction {
                    Drawings.deleteWhere { Drawings.id eq id }
                }
                call.respond(HttpStatusCode.OK, "Image with ID: $id deleted successfully.")
            } else {
                call.respond(HttpStatusCode.BadRequest, "Invalid image ID.")
            }
        }
    }
}
