package com.example.sodam_diary.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sodam_diary.data.entity.PhotoEntity
import com.example.sodam_diary.data.repository.PhotoRepository
import com.example.sodam_diary.utils.LocationHelper
import com.example.sodam_diary.utils.PhotoManager
import com.example.sodam_diary.ui.components.ScreenLayout
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhotoDetailScreen(
    navController: NavController,
    imagePath: String,
    userInput: String? = null
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
    val photoManager = remember { PhotoManager(context) }
    val locationHelper = remember { LocationHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var photoEntity by remember { mutableStateOf<PhotoEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 이미지 로드
    val bitmap = remember(decodedPath) {
        photoManager.loadRotatedBitmap(imageFile)
    }
    
    // 위치 권한 상태 관리
    fun checkLocationGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    var locationPermissionAsked by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember { mutableStateOf(checkLocationGranted()) }

    // 위치 권한 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            locationPermissionAsked = true
            locationPermissionGranted = result.values.any { it }
        }
    )

    fun ensureLocationPermission() {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 사진 정보 로드 (DB에서 찾거나 새로 저장)
    LaunchedEffect(decodedPath, userInput, locationPermissionGranted, locationPermissionAsked) {
        // 최초 1회 권한 요청만 트리거하고, 결과에 따라 다음 스텝 진행
        if (!locationPermissionGranted && !locationPermissionAsked) {
            ensureLocationPermission()
            return@LaunchedEffect
        }
        coroutineScope.launch {
            try {
                isLoading = true
                val photoRepository = PhotoRepository(context)
                
                // 먼저 DB에서 기존 데이터 찾아보기
                val allPhotos = photoRepository.getAllPhotos()
                val existingPhoto = allPhotos.find { it.photoPath == decodedPath }
                
                if (existingPhoto != null) {
                    // 기존 사진이 있으면 그것을 표시
                    photoEntity = existingPhoto
                } else {
                    // 기존 사진이 없으면 새로 저장
                    val locationData = locationHelper.getCurrentLocation()
                    val captureDate = System.currentTimeMillis()
                    
                    val result = if (!userInput.isNullOrBlank()) {
                        // 사용자 입력이 있으면 서버 통신
                        photoRepository.savePhotoWithEmotion(
                            photoPath = decodedPath,
                            userDescription = userInput,
                            latitude = locationData?.latitude,
                            longitude = locationData?.longitude,
                            locationName = locationData?.locationName,
                            captureDate = captureDate
                        )
                    } else {
                        // 사용자 입력이 없으면 로컬만 저장
                        photoRepository.savePhotoLocal(
                            photoPath = decodedPath,
                            userDescription = null,
                            latitude = locationData?.latitude,
                            longitude = locationData?.longitude,
                            locationName = locationData?.locationName,
                            captureDate = captureDate
                        )
                    }
                    
                    if (result.isSuccess) {
                        // 저장 후 DB에서 다시 조회해서 표시
                        val photoId = result.getOrDefault(0L)
                        photoEntity = photoRepository.getPhotoById(photoId)
                    } else {
                        errorMessage = "사진 저장에 실패했습니다."
                    }
                }
            } catch (e: Exception) {
                errorMessage = "오류가 발생했습니다: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 시각장애인용 고대비 디자인 + status bar 대응
    ScreenLayout {
        if (isLoading) {
            // 로딩 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.semantics { 
                        contentDescription = "사진을 저장하고 있습니다" 
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "사진을 저장하고 있습니다...",
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else if (errorMessage != null) {
            // 에러 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMessage!!,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = { navController.navigate("main") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
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
            // 사진 상세 정보 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 여백만 - 타이틀 완전 제거로 통일
                Spacer(modifier = Modifier.height(80.dp))
                
                // 사진 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "저장된 사진",
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics { contentDescription = "저장된 사진이 표시됩니다" },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                // 사진 정보 영역
                photoEntity?.let { photo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            val dateLine = formatDateOnly(photo.captureDate)
                            val city = extractCityFromAddress(photo.locationName)
                            val locationLine = city?.let { "${it}에서 찍은 사진이에요" }
                            val descriptionLine = photo.imageDescription ?: "설명 없음"

                            val combined = listOfNotNull(dateLine, locationLine, descriptionLine)
                                .joinToString("\n")

                            Text(
                                text = combined,
                                fontSize = 18.sp,
                                color = Color.Black,
                                modifier = Modifier.semantics { contentDescription = combined }
                            )
                        }
                    }
                }
                
                // 하단 버튼 - 완전 통일된 스타일과 기능
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 모든 경우에 갤러리로 버튼 (화이트 백그라운드)
                    Button(
                        onClick = { 
                            navController.navigate("gallery") {
                                popUpTo("main") { inclusive = false }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .semantics { contentDescription = "갤러리로 돌아가기" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "갤러리로",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    contentDescription: String
) {
    Column(
        modifier = Modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREAN)
    return formatter.format(date)
}

private fun formatLocation(photo: PhotoEntity): String {
    val name = photo.locationName
    if (!name.isNullOrBlank()) return name
    val lat = photo.latitude
    val lng = photo.longitude
    return if (lat != null && lng != null) {
        "위치(좌표): ${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
    } else {
        "위치 정보 없음"
    }
}

private fun formatDateOnly(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    return formatter.format(date)
}

// 간단한 도시명 추출: 쉼표로 분리된 주소에서 앞/뒤 토큰 중 한글/영문 단어를 선택
private fun extractCityFromAddress(address: String?): String? {
    if (address.isNullOrBlank()) return null
    // LocationHelper에서 이미 '시/도 + 구' 혹은 적절한 축약형으로 반환
    return address
}