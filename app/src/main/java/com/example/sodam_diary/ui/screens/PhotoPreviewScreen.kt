package com.example.sodam_diary.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import com.example.sodam_diary.utils.PhotoManager
import com.example.sodam_diary.ui.components.ScreenLayout
import com.example.sodam_diary.ui.components.PrimaryActionButton
import com.example.sodam_diary.ui.components.SecondaryActionButton
import java.io.File

@Composable
fun PhotoPreviewScreen(
    navController: NavController,
    imagePath: String
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
    val photoManager = remember { PhotoManager(context) }
    
    // 이미지 로드
    val bitmap = remember(decodedPath) {
        photoManager.loadRotatedBitmap(imageFile)
    }
    
    // 시각장애인용 고대비 디자인 + status bar 대응
    ScreenLayout {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 제목 - 메인화면과 동일한 위치
            Text(
                text = "사진 미리보기",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .semantics { contentDescription = "사진 미리보기" }
            )
            
            // 사진 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "촬영된 사진",
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { contentDescription = "촬영된 사진이 표시됩니다" },
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "이미지를 불러올 수 없습니다",
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { 
                            contentDescription = "이미지를 불러올 수 없습니다" 
                        }
                    )
                }
            }
            
            // 하단 버튼 영역 - 새로운 세로 정렬 디자인
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecondaryActionButton(
                    text = "다시 찍기",
                    onClick = {
                        imageFile.delete()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "다시 찍기" }
                )
                
                // 하단 버튼: 사진 사용 (화이트 백그라운드 + 블랙 텍스트)
                PrimaryActionButton(
                    text = "사진 사용",
                    onClick = {
                        val encodedPath = Uri.encode(decodedPath)
                        navController.navigate("photo_info_choice/$encodedPath")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "사진 사용하기" }
                )
            }
        }
    }
}