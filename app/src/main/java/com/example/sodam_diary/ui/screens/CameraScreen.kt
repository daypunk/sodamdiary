
package com.example.sodam_diary.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.sodam_diary.ui.components.CameraPreview
import java.io.File
import com.example.sodam_diary.util.getCurrentLocation
import com.example.sodam_diary.model.ImageMetaData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    // 1. 위치 권한 상태 추가
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

// 2. 위치 권한 런처 추가
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

// 3. 기존 LaunchedEffect 안에 위치 권한 요청 추가
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    if (hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                context = context,
                lifecycleOwner = lifecycleOwner,
                imageCapture = imageCapture
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val photoFile = File(
                            context.filesDir,
                            "photo_${System.currentTimeMillis()}.jpg"
                        )
                        val outputOptions = OutputFileOptions.Builder(photoFile).build()
                        val date = Date(photoFile.lastModified())
                        val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)

                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    coroutineScope.launch {
                                        val locationResult = getCurrentLocation(context)

                                        val metaData = ImageMetaData(
                                            filePath = photoFile.absolutePath,
                                            latitude = locationResult?.latitude,
                                            longitude = locationResult?.longitude,
                                            location = locationResult?.address,
                                            createdAt = createdAt
                                        )

                                        Log.d("Camera", "사진 저장됨: ${metaData.filePath}")
                                        Log.d("Camera", "위도: ${metaData.latitude}, 경도: ${metaData.longitude}")
                                        Log.d("Camera", "주소: ${metaData.location}")

                                        val encodedPath = Uri.encode(photoFile.absolutePath)
                                        navController.navigate("preview/$encodedPath")
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("Camera", "사진 저장 실패", exception)
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(bottom = 16.dp)
                        .semantics { contentDescription = "사진 촬영 버튼" }
                ) {
                    Text("📸 촬영")
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("카메라 권한이 필요합니다.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("권한 요청하기")
                }
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController)
        }
        composable("camera") {
            CameraScreen(navController)
        }
        composable(
            "preview/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            PreviewScreen(navController, imagePath)
        }
    }
}

