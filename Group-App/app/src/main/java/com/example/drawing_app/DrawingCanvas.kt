package com.example.drawing_app

import android.graphics.Bitmap
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import android.widget.Toast
import androidx.compose.foundation.Canvas
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.graphics.Color
import com.example.drawing_app.network.ApiViewModel
import kotlinx.coroutines.withContext

data class Point(val x: Float, val y: Float, val color: Color, val size: Float)

@Composable
fun DrawingCanvas(navController: NavController, drawingId: Int?, viewModel: DrawingViewModel,
                  onShakeCallback: (()-> Unit)-> Unit, onSensorEnabledChanged: (Boolean) -> Unit,
                  apiViewModel: ApiViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var currentPath = remember { mutableStateListOf<Point>() }
    var savedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) } // For displaying the loaded image
    var filePath by remember { mutableStateOf<String?>(null) }
    val creatingNewDrawing = drawingId == -1
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    // 观察 sensorPath 的变化
    val sensorPath by viewModel.sensorPath.observeAsState(emptyList())

    // 小球的当前位置
    var currentX by remember { mutableStateOf(0f) }
    var currentY by remember { mutableStateOf(0f) }

    // 控制是否启用重力传感器路径绘制
    var sensorEnabled by remember { mutableStateOf(false) }
    // 添加颜色选择的状态
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var showGravityColorOptions by remember { mutableStateOf(false) }

    fun toggleGravityMode() {
        sensorEnabled = !sensorEnabled
        showGravityColorOptions = sensorEnabled
    }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (sensorEnabled) {
                    val gravityData = event.values

                    // 获取当前的 Canvas 宽度和高度
                    val canvasWidth = context.resources.displayMetrics.widthPixels.toFloat()
                    val canvasHeight = context.resources.displayMetrics.heightPixels.toFloat()

                    // 更新小球的 X 和 Y 坐标，确保不超出画布范围
                    currentX = (currentX + gravityData[0] * 5).coerceIn(0f, canvasWidth - 20f)
                    currentY = (currentY + gravityData[1] * 5).coerceIn(0f, canvasHeight - 20f)

                    // 更新路径
                    viewModel.updatePathFromSensorData(gravityData, currentX, currentY, canvasWidth, canvasHeight)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

// 在传感器启用时，将 currentX 和 currentY 初始化为画布的中心位置
    LaunchedEffect(sensorEnabled) {
        if (sensorEnabled) {
            sensorManager.registerListener(sensorEventListener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)

            // 获取当前画布的宽高，设置初始位置为中心
            val canvasWidth = context.resources.displayMetrics.widthPixels.toFloat()
            val canvasHeight = context.resources.displayMetrics.heightPixels.toFloat()

            // 初始化 currentX 和 currentY 只在启用传感器时设置一次
            currentX = canvasWidth / 2f
            currentY = canvasHeight / 2f
        } else {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 使用 Spacer 将内容推到页面底部
        Spacer(modifier = Modifier.weight(1f))
        // 颜色选择按钮放在底部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // change it
        }

        // 启用或禁用传感器绘图的按钮放在颜色选择按钮下方
        Button(
            onClick = {
                toggleGravityMode()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)  // 添加底部间距
        ) {
            Text(if (sensorEnabled) "Disable Gravity Drawing" else "Enable Gravity Drawing")
        }

        if (showGravityColorOptions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Choose Color for Gravity Drawing:")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { selectedColor = Color.Red }) {
                        Text("Red")
                    }
                    Button(onClick = { selectedColor = Color.Blue }) {
                        Text("Blue")
                    }
                    Button(onClick = { selectedColor = Color.Green }) {
                        Text("Green")
                    }
                }
            }
        }
    }

    // pencil tool state data
    val pencil = remember { Pencil() }
    var showPencilOptions by remember { mutableStateOf(false) }

    fun increasePenSize() {
        var currentPenSize = pencil.size.value
        currentPenSize += 5f
        pencil.size.value = currentPenSize
        Toast.makeText(context, "Pen size increased to $currentPenSize", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(sensorEnabled) {
        onSensorEnabledChanged(sensorEnabled)
    }

    LaunchedEffect(Unit) {
        onShakeCallback { increasePenSize() }
    }

    // Load existing drawing if `drawingId` is provided
    LaunchedEffect(drawingId) {
        if (drawingId != null && drawingId != -1) {
            scope.launch(Dispatchers.IO) {
                try {
                    val drawing = viewModel.getDrawingById(drawingId)
                    drawing?.let {
                        name = it.name
                        filePath = it.filePath
                        val file = File(it.filePath)
                        if (file.exists()) {
                            val loadedBitmap = BitmapFactory.decodeFile(file.path)
                            savedImageBitmap = loadedBitmap.asImageBitmap() // Convert to ImageBitmap for display
                        }
                    }
                } catch (e: Exception) {
                    println("Error loading image: ${e.message}")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showPencilOptions = !showPencilOptions },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toggle Pencil Options")
        }
        Button(
            onClick = { if(filePath != null) {
                shareDrawing(context, filePath!!)
            } else {
                Toast.makeText(context, "No drawing available to share", Toast.LENGTH_SHORT).show()
            }
            }) {
            Text("Share Drawing")
        }

        if (showPencilOptions) {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row {
                    Button(
                        onClick = { pencil.changePencilColor(Color.Black) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        modifier = Modifier.size(40.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { pencil.changePencilColor(Color.Red) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.size(40.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { pencil.changePencilColor(Color.Blue) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                        modifier = Modifier.size(40.dp)
                    ) {}
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Size Slider
                Text("Pencil Size: ${pencil.size.value.toInt()}")
                Slider(
                    value = pencil.size.value,
                    onValueChange = { pencil.changePencilSize(it) },
                    valueRange = 5f..50f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Eraser Toggle
                Button(
                    onClick = { pencil.toggleEraser() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (pencil.isErasing.value) "Stop Erasing" else "Eraser")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } // end of if show pen options UI elements
        // Upload to Server Button
        Button(
            onClick = {
                filePath?.let { path ->
                    scope.launch {
                        val file = File(path) // 使用 filePath 生成 File 对象
                        if (file.exists()) {
                            val imageData = file.readBytes() // 读取文件为 ByteArray
                            apiViewModel.uploadImage(imageData, onSuccess = {
                                // 上传成功后的操作
                            }, onFailure = {
                                // 上传失败后的操作
                            })
                        } else {
                            println("File does not exist at path: $path")
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Drawing")
        }

        // Go to Shared Drawings
        Button(onClick = { navController.navigate("shared_drawings") }) {
            Text("View Shared Drawings")
        }
        // ----
        // Drawing area
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath.add(
                                Point(
                                    offset.x,
                                    offset.y,
                                    pencil.color.value,
                                    pencil.size.value
                                )
                            )
                        },
                        onDrag = { change, _ ->
                            val point = change.position
                            currentPath.add(
                                Point(
                                    point.x,
                                    point.y,
                                    pencil.color.value,
                                    pencil.size.value
                                )
                            )
                        }
                    )
                }
        ) {
            // Draw the saved image if available
            savedImageBitmap?.let { image ->
                drawImage(image = image, topLeft = Offset.Zero)
            }
            val canvasWidth = size.width  // 获取 Canvas 的宽度
            val canvasHeight = size.height  // 获取 Canvas 的高度

            if (sensorEnabled) {
                currentX = currentX.coerceIn(0f, canvasWidth - 20f)
                currentY = currentY.coerceIn(0f, canvasHeight - 20f)

                if (sensorPath.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(sensorPath[0].x, sensorPath[0].y)
                        for (point in sensorPath) {
                            lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = selectedColor,  // 使用选择的颜色
                        style = Stroke(width = 5f)
                    )
                }

                drawCircle(
                    color = selectedColor,  // 使用选择的颜色绘制小球
                    radius = 20f,
                    center = Offset(currentX, currentY)
                ) }

                // Draw current paths
                for (point in currentPath) {
                    drawCircle(
                        color = point.color,
                        radius = point.size,
                        center = Offset(point.x, point.y)
                    )

            }}

        Spacer(modifier = Modifier.height(16.dp))

        if (creatingNewDrawing) {
            BasicTextField(
                value = name,
                onValueChange = {name = it},
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerText ->
                    Box(
                        Modifier
                            .background(Color.Yellow)
                            .padding(16.dp)
                    ) {
                        if (name.isEmpty()) {
                            Text("Please enter a name for your drawing")
                            innerText()
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save button
        Button(
            onClick = {
                val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(bitmap)
                savedImageBitmap?.let { imageBitmap ->
                    canvas.drawBitmap(imageBitmap.asAndroidBitmap(), 0f, 0f, null)
                }
                for (point in currentPath) {
                    canvas.drawCircle(
                        point.x,
                        point.y,
                        point.size,
                        Paint().apply {
                            color = point.color.toArgb()
                            style = Paint.Style.FILL
                        }
                    )
                }

                scope.launch(Dispatchers.IO) {
                    if (creatingNewDrawing) {
                        filePath = File(context.filesDir, "$name.png").path
                    }
                    if (filePath != null) {
                        saveDrawing(
                            context = context,
                            bitmap = bitmap,
                            name = name.ifEmpty { "Untitled Drawing" },
                            filePath = filePath!!,
                            viewModel = viewModel,
                            navController = navController,
                            drawingId = drawingId
                        )
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No file path!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Drawing")
        }
    } // end of column scope
}


