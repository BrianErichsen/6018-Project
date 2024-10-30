package com.example.drawing_app.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ApiViewModel(
    private val repository: ApiRepository,
    private val imageRepository: ImageRepository = ImageRepository() // 添加 ImageRepository 实例
) : ViewModel() {

    val sharedImages: LiveData<List<DrawingResponse>> = repository.sharedImages

    // 从 Ktor 服务器获取共享图片
    fun fetchSharedImages() = repository.fetchSharedImages()

    // 上传图片到 Ktor 服务器
    fun uploadImage(imageData: ByteArray, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = imageRepository.uploadImage(imageData)
            if (success) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    // 获取用户的共享图片列表
    fun fetchUserImages(onSuccess: (List<String>) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            try {
                val images = imageRepository.getUserImages()
                onSuccess(images)
            } catch (e: Exception) {
                onFailure()
            }
        }
    }

    // 其他 API 方法
    fun shareImage(imageId: Int) = repository.shareImage(imageId)
    fun unshareImage(imageId: Int) = repository.unshareImage(imageId)
    fun deleteImage(imageId: Int) = repository.deleteImage(imageId)
}