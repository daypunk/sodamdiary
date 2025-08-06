package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import java.io.File

@Composable
fun MainScreen(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "메인 화면" }, // WCAG 2.2: 명확한 화면 영역 정의
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                onClick = { navController.navigate("camera") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .semantics { contentDescription = "사진 촬영 버튼" }, // WCAG 2.2: 스크린리더 지원
            ) {
                Text("📸 촬영")
            }

            Button(
                onClick = { navController.navigate("gallery") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "사진 갤러리 버튼" },
            ) {
                Text("🖼 갤러리")
            }
        }
    }
}
