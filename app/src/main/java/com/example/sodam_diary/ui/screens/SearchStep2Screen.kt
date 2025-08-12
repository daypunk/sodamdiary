package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
fun SearchStep2Screen(navController: NavController, selectedYear: String?) {
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    val months = (1..12).map { String.format("%02d", it) }
    
    // 시각장애인용 고대비 디자인 + status bar 대응 + 헤더 뒤로가기 버튼
    val titleFocus = remember { FocusRequester() }

    ScreenLayout(
        showBackButton = true,
        onBackClick = { navController.popBackStack() },
        initialFocusRequester = titleFocus,
        screenAnnouncement = "월 선택 화면입니다. 월을 선택하거나 건너뛰기 버튼을 누르세요."
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 타이틀
            Text(
                text = "몇 월인지\n알려주세요",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 32.dp)
                    .focusRequester(titleFocus)
                    .focusable()
                    .semantics { contentDescription = "몇 월인지 알려주세요" }
            )
            
            // 중앙 컨텐츠 - 월 버튼들 (4x3 그리드)
            Column(
                modifier = Modifier
                    .weight(1f).semantics { traversalIndex = 0f }
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(months) { month ->
                        Button(
                            onClick = { 
                                selectedMonth = month
                                val yearParam = selectedYear ?: "-"
                                val yearSafe = android.net.Uri.encode(yearParam)
                                val monthSafe = android.net.Uri.encode(month)
                                navController.navigate("search_step3/$yearSafe/$monthSafe")
                            },
                            modifier = Modifier
                                .aspectRatio(1f)
                                .semantics { contentDescription = "${month}월 선택" },
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
                                text = "${month}월",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // 하단 버튼 영역 - 다음으로만
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
                        val yearSafe = android.net.Uri.encode(yearParam)
                        val monthSafe = android.net.Uri.encode("-")
                        navController.navigate("search_step3/$yearSafe/$monthSafe")
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