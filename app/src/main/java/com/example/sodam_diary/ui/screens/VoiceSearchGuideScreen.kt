package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun VoiceSearchGuideScreen(navController: NavController) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "음성으로 이미지 검색을 하려면\n하단의 버튼을 눌러주세요.\n\n예시: \"강아지 있는 사진 보여줘\"",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = {
                    // TODO: 음성 인식 기능 시작
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text("🎙 음성 검색 시작")
            }
        }
    }
}
