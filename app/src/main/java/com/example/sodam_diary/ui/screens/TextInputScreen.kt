
package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.net.Uri
import java.io.File

@Composable
fun TextInputScreen(
    navController: NavController,
    imagePath: String
) {
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
    var textInput by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "사진에 대한 추가 설명을 입력해주세요.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 100.dp)
                    .semantics { contentDescription = "텍스트 입력 안내 문구" }
            )
            
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("사진 설명") },
                placeholder = { Text("예: 오늘 날씨가 정말 좋네요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .semantics { contentDescription = "사진 설명 입력 필드" }
            )

            Button(
                onClick = {
                    // TODO: PhotoRepository를 사용해서 사진과 텍스트를 서버에 전송하고 저장
                    // val photoRepository = PhotoRepository(context)
                    // val locationHelper = LocationHelper(context)
                    // lifecycleScope.launch {
                    //     val location = locationHelper.getCurrentLocation()
                    //     photoRepository.savePhotoWithEmotion(
                    //         photoPath = decodedPath,
                    //         userDescription = textInput.ifBlank { null },
                    //         latitude = location?.latitude,
                    //         longitude = location?.longitude,
                    //         locationName = location?.locationName,
                    //         captureDate = System.currentTimeMillis()
                    //     )
                    // }
                    navController.navigate("gallery")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
                    .semantics { contentDescription = "저장 버튼" }
            ) {
                Text("저장하기")
            }
        }
    }
}

// 호환성을 위한 별칭
@Composable
fun VoiceInputScreen(navController: NavController, imagePath: String) {
    TextInputScreen(navController, imagePath)
}
