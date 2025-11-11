package com.example.sodam_diary

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sodam_diary.ui.screens.CameraScreen
import com.example.sodam_diary.ui.screens.PhotoPreviewScreen
import com.example.sodam_diary.ui.screens.PhotoInfoChoiceScreen
import com.example.sodam_diary.ui.screens.PhotoDetailScreen
import com.example.sodam_diary.ui.screens.GalleryScreen
import com.example.sodam_diary.ui.theme.SodamdiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge 모드 활성화: 시스템 바 영역까지 그리되, Compose 쪽에서 안전영역 패딩 처리
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 상태/내비게이션 바를 검은색으로 고정하고 아이콘을 밝게 유지 (항상 블랙 UI 요구)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        controller.isAppearanceLightStatusBars = false  // 밝은 아이콘(화이트)
        controller.isAppearanceLightNavigationBars = false  // 밝은 아이콘(화이트)
        
        setContent {
            SodamdiaryTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController) }
        composable("camera") { CameraScreen(navController) }
        composable("gallery") { GalleryScreen(navController) }
        composable(
            "preview/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            PhotoPreviewScreen(navController, imagePath)
        }
        composable(
            "photo_info_choice/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            PhotoInfoChoiceScreen(navController, imagePath)
        }
        composable(
            "photo_detail/{imagePath}?userInput={userInput}&voicePath={voicePath}",
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("userInput") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("voicePath") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            val userInput = backStackEntry.arguments?.getString("userInput")
            val voicePath = backStackEntry.arguments?.getString("voicePath")
            val decodedUserInput = if (userInput != null) Uri.decode(userInput) else null
            val decodedVoicePath = if (voicePath != null) Uri.decode(voicePath) else null
            PhotoDetailScreen(navController, imagePath, decodedUserInput, decodedVoicePath)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val titleFocusRequester = remember { FocusRequester() }
    // 시각장애인용 고대비 디자인: 검은 배경 + status bar 영역
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 상단 검은색 헤더 영역 (status bar 공간 확보)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.Black)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 앱 제목 - 위쪽에 배치 (조금 낮춤)
            Text(
                text = "소담일기",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 120.dp)
                    .semantics {
                        contentDescription = "소담일기, 사진을 읽어드릴게요"
                        heading()
                    }
                    .focusRequester(titleFocusRequester)
            )
            LaunchedEffect(Unit) { titleFocusRequester.requestFocus() }

            // 버튼들을 담을 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
            ) {
            // 카메라 버튼
            Button(
                onClick = {
                    navController.navigate("camera")
                },
                modifier = Modifier
                    .size(width = 160.dp, height = 100.dp)
                    .semantics { contentDescription = "카메라로 사진 촬영하기" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "카메라",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // 갤러리 버튼
            Button(
                onClick = {
                    navController.navigate("gallery")
                },
                modifier = Modifier
                    .size(width = 160.dp, height = 100.dp)
                    .semantics { contentDescription = "갤러리에서 사진 보기" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 4.dp
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "갤러리",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            }

            // 하단 여백
            Spacer(modifier = Modifier.height(160.dp))
        }
    }
}


// Preview 삭제: 빌드 안정성을 위해 미사용 프리뷰 제거