package com.example.sodam_diary

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.sodam_diary.ui.screens.SearchStep1Screen
import com.example.sodam_diary.ui.screens.SearchStep2Screen
import com.example.sodam_diary.ui.screens.SearchStep3Screen
import com.example.sodam_diary.ui.screens.SearchStep4Screen
import com.example.sodam_diary.ui.screens.SearchResultScreen
import com.example.sodam_diary.ui.theme.SodamdiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Status bar 표시 설정
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
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
            "photo_detail/{imagePath}?userInput={userInput}",
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType },
                navArgument("userInput") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            val userInput = backStackEntry.arguments?.getString("userInput")
            val decodedUserInput = if (userInput != null) Uri.decode(userInput) else null
            PhotoDetailScreen(navController, imagePath, decodedUserInput)
        }
        
        // 검색 플로우
        composable("search_step1") { SearchStep1Screen(navController) }
        composable(
            "search_step2/{selectedYear}",
            arguments = listOf(navArgument("selectedYear") { type = NavType.StringType })
        ) { backStackEntry ->
            val selectedYear = backStackEntry.arguments?.getString("selectedYear")
            val year = when (selectedYear) {
                null, "null", "-" -> null
                else -> selectedYear
            }
            SearchStep2Screen(navController, year)
        }
        composable(
            "search_step3/{selectedYear}/{selectedMonth}",
            arguments = listOf(
                navArgument("selectedYear") { type = NavType.StringType },
                navArgument("selectedMonth") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val selectedYear = backStackEntry.arguments?.getString("selectedYear")
            val selectedMonth = backStackEntry.arguments?.getString("selectedMonth")
            val year = when (selectedYear) { null, "null", "-" -> null; else -> selectedYear }
            val month = when (selectedMonth) { null, "null", "-" -> null; else -> selectedMonth }
            SearchStep3Screen(navController, year, month)
        }
        composable(
            "search_step4/{selectedYear}/{selectedMonth}/{selectedLocation}",
            arguments = listOf(
                navArgument("selectedYear") { type = NavType.StringType },
                navArgument("selectedMonth") { type = NavType.StringType },
                navArgument("selectedLocation") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val selectedYear = backStackEntry.arguments?.getString("selectedYear")
            val selectedMonth = backStackEntry.arguments?.getString("selectedMonth")
            val selectedLocation = backStackEntry.arguments?.getString("selectedLocation")
            val year = when (selectedYear) { null, "null", "-" -> null; else -> selectedYear }
            val month = when (selectedMonth) { null, "null", "-" -> null; else -> selectedMonth }
            val location = when (selectedLocation) { null, "null", "-" -> null; else -> selectedLocation }
            SearchStep4Screen(navController, year, month, location)
        }
        composable(
            "search_result/{selectedYear}/{selectedMonth}/{selectedLocation}/{selectedContent}",
            arguments = listOf(
                navArgument("selectedYear") { type = NavType.StringType },
                navArgument("selectedMonth") { type = NavType.StringType },
                navArgument("selectedLocation") { type = NavType.StringType },
                navArgument("selectedContent") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val selectedYear = backStackEntry.arguments?.getString("selectedYear")
            val selectedMonth = backStackEntry.arguments?.getString("selectedMonth")
            val selectedLocation = backStackEntry.arguments?.getString("selectedLocation")
            val selectedContent = backStackEntry.arguments?.getString("selectedContent")
            val year = when (selectedYear) { null, "null", "-" -> null; else -> selectedYear }
            val month = when (selectedMonth) { null, "null", "-" -> null; else -> selectedMonth }
            val location = when (selectedLocation) { null, "null", "-" -> null; else -> selectedLocation }
            val content = when (selectedContent) { null, "null", "-" -> null; else -> selectedContent }
            SearchResultScreen(navController, year, month, location, content)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
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
                    .semantics { contentDescription = "소담일기 앱" }
            )
            
            // 중앙 영역: 버튼들과 가이드 텍스트를 한 묶음으로
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // 버튼들을 담을 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                // 카메라 버튼
                Button(
                    onClick = { 
                        navController.navigate("camera")
                    },
                    modifier = Modifier
                        .size(width = 140.dp, height = 80.dp)
                        .semantics { contentDescription = "카메라로 사진 촬영하기" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
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
                        .size(width = 140.dp, height = 80.dp)
                        .semantics { contentDescription = "갤러리에서 사진 보기" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = "갤러리",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                }
                
                // 가이드 텍스트 - 버튼들과 함께 중앙 그룹에 포함
    Text(
                    text = "버튼을 터치하여 시작하세요",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .semantics { contentDescription = "사용 안내: 버튼을 터치하여 시작하세요" }
                )
            }
            
            // 하단 여백
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SodamdiaryTheme {
        MainScreen(rememberNavController())
    }
}