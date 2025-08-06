
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
fun VoiceInputScreen(
    navController: NavController,
    imagePath: String
) {
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
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
                text = "사진에 대한 추가 설명을 위해\n하단의 '음성 입력' 버튼을 눌러주세요.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(top = 100.dp)
                    .semantics { contentDescription = "음성 입력 안내 문구" }
            )

            Button(
                onClick = {
                    // TODO: 음성 입력 시작 및 결과 → 서버 LLM 전달
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
                    .semantics { contentDescription = "음성 입력 버튼" }
            ) {
                Text("🎤 음성 입력")
            }
        }
    }
}
