package com.example.sodam_diary.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.sodam_diary.data.entity.PhotoEntity
import com.example.sodam_diary.data.repository.PhotoRepository
import com.example.sodam_diary.utils.PhotoManager
import com.example.sodam_diary.utils.VoiceRecorder
import com.example.sodam_diary.ui.components.ScreenLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val view = LocalView.current
    val photoManager = remember { PhotoManager(context) }
    val photoRepository = remember { PhotoRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var photos by remember { mutableStateOf<List<PhotoEntity>>(emptyList()) }
    var allPhotos by remember { mutableStateOf<List<PhotoEntity>>(emptyList()) } // 전체 사진 저장용
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    // 검색 관련 상태
    var showSearchDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isSearchRecording by remember { mutableStateOf(false) }
    var currentSearchVoicePath by remember { mutableStateOf<String?>(null) }
    var isSearchMode by remember { mutableStateOf(false) } // 검색 결과 표시 중인지 여부
    
    // VoiceRecorder
    val voiceRecorder = remember { VoiceRecorder(context) }
    val searchVoiceRecorder = remember { VoiceRecorder(context) }
    
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
                view.announceForAccessibility("마이크 권한이 필요합니다")
            } else {
                showSearchDialog = true
            }
        }
    )
    
    // VoiceRecorder 콜백 설정 (음성 검색용)
    DisposableEffect(Unit) {
        searchVoiceRecorder.setCallbacks(
            onTranscription = { text ->
                searchQuery = text
                isSearchRecording = false
                view.announceForAccessibility("검색어: $text")
            },
            onError = { error ->
                isSearchRecording = false
                view.announceForAccessibility(error)
                // 오류 발생 시 다이얼로그 닫기
                showSearchDialog = false
                searchQuery = ""
            },
            onReady = {
                view.announceForAccessibility("녹음이 시작되었습니다. 검색어를 말씀해주세요")
            }
        )
        
        onDispose {
            if (isSearchRecording) {
                searchVoiceRecorder.cancelRecording()
            }
        }
    }
    
    // 사진 데이터 로드
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val loadedPhotos = photoRepository.getAllPhotos()
                photos = loadedPhotos
                allPhotos = loadedPhotos // 전체 사진 저장
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "사진을 불러오는데 실패했습니다: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // 연월별로 그룹핑
    val groupedPhotos = remember(photos) {
        photos.groupBy { photo ->
            val date = Date(photo.captureDate)
            val formatter = SimpleDateFormat("yyyy년 M월", Locale.KOREAN)
            formatter.format(date)
        }.toList().sortedByDescending { (_, photoList) ->
            photoList.maxOfOrNull { it.captureDate } ?: 0L
        }
    }
    
    // 시각장애인용 고대비 디자인 + status bar 대응 + 헤더 홈 버튼
    val firstMonthTitleFocus = remember { FocusRequester() }

    ScreenLayout(
        showHomeButton = true,
        onHomeClick = { navController.navigate("main") },
        actionIcon = if (selectionMode) Icons.Filled.Close else Icons.Filled.Delete,
        onActionClick = {
            if (selectionMode) {
                // 취소하기: 선택 해제 및 모드 종료
                selectedIds = emptySet()
                selectionMode = false
            } else {
                // 선택 삭제 모드 진입
                selectionMode = true
            }
        },
        initialFocusRequester = firstMonthTitleFocus,
        contentFocusLabel = "사진 목록",
        // 하단 버튼 영역보다 컨텐츠 영역이 먼저 읽히도록 기본 설정 유지
    ) {
            // 컨텐츠 영역 - 타이틀 제거하고 상단 여백만
            Box(modifier = Modifier.weight(1f).semantics { traversalIndex = 0f }) {
                when {
                    isLoading -> {
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
                                    contentDescription = "사진을 불러오고 있습니다" 
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "사진을 불러오고 있습니다...",
                                fontSize = 18.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    errorMessage != null -> {
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
                    }
                    
                    photos.isEmpty() -> {
                        // 빈 갤러리 화면
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "아직 저장된 사진이 없습니다",
                                fontSize = 20.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "카메라로 첫 번째 사진을 촬영해보세요!",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    else -> {
                        // 사진 갤러리
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp) // 상단 여백 축소, 하단 버튼 공간 확보
                        ) {
                            items(groupedPhotos) { (monthYear, monthPhotos) ->
                                // 월별 타이틀
                                Text(
                                    text = monthYear,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                        .then(
                                            if (groupedPhotos.firstOrNull()?.first == monthYear)
                                                Modifier.focusRequester(firstMonthTitleFocus).focusable()
                                            else Modifier
                                        )
                                        .semantics { contentDescription = "$monthYear 사진들" }
                                )
                                
                                // 3열 그리드로 사진 배치
                                val chunkedPhotos = monthPhotos.sortedByDescending { it.captureDate }.chunked(3)
                                chunkedPhotos.forEach { rowPhotos ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPhotos.forEach { photo ->
                                            PhotoThumbnail(
                                                photo = photo,
                                                photoManager = photoManager,
                                                modifier = Modifier.weight(1f),
                                                selectionMode = selectionMode,
                                                selected = selectedIds.contains(photo.id),
                                                onToggleSelect = {
                                                    selectedIds = selectedIds.toMutableSet().also { set ->
                                                        if (set.contains(photo.id)) set.remove(photo.id) else set.add(photo.id)
                                                    }
                                                },
                                                onClick = {
                                                    if (selectionMode) {
                                                        // 선택 모드에서는 토글만 수행
                                                        selectedIds = selectedIds.toMutableSet().also { set ->
                                                            if (set.contains(photo.id)) set.remove(photo.id) else set.add(photo.id)
                                                        }
                                                    } else {
                                                        // 상세로 이동 (전체 사진 ID 리스트 전달)
                                                        val encodedPath = Uri.encode(photo.photoPath)
                                                        val sortedPhotos = photos.sortedByDescending { it.captureDate }
                                                        val photoIdsString = sortedPhotos.joinToString(",") { it.id.toString() }
                                                        val encodedPhotoIds = Uri.encode(photoIdsString)
                                                        navController.navigate("photo_detail/$encodedPath?photoIds=$encodedPhotoIds")
                                                    }
                                                }
                                            )
                                        }
                                        // 빈 공간 채우기 (3개 미만인 경우)
                                        repeat(3 - rowPhotos.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
            
            // 하단 버튼 영역 - 선택삭제 모드 / 검색 모드에 따라 버튼 전환
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .semantics { traversalIndex = 1f },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectionMode) {
                    // 삭제하기 버튼 (빨간색) - 선택 없으면 disabled
                    val enabled = selectedIds.isNotEmpty()
                    Button(
                        onClick = {
                            val idsToDelete = selectedIds
                            if (idsToDelete.isNotEmpty()) {
                                coroutineScope.launch {
                                    idsToDelete.forEach { id ->
                                        photos.find { it.id == id }?.let { photo ->
                                            photoRepository.deletePhoto(photo)
                                        }
                                    }
                                    // 로컬 상태 갱신
                                    val updatedPhotos = photoRepository.getAllPhotos()
                                    photos = updatedPhotos
                                    allPhotos = updatedPhotos
                                    selectedIds = emptySet()
                                    selectionMode = false
                                    isSearchMode = false // 검색 모드도 해제
                                }
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .semantics { contentDescription = "선택한 사진 삭제" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) Color(0xFFD32F2F) else Color(0xFFBDBDBD),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "삭제하기",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (isSearchMode) {
                    // 검색 결과 표시 중 - 갤러리로 돌아가기 버튼
                    Button(
                        onClick = { 
                            photos = allPhotos // 전체 사진 복원
                            isSearchMode = false
                            view.announceForAccessibility("전체 갤러리로 돌아갑니다")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .semantics { contentDescription = "전체 갤러리로 돌아가기" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
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
                } else {
                    // 일반 모드 - 음성 검색하기 버튼
                    Button(
                        onClick = { 
                            if (micPermissionGranted.value) {
                                // 즉시 녹음 시작
                                searchQuery = ""
                                currentSearchVoicePath = searchVoiceRecorder.startRecording()
                                isSearchRecording = true
                                showSearchDialog = true
                                view.announceForAccessibility("녹음이 시작되었습니다. 검색할 내용을 말씀해주세요")
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .semantics { contentDescription = "음성 검색하기, 버튼을 누르면 바로 녹음이 시작됩니다" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "음성 검색",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
    }
    
    // 전사 완료 시 자동으로 검색 시작
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && !isSearchRecording && showSearchDialog && !isSearching) {
            isSearching = true
            view.announceForAccessibility("검색을 시작합니다")
            
            coroutineScope.launch {
                try {
                    val results = photoRepository.searchPhotosByVoice(searchQuery)
                    if (results.isEmpty()) {
                        view.announceForAccessibility("검색 결과가 없습니다")
                        isSearchMode = false // 결과 없으면 검색 모드 해제
                    } else {
                        view.announceForAccessibility("${results.size}개의 사진을 찾았습니다")
                        photos = results
                        isSearchMode = true // 검색 모드 활성화
                    }
                } catch (e: Exception) {
                    view.announceForAccessibility("검색에 실패했습니다")
                    isSearchMode = false
                } finally {
                    isSearching = false
                    showSearchDialog = false
                    searchQuery = ""
                }
            }
        }
    }
    
    // 간단한 음성 검색 다이얼로그
    if (showSearchDialog && !isSearching) {
        SimpleRecordingDialog(
            isRecording = isSearchRecording,
            onStop = {
                if (isSearchRecording) {
                    searchVoiceRecorder.stopRecording()
                    isSearchRecording = false
                }
            },
            onCancel = {
                if (isSearchRecording) {
                    searchVoiceRecorder.cancelRecording()
                    isSearchRecording = false
                }
                showSearchDialog = false
                searchQuery = ""
            }
        )
    }
    
    // 검색 중 다이얼로그
    if (showSearchDialog && isSearching) {
        SearchingDialog()
    }
}

@Composable
private fun PhotoThumbnail(
    photo: PhotoEntity,
    photoManager: PhotoManager,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit
) {
    val bitmap = remember(photo.photoPath) {
        photoManager.loadRotatedBitmap(photo.photoPath)
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f) // 정사각형
            .clip(RoundedCornerShape(12.dp)) // 라운드 모서리
            .background(Color.Gray.copy(alpha = 0.3f))
            .clickable { onClick() }
            .semantics { 
                contentDescription = "사진보기 버튼, ${formatThumbnailDate(photo.captureDate)}에 찍은 사진이에요"
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 이미지 로드 실패시 플레이스홀더
            Text(
                text = "이미지\n없음",
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // 선택 모드일 때 우상단 체크박스 오버레이
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
            modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .border(2.dp, if (selected) Color.Red else Color.White, RoundedCornerShape(6.dp))
                        .clickable { onToggleSelect() }
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red)
                        ) {}
                    }
                }
            }
        }
    }
}

private fun formatThumbnailDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("M월 d일", Locale.KOREAN)
    return formatter.format(date)
}

// 간단한 녹음 다이얼로그 (중지 버튼만)
@Composable
private fun SimpleRecordingDialog(
    isRecording: Boolean,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    val view = LocalView.current
    
    Dialog(onDismissRequest = { if (!isRecording) onCancel() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 중지 버튼만 표시
                Button(
                    onClick = {
                        onStop()
                        view.announceForAccessibility("녹음을 중지했습니다")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics {
                            contentDescription = "녹음 중지하기, 말씀이 끝나면 눌러주세요"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "중지",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 검색 중 다이얼로그
@Composable
private fun SearchingDialog() {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.Black
                )
                Text(
                    text = "검색하고 있어요...",
                    fontSize = 18.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}