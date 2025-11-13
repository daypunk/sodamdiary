package com.example.sodam_diary.ui.screens

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.sodam_diary.ui.theme.AppBackground
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.sodam_diary.ui.components.ScreenLayout
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    // 권한 요청 런처
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // 권한 확인 및 요청
    LaunchedEffect(Unit) {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // 시각장애인용 고대비 디자인
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
    if (hasCameraPermission) {
            CameraContent(
                context = context,
                lifecycleOwner = lifecycleOwner,
                navController = navController
            )
        } else {
            // 권한 요청 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "카메라 권한이 필요합니다",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .semantics { contentDescription = "카메라 권한이 필요합니다" }
                )
                
                Button(
                    onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AppBackground
                    ),
                    modifier = Modifier.semantics { contentDescription = "권한 허용하기" }
                ) {
                    Text(
                        text = "권한 허용",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AppBackground
                    ),
                    modifier = Modifier.semantics { contentDescription = "홈으로 돌아가기" }
                ) {
                    Text(
                        text = "홈으로",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CameraContent(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    navController: NavController
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var cameraError by remember { mutableStateOf<String?>(null) }
    
    val captureButtonFocus = remember { FocusRequester() }
    
    // 화면 진입 시 촬영 버튼에 포커스 요청 (매번 요청)
    LaunchedEffect(navController.currentBackStackEntry) {
        kotlinx.coroutines.delay(200) // 카메라 초기화 대기
        captureButtonFocus.requestFocus()
    }

    ScreenLayout(
        showHomeButton = true,
        onHomeClick = { navController.popBackStack() },
        initialFocusRequester = captureButtonFocus,
        contentFocusLabel = "카메라 미리보기"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics { traversalIndex = 1f } // 버튼 다음에 읽힘
        ) {
        if (cameraError != null) {
            // 카메라 에러 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "카메라 오류",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = cameraError!!,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AppBackground
                    )
                ) {
                    Text(
                        text = "홈으로",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        
                        Log.d("CameraScreen", "카메라 초기화 성공")
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "카메라 바인딩 실패", e)
                        cameraError = "카메라를 초기화할 수 없습니다.\n${e.message}"
                    }
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 하단 컨트롤 영역 - 먼저 읽힘
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppBackground.copy(alpha = 0.7f))
                .padding(24.dp)
                .semantics { traversalIndex = 0f }, // 가장 먼저 읽힘
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 촬영 버튼 (화이트 백그라운드 + 블랙 텍스트)
            Button(
                onClick = {
                    if (cameraError == null && imageCapture != null) {
                        captureImage(
                            context = context,
                            imageCapture = imageCapture,
                            executor = executor,
                            onSuccess = { imagePath ->
                                // 촬영 완료 후 프리뷰 화면으로 이동
                                val encodedPath = android.net.Uri.encode(imagePath)
                                navController.navigate("preview/$encodedPath")
                            },
                            onError = { error ->
                                cameraError = "사진 촬영에 실패했습니다.\n$error"
                            }
                        )
                    }
                },
                enabled = cameraError == null && imageCapture != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .focusRequester(captureButtonFocus)
                    .focusable()
                    .semantics { 
                        contentDescription = "촬영"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = AppBackground
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "촬영",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        }
        
        DisposableEffect(Unit) {
            onDispose {
                executor.shutdown()
            }
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture?,
    executor: ExecutorService,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val imageCapture = imageCapture ?: run {
        onError("카메라가 초기화되지 않았습니다")
        return
    }
    
    try {
        // 앱 내부 저장소에 파일 생성
        val photosDir = File(context.filesDir, "photos").apply {
            if (!exists()) {
                val created = mkdirs()
                if (!created) {
                    onError("사진 저장 폴더를 생성할 수 없습니다")
                    return
                }
            }
        }
        
        val photoFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraScreen", "사진 저장됨: ${photoFile.absolutePath}")
                    onSuccess(photoFile.absolutePath)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraScreen", "사진 저장 실패", exception)
                    onError(exception.message ?: "알 수 없는 오류")
                }
            }
        )
    } catch (e: Exception) {
        Log.e("CameraScreen", "사진 촬영 중 오류", e)
        onError(e.message ?: "알 수 없는 오류")
    }
}