package com.example.sodam_diary.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.NavController
import com.example.sodam_diary.data.entity.PhotoEntity
import com.example.sodam_diary.data.repository.PhotoRepository
import com.example.sodam_diary.utils.PhotoManager
import com.example.sodam_diary.ui.components.ScreenLayout
import kotlinx.coroutines.launch

@Composable
fun SearchResultScreen(
    navController: NavController,
    selectedYear: String?,
    selectedMonth: String?, 
    selectedLocation: String?,
    selectedContent: String?
) {
    val context = LocalContext.current
    val photoManager = remember { PhotoManager(context) }
    val photoRepository = remember { PhotoRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var searchResults by remember { mutableStateOf<List<PhotoEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 검색 실행
    LaunchedEffect(selectedYear, selectedMonth, selectedLocation, selectedContent) {
        coroutineScope.launch {
            try {
                isLoading = true
                
                // 파라미터 정리
                val year = when (selectedYear) { null, "null", "-" -> null; else -> selectedYear }
                val monthRaw = when (selectedMonth) { null, "null", "-" -> null; else -> selectedMonth }
                val month = monthRaw?.padStart(2, '0')
                val location = when (selectedLocation) { null, "null", "-" -> null; else -> selectedLocation }
                val content = when (selectedContent) { null, "null", "-" -> null; else -> Uri.decode(selectedContent) }
                
                // 검색 로직: 단순하고 일관된 단계별 필터 (연/월/장소)
                var results = photoRepository.getAllPhotos()
                
                val yearInt = year?.toIntOrNull()
                val monthInt = month?.toIntOrNull()
                
                if (yearInt != null) {
                    results = results.filter { extractYear(it.captureDate) == yearInt }
                }
                if (monthInt != null) {
                    results = results.filter { extractMonth(it.captureDate) == monthInt }
                }
                if (!location.isNullOrBlank()) {
                    results = results.filter { it.locationName?.contains(location, ignoreCase = true) == true }
                }
                
                // 내용 필터링 (이미지 설명이나 사용자 설명에서 검색)
                if (content != null) {
                    results = results.filter { photo ->
                        (photo.imageDescription?.contains(content, ignoreCase = true) == true) ||
                        (photo.userDescription?.contains(content, ignoreCase = true) == true)
                    }
                }
                
                searchResults = results
                
            } catch (e: Exception) {
                errorMessage = "검색 중 오류가 발생했습니다: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 시각장애인용 고대비 디자인 + status bar 대응 + 헤더 뒤로가기 버튼
    val resultCountFocus = remember { FocusRequester() }
    
    ScreenLayout(
        showBackButton = true,
        onBackClick = { navController.popBackStack() },
        initialFocusRequester = resultCountFocus,
        contentFocusLabel = "검색 결과"
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 컨텐츠 영역
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
                                    contentDescription = "검색하고 있습니다" 
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "검색하고 있습니다...",
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
                        }
                    }
                    
                    searchResults.isEmpty() -> {
                        // 빈 결과 화면
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "해당하는 사진이 없어요",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "다른 조건으로 검색해보세요",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    else -> {
                        // 검색 결과 표시 (3열 그리드)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                        ) {
                            // 결과 개수 표시
                            item {
                                Text(
                                    text = "검색 결과: ${searchResults.size}개",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                        .focusRequester(resultCountFocus)
                                        .focusable()
                                        .semantics { contentDescription = "검색 결과 ${searchResults.size}개" }
                                )
                            }
                            
                            // 3열 그리드로 사진 배치
                            val chunkedPhotos = searchResults.sortedByDescending { it.captureDate }.chunked(3)
                            items(chunkedPhotos) { rowPhotos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPhotos.forEach { photo ->
                                        SearchResultThumbnail(
                                            photo = photo,
                                            photoManager = photoManager,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                // 사진 상세보기로 이동 (검색 결과 리스트 전달)
                                                val encodedPath = Uri.encode(photo.photoPath)
                                                val sortedPhotos = searchResults.sortedByDescending { it.captureDate }
                                                val photoIdsString = sortedPhotos.joinToString(",") { it.id.toString() }
                                                val encodedPhotoIds = Uri.encode(photoIdsString)
                                                navController.navigate("photo_detail/$encodedPath?photoIds=$encodedPhotoIds")
                                            }
                                        )
                                    }
                                    // 빈 공간 채우기 (3개 미만인 경우)
                                    repeat(3 - rowPhotos.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 하단 버튼 - 갤러리로
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .semantics { traversalIndex = 1f },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        navController.navigate("gallery") {
                            popUpTo("gallery") { inclusive = true }
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
            }
        }
    }
}

@Composable
private fun SearchResultThumbnail(
    photo: PhotoEntity,
    photoManager: PhotoManager,
    modifier: Modifier = Modifier,
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
                contentDescription = "검색 결과 사진, 촬영일: ${formatSearchResultDate(photo.captureDate)}"
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
    }
}

private fun formatSearchResultDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("M월 d일", java.util.Locale.KOREAN)
    return formatter.format(date)
}

private fun extractYear(timestamp: Long): Int {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = timestamp
    return cal.get(java.util.Calendar.YEAR)
}

private fun extractMonth(timestamp: Long): Int {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = timestamp
    // Calendar.MONTH는 0부터 시작하므로 +1
    return cal.get(java.util.Calendar.MONTH) + 1
}