package com.example.sodam_diary.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import com.example.sodam_diary.util.loadRotatedBitmap

@Composable
fun GalleryPreviewScreen(
    navController: NavController,
    imagePath: String
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
    val bitmap = loadRotatedBitmap(imageFile)?.asImageBitmap()

    var llmResult by remember { mutableStateOf<String?>(null) }

    // 서버에 이미지 전송 + 응답 받는 로직
    LaunchedEffect(decodedPath) {
        // TODO: 실제 서버에 이미지 전송하고 결과 받아오기
        // 예시: delay(1000); llmResult = "이 사진은 노트북 화면입니다."
        llmResult = "이 사진은 노트북 화면입니다." // 임시 하드코딩
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "선택한 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (llmResult != null) {
                Text(
                    text = "🧠 분석 결과:\n$llmResult",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
