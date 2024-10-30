package com.example.drawing_app.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

import kotlinx.serialization.json.Json

@Serializable
data class DrawingResponse(
    val id: Int,
    val userId: String,
    val imageUrl: String,
    val shared: Boolean
)

@Serializable
data class ImageUploadRequest(val userId: String, val imageUrl: String)

class ApiService {
    private val URL_BASE = "http://10.0.2.2:8080"

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Resources)
    }
    // 修改后的 uploadImage 方法，返回 Boolean 表示上传成功与否
    suspend fun uploadImage(request: ImageUploadRequest): Boolean {
        val response: HttpResponse = httpClient.post("http://10.0.2.2:8080/images/upload") { // 确保路径正确
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        // 打印响应文本以调试
        val responseText = response.bodyAsText()
        println("Server response: $responseText")

        // 检查状态码，如果是 201 Created 则表示成功
        return response.status == HttpStatusCode.Created
    }


    suspend fun fetchSharedImages(): List<DrawingResponse> {
        return httpClient.get("$URL_BASE/images/shared").body()
    }

    suspend fun shareImage(imageId: Int) {
        httpClient.post("$URL_BASE/images/$imageId/share").body<Unit>()
    }

    suspend fun unshareImage(imageId: Int) {
        httpClient.post("$URL_BASE/images/$imageId/unshare").body<Unit>()
    }

    suspend fun deleteImage(imageId: Int) {
        httpClient.delete("$URL_BASE/images/$imageId").body<Unit>()
    }
}
