package com.example.sodam_diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.core.view.ViewCompat

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
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    onHomeClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    screenAnnouncement: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    // 화면 진입 시 목적/사용법을 안내
    if (!screenAnnouncement.isNullOrBlank()) {
        androidx.compose.runtime.LaunchedEffect(screenAnnouncement) {
            // 화면 목적 안내를 즉시 발표
            view.announceForAccessibility(screenAnnouncement!!)
        }
    }
    // 안전 영역 중 하단만 자동 패딩 적용 (상단은 우리가 검은 배경으로 직접 그려줌)
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .background(Color.Black)
    ) {
        // Status bar 영역을 검정으로 채워서 흰 상태바와 겹침 문제 방지
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.Black)
                .clearAndSetSemantics { }
        )
        
        // 헤더 영역 (status bar 아래에 별도로)
        if (showHomeButton || showBackButton || actionIcon != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Black)
                    .padding(horizontal = 8.dp)
                    .semantics {
                        traversalIndex = 1f // 메인 컨텐츠보다 뒤에 읽히도록 설정
                    },
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
                Spacer(modifier = Modifier.weight(1f))
                if (actionIcon != null && onActionClick != null) {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "액션" }
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
        
        // 메인 컨텐츠 (화면에서 가장 먼저 읽히도록 그룹/순서 지정)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    traversalIndex = 0f
                }
        ) {
            content()
        }

        // 별도 하단 Spacer 제거 (safeDrawing Bottom이 처리)
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
    contentDescription: String = text,
    onClickLabel: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (onClickLabel != null) {
                    this.onClick(label = onClickLabel) { true }
                }
            },
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
    contentDescription: String = text,
    onClickLabel: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                this.contentDescription = contentDescription
                if (onClickLabel != null) {
                    this.onClick(label = onClickLabel) { true }
                }
            },
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