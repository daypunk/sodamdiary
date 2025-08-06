package com.example.sodam_diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sodam_diary.ui.screens.MainScreen
import com.example.sodam_diary.ui.screens.CameraScreen
import com.example.sodam_diary.ui.screens.PreviewScreen
import com.example.sodam_diary.ui.screens.TextInputScreen
import com.example.sodam_diary.ui.screens.GalleryScreen
import com.example.sodam_diary.ui.screens.SearchScreen
import com.example.sodam_diary.ui.screens.GalleryPreviewScreen
import com.example.sodam_diary.ui.theme.SodamdiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            SodamdiaryTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController)
        }
        composable("camera") {
            CameraScreen(navController)
        }
        composable(
            "preview/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            PreviewScreen(navController, imagePath)
        }
        composable(
            "text_input/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            TextInputScreen(navController, imagePath)
        }
        composable("gallery") {
            GalleryScreen(navController)
        }
        composable("search") {
            SearchScreen(navController)
        }
        composable(
            "gallery_preview/{imagePath}",
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
            GalleryPreviewScreen(navController, imagePath)
        }
    }
}