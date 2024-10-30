package com.example.drawing_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.drawing_app.network.ApiRepository
import com.example.drawing_app.network.ApiService
import com.example.drawing_app.network.ApiViewModel
import com.example.drawing_app.network.ApiViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private val drawingViewModel: DrawingViewModel by viewModels {
        DrawingViewModelFactory((application as DrawingApplication).repository)
    }
    private val apiViewModel: ApiViewModel by viewModels {
        ApiViewModelFactory(ApiRepository(CoroutineScope(Dispatchers.IO), ApiService()))
    }
    private lateinit var shakeListener: ShakeListener
    private var onShakeCallback: (() -> Unit)? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        shakeListener = ShakeListener(this) {
            onShakeDetected()
        }
        setContent {
            val navController = rememberNavController()
            Surface {
                NavGraph(navController = navController, viewModel = drawingViewModel, apiViewModel = apiViewModel,
                    onShakeCallback = { callback -> onShakeCallback = callback },
                    onSensorEnabledChanged = { enabled -> toggleShakeListener(enabled) }
                )
            }
        }

        // 示例：上传图片
        val imageData = ByteArray(0) // 您的图片数据
        uploadImage(imageData)

        // 示例：获取用户的共享图片列表
        fetchUserImages()
    }

    private fun uploadImage(imageData: ByteArray) {
        apiViewModel.uploadImage(
            imageData,
            onSuccess = {
                // 上传成功后的处理逻辑
                println("Image uploaded successfully")
            },
            onFailure = {
                // 上传失败后的处理逻辑
                println("Image upload failed")
            }
        )
    }

    private fun fetchUserImages() {
        apiViewModel.fetchUserImages(
            onSuccess = { images ->
                // 成功获取用户图片后的处理逻辑
                images.forEach { println("User image: $it") }
            },
            onFailure = {
                // 获取图片失败后的处理逻辑
                println("Failed to fetch user images")
            }
        )
    }

    override fun onResume() {
        super.onResume()
        shakeListener.start()
    }

    override fun onPause() {
        super.onPause()
        shakeListener.stop()
    }

    private fun onShakeDetected() {
        onShakeCallback?.invoke()
    }

    private fun toggleShakeListener(sensorEnabled: Boolean) {
        if (sensorEnabled) {
            shakeListener.stop()
        } else {
            shakeListener.start()
        }
    }
}
