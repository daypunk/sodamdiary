package com.example.sodam_diary.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.sodam_diary.data.repository.PhotoRepository
import com.example.sodam_diary.ui.components.ScreenLayout
import com.example.sodam_diary.ui.components.PrimaryActionButton
import com.example.sodam_diary.ui.components.SecondaryActionButton
import com.example.sodam_diary.utils.VoiceRecorder
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable

@Composable
fun PhotoInfoChoiceScreen(
    navController: NavController,
    imagePath: String
) {
    val context = LocalContext.current
    val view = LocalView.current
    val decodedPath = Uri.decode(imagePath)
    val coroutineScope = rememberCoroutineScope()
    val photoRepository = remember { PhotoRepository(context) }
    val locationHelper = remember { com.example.sodam_diary.utils.LocationHelper(context) }
    
    // 상태 관리
    var showDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 백그라운드 API 상태
    var captionResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // VoiceRecorder
    val voiceRecorder = remember { VoiceRecorder(context) }
    var currentVoicePath by remember { mutableStateOf<String?>(null) }
    
    // 마이크 권한 체크
    val micPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == 
            PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            micPermissionGranted.value = granted
            if (!granted) {
                errorMessage = "마이크 권한이 필요합니다"
                view.announceForAccessibility("마이크 권한이 필요합니다")
            }
        }
    )
    
    // 화면 진입 시 백그라운드에서 analyze API 호출
    LaunchedEffect(decodedPath) {
        isAnalyzing = true
        coroutineScope.launch {
            try {
                val photoFile = File(decodedPath)
                if (photoFile.exists()) {
                    // 1단계: BLIP 캡션 분석
                    captionResult = photoRepository.analyzeImageForCaption(decodedPath)
                    if (captionResult != null) {
                        view.announceForAccessibility("사진 분석이 완료되었습니다")
                    }
                }
            } catch (e: Exception) {
                captionResult = null
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    // VoiceRecorder 콜백 설정
    DisposableEffect(Unit) {
        voiceRecorder.setCallbacks(
            onTranscription = { text ->
                transcribedText = text
                isRecording = false
                view.announceForAccessibility("음성 인식이 완료되었습니다. $text")
            },
            onError = { error ->
                errorMessage = error
                isRecording = false
                view.announceForAccessibility(error)
            },
            onReady = {
                view.announceForAccessibility("녹음이 시작되었습니다. 말씀해주세요")
            }
        )
        
        onDispose {
            if (isRecording) {
                voiceRecorder.cancelRecording()
            }
        }
    }
    
    // 시각장애인용 고대비 디자인
    val titleFocus = remember { FocusRequester() }

    ScreenLayout(
        initialFocusRequester = titleFocus
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 여백
            Spacer(modifier = Modifier.height(80.dp))
            
            // 중앙 영역: 타이틀과 본문을 한 묶음으로
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // 타이틀
                Text(
                    text = "사진에 정보를\n추가할까요?",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 48.sp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .focusRequester(titleFocus)
                        .focusable()
                        .semantics { contentDescription = "사진에 정보를 추가할까요?" }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 본문 설명
                Text(
                    text = "지금의 상황이나 감정을 추가하면\n일기가 더욱 다채로워져요.",
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .semantics { 
                            contentDescription = "지금의 상황이나 감정을 추가하면 일기가 더욱 다채로워져요" 
                        }
                )
            }
            
            // 하단 버튼 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 추가하기 버튼
                PrimaryActionButton(
                    text = "추가하기",
                    onClick = { 
                        if (micPermissionGranted.value) {
                            showDialog = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "음성으로 정보 추가하기" }
                )

                // 건너뛰기 버튼
                SecondaryActionButton(
                    text = "건너뛰기",
                    onClick = {
                        coroutineScope.launch {
                            try {
                                view.announceForAccessibility("사진을 저장하고 있습니다")
                                
                                // 위치 정보 가져오기
                                val locationData = locationHelper.getCurrentLocation()
                                
                                // generate API 호출 (userInput은 null)
                                val diaryResult = if (captionResult != null) {
                                    photoRepository.generateDiaryWithLLM(
                                        userInput = null,  // 사용자 입력 없음
                                        blipCaption = captionResult,
                                        latitude = locationData?.latitude,
                                        longitude = locationData?.longitude,
                                        location = locationData?.locationName
                                    )
                                } else {
                                    null
                                }
                                
                                // DB에 저장
                                val result = photoRepository.savePhotoLocal(
                                    photoPath = decodedPath,
                                    userDescription = null,
                                    userVoicePath = null,
                                    latitude = locationData?.latitude,
                                    longitude = locationData?.longitude,
                                    locationName = locationData?.locationName,
                                    captureDate = System.currentTimeMillis(),
                                    caption = captionResult,
                                    imageDescription = diaryResult?.first,
                                    tags = diaryResult?.second
                                )
                                
                                if (result.isSuccess) {
                                    val encodedPath = Uri.encode(decodedPath)
                                    navController.navigate("photo_detail/$encodedPath") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                }
                            } catch (e: Exception) {
                                view.announceForAccessibility("사진 저장에 실패했습니다")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "정보 추가 없이 저장하기" }
                )
            }
        }
    }
    
    // STT 녹음 다이얼로그
    if (showDialog) {
        VoiceRecordingDialog(
            isRecording = isRecording,
            transcribedText = transcribedText,
            isProcessing = isProcessing,
            errorMessage = errorMessage,
            onStartRecording = {
                if (!isRecording) {
                    errorMessage = null
                    transcribedText = ""
                    currentVoicePath = voiceRecorder.startRecording()
                    isRecording = true
                }
            },
            onStopRecording = {
                if (isRecording) {
                    voiceRecorder.stopRecording()
                    isRecording = false
                }
            },
            onConfirm = {
                if (transcribedText.isNotBlank()) {
                    isProcessing = true
                    view.announceForAccessibility("일기를 생성하고 있습니다")
                    
                    coroutineScope.launch {
                        try {
                            // 1. 위치 정보 가져오기
                            val locationData = locationHelper.getCurrentLocation()
                            
                            // 2. generate API 호출
                            val diaryResult = photoRepository.generateDiaryWithLLM(
                                userInput = transcribedText,
                                blipCaption = captionResult,
                                latitude = locationData?.latitude,
                                longitude = locationData?.longitude,
                                location = locationData?.locationName
                            )
                            
                            // 3. DB에 저장
                            val result = photoRepository.savePhotoLocal(
                                photoPath = decodedPath,
                                userDescription = transcribedText,
                                userVoicePath = currentVoicePath,
                                latitude = locationData?.latitude,
                                longitude = locationData?.longitude,
                                locationName = locationData?.locationName,
                                captureDate = System.currentTimeMillis(),
                                caption = captionResult,
                                imageDescription = diaryResult?.first,
                                tags = diaryResult?.second
                            )
                            
                            if (result.isSuccess) {
                                view.announceForAccessibility("일기가 저장되었습니다")
                                showDialog = false
                                
                                // 4. PhotoDetailScreen으로 이동
                                val encodedPath = Uri.encode(decodedPath)
                                navController.navigate("photo_detail/$encodedPath") {
                                    popUpTo("main") { inclusive = false }
                                }
                            } else {
                                errorMessage = "사진 저장에 실패했습니다"
                                view.announceForAccessibility("사진 저장에 실패했습니다")
                            }
                        } catch (e: Exception) {
                            errorMessage = "일기 생성에 실패했습니다: ${e.message}"
                            view.announceForAccessibility("일기 생성에 실패했습니다")
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            },
            onCancel = {
                if (isRecording) {
                    voiceRecorder.cancelRecording()
                    isRecording = false
                }
                showDialog = false
                transcribedText = ""
                errorMessage = null
                currentVoicePath = null
            },
            onRetry = {
                errorMessage = null
                transcribedText = ""
            }
        )
    }
}

@Composable
private fun VoiceRecordingDialog(
    isRecording: Boolean,
    transcribedText: String,
    isProcessing: Boolean,
    errorMessage: String?,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val view = LocalView.current
    
    Dialog(onDismissRequest = { if (!isRecording && !isProcessing) onCancel() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 다이얼로그 타이틀
                Text(
                    text = "음성으로 추가하기",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                // 상태에 따른 안내 메시지
                when {
                    isProcessing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color.Black
                        )
                        Text(
                            text = "일기를 생성하고 있어요...",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            fontSize = 16.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    transcribedText.isNotBlank() -> {
                        Text(
                            text = "인식된 내용:",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = transcribedText,
                            fontSize = 18.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = "버튼을 눌러 말씀해주세요",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // STT 버튼 (토글형)
                if (!isProcessing && errorMessage == null) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                onStopRecording()
                            } else if (transcribedText.isBlank()) {
                                onStartRecording()
                            }
                        },
                        enabled = transcribedText.isBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .semantics {
                                contentDescription = if (isRecording) {
                                    "녹음 중지하기, 말씀이 끝나면 눌러주세요"
                                } else {
                                    "녹음 시작하기, 말씀하시면 바로 녹음하고 글자로 바꿔드려요"
                                }
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Red else Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isRecording) "🔴 녹음 중지" else "🎤 녹음 시작",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        isProcessing -> {
                            // 처리 중일 때는 버튼 없음
                        }
                        errorMessage != null -> {
                            // 오류 발생 시: 다시 시도, 취소
                            Button(
                                onClick = onRetry,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .semantics { contentDescription = "다시 시도하기" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("다시 시도", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .semantics { contentDescription = "취소하기" }
                            ) {
                                Text("취소", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        transcribedText.isNotBlank() -> {
                            // 전사 완료 시: 확인, 취소
                            Button(
                                onClick = {
                                    view.announceForAccessibility("일기를 생성합니다")
                                    onConfirm()
                                },
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(50.dp)
                                    .semantics { contentDescription = "확인, 일기 생성하기" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("확인", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .semantics { contentDescription = "취소하기" }
                            ) {
                                Text("취소", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        else -> {
                            // 대기 중: 취소만
                            TextButton(
                                onClick = onCancel,
                                enabled = !isRecording,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .semantics { contentDescription = "취소하기" }
                            ) {
                                Text("취소", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
