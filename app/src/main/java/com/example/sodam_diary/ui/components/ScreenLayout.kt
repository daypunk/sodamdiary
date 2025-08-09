package com.example.sodam_diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 모든 화면에서 사용할 공통 레이아웃
 * - status bar 공간 확보
 * - 검은색 헤더 영역
 * - 시각장애인용 고대비 디자인
 */
@Composable
fun ScreenLayout(
    modifier: Modifier = Modifier,
    showHomeButton: Boolean = false,
    showBackButton: Boolean = false,
    onHomeClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Status bar 공간 확보
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.Black)
        )
        
        // 헤더 영역 (status bar 아래에 별도로)
        if (showHomeButton || showBackButton) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Black)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    showHomeButton && onHomeClick != null -> {
                        // 홈 아이콘 버튼
                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "홈으로 돌아가기" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp) // 아이콘 크기 증가
                            )
                        }
                    }
                    showBackButton && onBackClick != null -> {
                        // 뒤로가기 화살표 아이콘 버튼
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(48.dp)
                                .semantics { contentDescription = "이전으로 돌아가기" }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp) // 아이콘 크기 증가
                            )
                        }
                    }
                }
            }
        }
        
        // 메인 컨텐츠
        content()
    }
}

/**
 * 접근성 친화적 기본 액션 버튼 (화이트 배경, 블랙 텍스트)
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = text
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { this.contentDescription = contentDescription },
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
        Text(text = text, fontSize = 22.sp)
    }
}

/**
 * 접근성 친화적 보조 액션 버튼 (투명 배경, 화이트 보더/텍스트)
 */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = text
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder,
    ) {
        Text(text = text, fontSize = 22.sp, color = Color.White)
    }
}