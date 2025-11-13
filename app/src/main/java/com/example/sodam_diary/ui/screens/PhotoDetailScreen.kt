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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.sodam_diary.ui.theme.AppBackground
import com.example.sodam_diary.ui.theme.AppSurfaceBackground
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.sodam_diary.data.entity.PhotoEntity
import com.example.sodam_diary.data.repository.PhotoRepository
import com.example.sodam_diary.utils.LocationHelper
import com.example.sodam_diary.utils.PhotoManager
import com.example.sodam_diary.utils.VoicePlayer
import com.example.sodam_diary.ui.components.ScreenLayout
import androidx.compose.ui.platform.LocalView
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
    userInput: String? = null,
    voicePath: String? = null,
    photoIds: List<Long>? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val decodedPath = Uri.decode(imagePath)
    val decodedVoicePath = voicePath?.let { Uri.decode(it) }
    val imageFile = File(decodedPath)
    val photoManager = remember { PhotoManager(context) }
    val locationHelper = remember { LocationHelper(context) }
    val voicePlayer = remember { VoicePlayer(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var photoEntity by remember { mutableStateOf<PhotoEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPlayingVoice by remember { mutableStateOf(false) }
    
    // 이전/다음 사진 네비게이션
    val showNavigation = photoIds != null && photoIds.isNotEmpty()
    val currentPhotoIndex = remember(photoEntity, photoIds) {
        if (photoEntity != null && photoIds != null) {
            photoIds.indexOf(photoEntity!!.id)
        } else -1
    }
    val hasPrevious = currentPhotoIndex > 0
    val hasNext = currentPhotoIndex >= 0 && currentPhotoIndex < (photoIds?.size ?: 0) - 1
    
    // VoicePlayer 콜백 설정
    DisposableEffect(Unit) {
        voicePlayer.setCallbacks(
            onComplete = {
                isPlayingVoice = false
                view.announceForAccessibility("재생이 완료되었습니다")
            },
            onError = { error ->
                isPlayingVoice = false
                view.announceForAccessibility(error)
            }
        )
        
        onDispose {
            voicePlayer.release()
        }
    }
    
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
                    
                    // 사용자 입력 유무와 관계없이 서버로 전송 (file만 있어도 통신)
                    val result = photoRepository.savePhotoWithEmotion(
                        photoPath = decodedPath,
                        userDescription = userInput,
                        userVoicePath = decodedVoicePath,
                        latitude = locationData?.latitude,
                        longitude = locationData?.longitude,
                        locationName = locationData?.locationName,
                        captureDate = captureDate
                    )
                    
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
    val descriptionFocus = remember { FocusRequester() }

    ScreenLayout(
        showHomeButton = true,
        onHomeClick = { navController.navigate("main") },
        initialFocusRequester = descriptionFocus,
        contentFocusLabel = "사진 설명"
    ) {
        if (isLoading) {
            // 로딩 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .semantics { 
                        traversalIndex = 0f
                        contentDescription = "인공지능이 사진을 분석하고 일기를 작성하고 있습니다. 잠시만 기다려주세요."
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "일기를 적고 있어요",
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
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .semantics { 
                            traversalIndex = 0f
                            contentDescription = "오류 메시지. $errorMessage"
                        }
                )
                
                Button(
                    onClick = { navController.navigate("main") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AppBackground
                    ),
                    modifier = Modifier.semantics {
                        traversalIndex = 1f
                        contentDescription = "홈으로 돌아가기 버튼. 메인 화면으로 이동합니다."
                    }
                ) {
                    Text(
                        text = "홈으로",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // 사진 상세 정보 화면 - 재구성 (사진 위, 텍스트+버튼 하단 고정)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 여백
                Spacer(modifier = Modifier.height(24.dp))

                // 상단 사진 영역 (원본 비율 유지 + 화살표 네비게이션)
                if (bitmap != null) {
                    if (showNavigation) {
                        // 갤러리/검색 결과에서 진입: 좌우 화살표 표시
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 왼쪽 화살표 (이전 사진)
                            IconButton(
                                onClick = {
                                    if (hasPrevious && photoIds != null) {
                                        val prevPhotoId = photoIds[currentPhotoIndex - 1]
                                        coroutineScope.launch {
                                            val prevPhoto = PhotoRepository(context).getPhotoById(prevPhotoId)
                                            if (prevPhoto != null) {
                                                val encodedPath = Uri.encode(prevPhoto.photoPath)
                                                val encodedPhotoIds = Uri.encode(photoIds.joinToString(","))
                                                navController.navigate("photo_detail/$encodedPath?photoIds=$encodedPhotoIds") {
                                                    popUpTo("photo_detail/{imagePath}") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = hasPrevious,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { 
                                        contentDescription = if (hasPrevious) "이전 사진 보기" else "첫 번째 사진입니다"
                                    }
                            ) {
                                Text(
                                    text = "◀",
                                    fontSize = 28.sp,
                                    color = if (hasPrevious) Color.White else Color.Gray
                                )
                            }
                            
                            // 중앙 사진
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "저장된 사진이 표시됩니다",
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 320.dp)
                                    .padding(horizontal = 8.dp)
                                    .semantics { 
                                        traversalIndex = 2f
                                    },
                                contentScale = ContentScale.Fit
                            )
                            
                            // 오른쪽 화살표 (다음 사진)
                            IconButton(
                                onClick = {
                                    if (hasNext && photoIds != null) {
                                        val nextPhotoId = photoIds[currentPhotoIndex + 1]
                                        coroutineScope.launch {
                                            val nextPhoto = PhotoRepository(context).getPhotoById(nextPhotoId)
                                            if (nextPhoto != null) {
                                                val encodedPath = Uri.encode(nextPhoto.photoPath)
                                                val encodedPhotoIds = Uri.encode(photoIds.joinToString(","))
                                                navController.navigate("photo_detail/$encodedPath?photoIds=$encodedPhotoIds") {
                                                    popUpTo("photo_detail/{imagePath}") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = hasNext,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { 
                                        contentDescription = if (hasNext) "다음 사진 보기" else "마지막 사진입니다"
                                    }
                            ) {
                                Text(
                                    text = "▶",
                                    fontSize = 28.sp,
                                    color = if (hasNext) Color.White else Color.Gray
                                )
                            }
                        }
                    } else {
                        // 촬영 후 진입: 화살표 없이 사진만
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "저장된 사진이 표시됩니다",
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .semantics { 
                                    traversalIndex = 2f
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                // 사진과 하단 섹션 사이 여백
                Spacer(modifier = Modifier.height(16.dp))

                // 하단 섹션: 텍스트와 버튼 (높이 제한)
                Surface(
                    color = AppSurfaceBackground,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // 남은 공간 차지하되, 최소 높이 보장
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 일기 텍스트 영역 (스크롤 가능, 가장 먼저 읽힘)
                        Box(
                            modifier = Modifier
                                .weight(1f) // 버튼을 제외한 공간 전부
                                .fillMaxWidth()
                                .padding(16.dp)
                                .semantics { traversalIndex = 0f } // 가장 먼저 읽힘
                        ) {
                            val state = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(state)
                            .semantics(mergeDescendants = false) { }
                    ) {
                        photoEntity?.let { photo ->
                            val dateLine = formatDateOnly(photo.captureDate)
                            val city = extractCityFromAddress(photo.locationName)
                            val locationLine = city?.let { "${it}에서 찍은 사진이에요" }
                            val descriptionLine = photo.imageDescription ?: "설명 없음"

                            val combined = listOfNotNull(dateLine, locationLine, descriptionLine)
                                .joinToString("\n\n")

                            Text(
                                text = combined,
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(descriptionFocus)
                                    .focusable()
                                            .semantics { 
                                                contentDescription = "일기 내용. $combined"
                                            }
                                    )
                                }
                            }
                        }

                        // 하단 버튼 영역 (최하단 고정)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .semantics { traversalIndex = 1f }, // 텍스트 다음에 읽힘
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 내 목소리 듣기 버튼 (조건부 렌더링)
                            if (!photoEntity?.userVoicePath.isNullOrBlank()) {
                                Button(
                                    onClick = {
                                        val voicePathToPlay = photoEntity!!.userVoicePath!!
                                        if (isPlayingVoice) {
                                            voicePlayer.stopVoice()
                                            isPlayingVoice = false
                                            view.announceForAccessibility("재생을 중지했습니다")
                                        } else {
                                            val success = voicePlayer.playVoice(voicePathToPlay)
                                            if (success) {
                                                isPlayingVoice = true
                                                view.announceForAccessibility("음성을 재생합니다")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .semantics { 
                                            contentDescription = if (isPlayingVoice) {
                                                "재생 중지 버튼. 지금 재생 중인 음성을 멈춥니다."
                                            } else {
                                                "내 목소리 듣기 버튼. 사진을 찍을 때 녹음했던 음성을 재생합니다. 당시의 상황과 감정을 다시 들어볼 수 있습니다."
                                            }
                                        },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlayingVoice) Color.Red else Color(0xFF4CAF50),
                                        contentColor = Color.White
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 8.dp,
                                        pressedElevation = 4.dp
                                    )
                                ) {
                                    Text(
                                        text = if (isPlayingVoice) "⏹️ 재생 중지" else "🎤 내 목소리 듣기",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            // 갤러리로 버튼
                            Button(
                                onClick = {
                                    navController.navigate("gallery") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .semantics { 
                                        contentDescription = "갤러리로 버튼. 다른 사진들을 보기 위해 갤러리 화면으로 돌아갑니다."
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
            color = AppBackground.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = AppBackground,
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