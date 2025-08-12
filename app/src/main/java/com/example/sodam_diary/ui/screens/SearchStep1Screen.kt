package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.sodam_diary.ui.components.ScreenLayout
import com.example.sodam_diary.ui.components.SecondaryActionButton

@Composable
fun SearchStep1Screen(navController: NavController) {
    var selectedYear by remember { mutableStateOf<String?>(null) }
    val years = listOf("2025", "2024", "2023")
    
    // 시각장애인용 고대비 디자인 + status bar 대응 + 헤더 뒤로가기 버튼
    val titleFocus = remember { FocusRequester() }

    ScreenLayout(
        showBackButton = true,
        onBackClick = { navController.popBackStack() },
        initialFocusRequester = titleFocus,
        contentFocusLabel = "년도를 알려주세요"
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 타이틀
            Text(
                text = "년도를 알려주세요",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .focusRequester(titleFocus)
                    .focusable()
                    .semantics { contentDescription = "년도를 알려주세요" }
            )
            
            // 중앙 컨텐츠 - 년도 버튼들
            Column(
                modifier = Modifier.weight(1f).semantics { traversalIndex = 0f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                years.forEach { year ->
                    Button(
                        onClick = { 
                            selectedYear = year
                            navController.navigate("search_step2/$year")
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(80.dp)
                            .padding(vertical = 8.dp)
                            .semantics { contentDescription = "${year}년 선택" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Text(
                            text = "${year}년",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // 하단 버튼 영역 - 건너뛰기만
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .semantics { traversalIndex = 1f },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 건너뛰기 버튼 (회색 배경 + 흰 텍스트)
                SecondaryActionButton(
                    text = "건너뛰기",
                    onClick = {
                        // 연도 스킵 시 월도 자동 스킵하여 바로 3단계로 이동
                        navController.navigate("search_step3/-/-")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics { contentDescription = "건너뛰기" }
                )
            }
        }
    }
}