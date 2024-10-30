package com.example.drawing_app.network

import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ImageRepository {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    suspend fun uploadImage(imageData: ByteArray): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return if (uid != null) {
            val response: HttpResponse = client.post("http://10.0.2.2:8080/uploadImage") {
                header("Authorization", uid) // 将 UID 作为请求头发送
                setBody(imageData)
            }

            // 将响应处理为字符串，并检查状态码是否为 200
            val responseText = response.bodyAsText()
            println("Server response: $responseText")
            response.status == HttpStatusCode.Created
        } else {
            false
        }
    }

    suspend fun getUserImages(): List<String> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return if (uid != null) {
            client.get("http://10.0.2.2:8080/getImages") {
                header("Authorization", uid)
            }.body()
        } else {
            emptyList()
        }
    }
}
