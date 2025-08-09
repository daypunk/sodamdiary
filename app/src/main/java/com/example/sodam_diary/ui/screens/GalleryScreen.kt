package com.example.sodam_diary.ui.screens

import android.net.Uri
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val photoManager = remember { PhotoManager(context) }
    val photoRepository = remember { PhotoRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var photos by remember { mutableStateOf<List<PhotoEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // 사진 데이터 로드
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                photos = photoRepository.getAllPhotos()
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
    ScreenLayout(
        showHomeButton = true,
        onHomeClick = { navController.navigate("main") }
    ) {
            // 컨텐츠 영역 - 타이틀 제거하고 상단 여백만
            Box(modifier = Modifier.weight(1f)) {
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
                            contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp) // 상단 여백 추가, 하단 버튼 공간 확보
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
                                                                                onClick = {
                                    // 사진 상세보기로 이동
                                    val encodedPath = Uri.encode(photo.photoPath)
                                    navController.navigate("photo_detail/$encodedPath")
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
            
            // 하단 버튼 영역 - 검색하기만 남김
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 검색하기 버튼 (화이트 백그라운드 + 블랙 텍스트)
                Button(
                    onClick = {
                        navController.navigate("search_step1")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { contentDescription = "사진 검색하기" },
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
                        text = "검색하기",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
    }
}

@Composable
private fun PhotoThumbnail(
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
                contentDescription = "사진 썸네일, 촬영일: ${formatThumbnailDate(photo.captureDate)}"
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

private fun formatThumbnailDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("M월 d일", Locale.KOREAN)
    return formatter.format(date)
}