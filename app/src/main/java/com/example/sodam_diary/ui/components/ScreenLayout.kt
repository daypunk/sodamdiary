package com.example.sodam_diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.sodam_diary.ui.theme.AppBackground
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import kotlinx.coroutines.delay
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
    initialFocusRequester: FocusRequester? = null,
    initialFocusDelayMs: Long = 700,
    suppressHeaderUntilFocused: Boolean = true,
    contentFocusLabel: String? = "화면의 주요 내용",
    screenAnnouncement: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val contentFocusRequester = remember { FocusRequester() }
    var suppressHeaderA11y by remember { mutableStateOf(suppressHeaderUntilFocused) }
    // 화면 진입 시 안내 후 메인 컨텐츠로 초기 포커스 강제 이동
    androidx.compose.runtime.LaunchedEffect(screenAnnouncement, initialFocusRequester) {
        val announcement = screenAnnouncement?.takeIf { it.isNotBlank() }
        if (announcement != null) {
            view.announceForAccessibility(announcement)
            // TalkBack 안내 읽기 직후 포커스 적용을 위해 약간 지연
            delay(initialFocusDelayMs)
        } else {
            // 안내가 없는 경우에도 렌더링 안정화를 위해 소폭 지연
            delay(100)
        }
        // 페이지가 제공한 명시적 초기 포커스 대상이 있으면 우선 사용
        val focused = if (initialFocusRequester != null) {
            try {
                initialFocusRequester.requestFocus()
                true
            } catch (_: IllegalStateException) {
                false
            }
        } else false

        if (!focused) {
            // 안전한 기본 컨텐츠 포커스
            contentFocusRequester.requestFocus()
        }
        // 포커스 적용 이후 헤더를 조금 더 늦게 활성화하여 초기 포커스가 헤더로 이동하는 것을 방지
        delay(500)
        suppressHeaderA11y = false
    }
    // 안전 영역 중 하단만 자동 패딩 적용 + 전체 배경
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .background(AppBackground)
    ) {
        // 메인 컨텐츠 (먼저 선언하여 접근성 순서상 먼저 읽히도록)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = if (showHomeButton || showBackButton || actionIcon != null) 56.dp else 0.dp)
                .focusRequester(contentFocusRequester)
                .focusable()
                .semantics {
                    traversalIndex = 0f
                    if (!contentFocusLabel.isNullOrBlank()) {
                        contentDescription = contentFocusLabel
                    }
                }
        ) {
            content()
        }

        // 헤더와 상태바 오버레이 (시각적 상단 고정)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBackground)
        ) {
            // Status bar 영역을 검은색으로 채움 (접근성 제외)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(AppBackground)
                    .clearAndSetSemantics { }
            )
            // 헤더 영역
            if (showHomeButton || showBackButton || actionIcon != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(AppBackground)
                        .padding(horizontal = 8.dp)
                        .then(
                            if (suppressHeaderA11y) Modifier.clearAndSetSemantics { }
                        else Modifier.semantics { traversalIndex = 2f }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        showHomeButton && onHomeClick != null -> {
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
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        showBackButton && onBackClick != null -> {
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
                                    modifier = Modifier.size(32.dp)
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
        }
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
            contentColor = AppBackground
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
        border = BorderStroke(1.dp, Color.White),
    ) {
        Text(text = text, fontSize = 22.sp, color = Color.White)
    }
}