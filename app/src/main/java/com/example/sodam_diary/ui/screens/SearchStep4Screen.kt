package com.example.sodam_diary.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

@Composable
fun SearchStep4Screen(
    navController: NavController, 
    selectedYear: String?, 
    selectedMonth: String?,
    selectedLocation: String?
) {
    var contentInput by remember { mutableStateOf("") }
    
    // 시각장애인용 고대비 디자인 + status bar 대응 + 헤더 뒤로가기 버튼
    val titleFocus = remember { FocusRequester() }

    ScreenLayout(
        showBackButton = true,
        onBackClick = { navController.popBackStack() },
        initialFocusRequester = titleFocus,
        screenAnnouncement = "내용 입력 화면입니다. 촬영 대상이나 키워드를 입력한 뒤 입력 버튼을 누르세요. 건너뛰기도 가능합니다."
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 타이틀
            Text(
                text = "어떤 걸 찍었나요?",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .focusRequester(titleFocus)
                    .focusable()
                    .semantics { contentDescription = "어떤 걸 찍었나요?" }
            )
            
            // 중앙 컨텐츠 - 텍스트 입력
            Column(
                modifier = Modifier
                    .weight(1f).semantics { traversalIndex = 0f }
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
                    // 디자인 복원: BasicTextField + 플레이스홀더, TalkBack 라벨은 별도 제공
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "촬영 내용",
                            color = Color.White.copy(alpha = 0.001f),
                            fontSize = 1.sp,
                            modifier = Modifier.semantics { contentDescription = "촬영 내용" }
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = contentInput,
                                onValueChange = { contentInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "촬영 내용 입력창" },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 24.sp
                                ),
                                singleLine = true
                            )
                            if (contentInput.isEmpty()) {
                                Text(
                                    text = "예: 바다, 강아지 등",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                    
                    // 전송 버튼
                    Button(
                        onClick = {
                            if (contentInput.isNotBlank()) {
                                // 검색 결과 화면으로 이동
                        val yearParam = selectedYear ?: "-"
                        val monthParam = selectedMonth ?: "-"
                        val locationParam = selectedLocation ?: "-"
                                val searchParams = listOf(
                                    Uri.encode(yearParam),
                                    Uri.encode(monthParam),
                                    Uri.encode(locationParam),
                                    Uri.encode(contentInput)
                                ).joinToString("/")
                                navController.navigate("search_result/$searchParams")
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .semantics { contentDescription = "입력" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
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
            
                // 하단 버튼 영역 - 검색하기만
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .semantics { traversalIndex = 1f },
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecondaryActionButton(
                    text = "건너뛰기",
                    onClick = {
                        val yearParam = selectedYear ?: "-"
                        val monthParam = selectedMonth ?: "-"
                        val locationParam = selectedLocation ?: "-"
                        val searchParams = listOf(
                            Uri.encode(yearParam),
                            Uri.encode(monthParam),
                            Uri.encode(locationParam),
                            Uri.encode("-")
                        ).joinToString("/")
                        navController.navigate("search_result/$searchParams")
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