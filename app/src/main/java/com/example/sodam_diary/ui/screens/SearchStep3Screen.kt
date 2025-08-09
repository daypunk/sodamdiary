package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sodam_diary.ui.components.ScreenLayout

@Composable
fun SearchStep3Screen(
    navController: NavController, 
    selectedYear: String?, 
    selectedMonth: String?
) {
    var locationInput by remember { mutableStateOf("") }
    
    // 시각장애인용 고대비 디자인 + status bar 대응 + 헤더 뒤로가기 버튼
    ScreenLayout(
        showBackButton = true,
        onBackClick = { navController.popBackStack() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 타이틀
            Text(
                text = "어디서 찍었나요?",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .semantics { contentDescription = "어디서 찍었나요?" }
            )
            
            // 중앙 컨텐츠 - 텍스트 입력
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 텍스트 입력 영역
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 텍스트필드
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "장소 입력 필드" },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            singleLine = true
                        )
                        
                        // 플레이스홀더
                        if (locationInput.isEmpty()) {
                            Text(
                                text = "서울",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    
                    // 전송 버튼
                    Button(
                        onClick = {
                            if (locationInput.isNotBlank()) {
                                val yearParam = selectedYear ?: "null"
                                val monthParam = selectedMonth ?: "null"
                                val locationParam = android.net.Uri.encode(locationInput)
                                navController.navigate("search_step4/$yearParam/$monthParam/$locationParam")
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .semantics { contentDescription = "전송" },
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
                            text = "→",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 실선
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White)
                )
            }
            
            // 하단 버튼 영역 - 다음으로만
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 건너뛰기 버튼 (회색 배경 + 흰 텍스트)
                Button(
                    onClick = {
                        val yearParam = selectedYear ?: "null"
                        val monthParam = selectedMonth ?: "null"
                        navController.navigate("search_step4/$yearParam/$monthParam/null")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { contentDescription = "건너뛰기" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = "건너뛰기",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}