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
                // 상단 버튼: 다시 찍기 (화이트 보더 + 화이트 텍스트)
                Button(
                    onClick = { 
                        // 촬영된 파일 삭제
                        imageFile.delete()
                        // 카메라 화면으로 돌아가기
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .semantics { contentDescription = "다시 찍기" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "다시 찍기",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                // 하단 버튼: 사진 사용 (화이트 백그라운드 + 블랙 텍스트)
                Button(
                    onClick = {
                        // 정보 추가 선택 화면으로 이동
                        val encodedPath = Uri.encode(decodedPath)
                        navController.navigate("photo_info_choice/$encodedPath")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { contentDescription = "사진 사용하기" },
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
                        text = "사진 사용",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}