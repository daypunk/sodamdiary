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
import androidx.compose.ui.semantics.traversalIndex
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGeneratingDiary by remember { mutableStateOf(false) } // 일기 생성 중 로딩 상태
    
    // 백그라운드 API 상태
    var captionResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // VoiceRecorder (STT만 사용, 파일 저장 안 함)
    val voiceRecorder = remember { VoiceRecorder(context) }
    
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
                    // 1단계: BLIP 캡션 분석 (TalkBack 안내 제거)
                    captionResult = photoRepository.analyzeImageForCaption(decodedPath)
                }
            } catch (e: Exception) {
                captionResult = null
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    // 녹음된 음성 파일 경로 저장
    var recordedVoicePath by remember { mutableStateOf<String?>(null) }
    
    // VoiceRecorder 콜백 설정
    DisposableEffect(Unit) {
        voiceRecorder.setCallbacks(
            { text ->
                transcribedText = text
                isRecording = false
                // 녹음 완료 시 음성 파일 경로 저장
                recordedVoicePath = voiceRecorder.getCurrentVoicePath()
                // TalkBack 간섭 방지: 음성 인식 완료 안내 제거
            },
            { error ->
                isRecording = false
                showDialog = false
                // STT 실패 시 화면 전환하지 않음 - 그냥 다이얼로그만 닫기
                errorMessage = error
                view.announceForAccessibility(error)
            },
            {
                // TalkBack 간섭 방지: 녹음 시작 안내는 버튼 클릭 시에만
            }
        )
        
        onDispose {
            if (isRecording) {
                voiceRecorder.cancelRecording()
            }
        }
    }
    
    // 시각장애인용 고대비 디자인
    val addButtonFocus = remember { FocusRequester() }

    ScreenLayout(
        initialFocusRequester = addButtonFocus,
        contentFocusLabel = "사진에 추가 정보 입력"
    ) {
        if (isGeneratingDiary) {
            // 일기 생성 중 로딩 화면 (TalkBack 간섭 최소화)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { 
                        traversalIndex = 0f
                        contentDescription = "일기를 적고 있어요"
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // CircularProgressIndicator를 TalkBack에서 숨김
                    Box(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Text도 TalkBack에서 숨김 (상위 contentDescription만 읽힘)
                    Box(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                        Text(
                            text = "일기를 적고 있어요",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 여백
            Spacer(modifier = Modifier.height(80.dp))
            
            // 중앙 영역: 타이틀과 본문을 한 묶음으로 (나중에 읽힘)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { traversalIndex = 1f } // 버튼 다음에 읽힘
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
                        .semantics { 
                            contentDescription = "사진에 정보를 추가할까요? 음성으로 지금의 상황이나 감정을 말씀하시면, 인공지능이 사진과 함께 일기를 작성해드립니다."
                        }
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
                            contentDescription = "음성을 추가하면 더 감성적인 일기가 만들어집니다" 
                        }
                )
            }
            
            // 하단 버튼 영역 (먼저 읽힘)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .semantics { traversalIndex = 0f }, // 가장 먼저 읽힘
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 추가하기 버튼
                PrimaryActionButton(
                    text = "추가하기",
                    onClick = { 
                        if (micPermissionGranted.value) {
                            // 즉시 STT 시작 (파일 저장 안 함)
                            errorMessage = null
                            transcribedText = ""
                            voiceRecorder.startRecording()
                            isRecording = true
                            showDialog = true
                            view.announceForAccessibility("녹음이 시작되었습니다. 말씀해주세요")
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(addButtonFocus)
                        .focusable()
                        .semantics { 
                            contentDescription = "음성으로 정보 추가하기. 버튼을 누르면 녹음이 시작됩니다. 지금의 상황이나 감정을 자유롭게 말씀해주세요. 예를 들어, 오늘 날씨가 좋아서 산책을 나왔어요, 라고 말씀하시면 됩니다."
                        }
                )

                // 건너뛰기 버튼
                SecondaryActionButton(
                    text = "건너뛰기",
                    onClick = {
                        // 로딩 상태로 전환
                        isGeneratingDiary = true
                        
                        coroutineScope.launch {
                            try {
                                // 1. analyze 완료 대기 (TalkBack 안내 제거)
                                while (isAnalyzing) {
                                    kotlinx.coroutines.delay(100)
                                }
                                
                                // 2. 위치 정보 가져오기
                                val locationData = locationHelper.getCurrentLocation()
                                
                                // 3. generate API 호출 (userInput은 null)
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
                                
                                // 4. DB에 저장 (userVoicePath는 null)
                                val result = photoRepository.savePhotoLocal(
                                    photoPath = decodedPath,
                                    userDescription = null,
                                    userVoicePath = null, // 건너뛰기는 음성 없음
                                    latitude = locationData?.latitude,
                                    longitude = locationData?.longitude,
                                    locationName = locationData?.locationName,
                                    captureDate = System.currentTimeMillis(),
                                    caption = captionResult,
                                    imageDescription = diaryResult?.first,
                                    tags = diaryResult?.second
                                )
                                
                                if (result.isSuccess) {
                                    // TalkBack 안내 제거
                        val encodedPath = Uri.encode(decodedPath)
                        navController.navigate("photo_detail/$encodedPath") {
                            popUpTo("main") { inclusive = false }
                                    }
                                } else {
                                    view.announceForAccessibility("사진 저장에 실패했습니다")
                                    isGeneratingDiary = false
                                }
                            } catch (e: Exception) {
                                view.announceForAccessibility("일기 생성에 실패했습니다")
                                isGeneratingDiary = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { 
                            contentDescription = "건너뛰기. 음성 추가 없이 사진만으로 일기를 만듭니다. 인공지능이 사진을 분석해서 자동으로 일기를 작성해드립니다."
                        }
                )
            }
        }
        }  // else 블록 닫기
    }
    
    // 전사 완료 시 자동으로 다음 단계 진행 (다이얼로그 닫고 페이지 로딩으로 전환)
    LaunchedEffect(transcribedText, recordedVoicePath) {
        if (transcribedText.isNotBlank() && !isRecording && showDialog) {
            // 다이얼로그 닫기
            showDialog = false
            
            // 페이지 로딩 상태로 전환
            isGeneratingDiary = true
            
            coroutineScope.launch {
                try {
                    // 1. analyze 완료 대기 (TalkBack 안내 제거)
                    while (isAnalyzing) {
                        kotlinx.coroutines.delay(100)
                    }
                    
                    // 2. 위치 정보 가져오기
                    val locationData = locationHelper.getCurrentLocation()
                    
                    // 3. generate API 호출
                    val diaryResult = photoRepository.generateDiaryWithLLM(
                        userInput = transcribedText,
                        blipCaption = captionResult,
                        latitude = locationData?.latitude,
                        longitude = locationData?.longitude,
                        location = locationData?.locationName
                    )
                    
                    // 4. DB에 저장 (녹음된 음성 파일 경로 포함)
                    val result = photoRepository.savePhotoLocal(
                        photoPath = decodedPath,
                        userDescription = transcribedText,
                        userVoicePath = recordedVoicePath, // 녹음된 M4A 파일 경로
                        latitude = locationData?.latitude,
                        longitude = locationData?.longitude,
                        locationName = locationData?.locationName,
                        captureDate = System.currentTimeMillis(),
                        caption = captionResult,
                        imageDescription = diaryResult?.first,
                        tags = diaryResult?.second
                    )
                    
                    if (result.isSuccess) {
                        // TalkBack 안내 제거
                        
                        // 5. PhotoDetailScreen으로 이동
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
                    isGeneratingDiary = false
                }
            }
        }
    }
    
    // 간단한 녹음 다이얼로그 (중지 버튼만)
    if (showDialog) {
        SimpleRecordingDialog(
            isRecording = isRecording,
            onStop = {
                if (isRecording) {
                    voiceRecorder.stopRecording()
                    isRecording = false
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
            }
        )
    }
}

// 간단한 녹음 다이얼로그 (중지 버튼만)
@Composable
private fun SimpleRecordingDialog(
    isRecording: Boolean,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isRecording) onCancel() }) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    onStop()
                    // TalkBack 간섭 방지: 중지 안내 제거 (사용자가 버튼 누른 것만으로 충분)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(80.dp)
                    .semantics {
                        contentDescription = "녹음 중지 버튼. 말씀이 끝나면 이 버튼을 눌러주세요. 지금까지 말씀하신 내용이 자동으로 글자로 변환됩니다."
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "녹음 중지",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

