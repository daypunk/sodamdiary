package com.example.sodam_diary.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.io.File
import com.example.sodam_diary.util.loadRotatedBitmap

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    var imageFiles by remember {
        mutableStateOf(
            context.filesDir
                .listFiles { file -> file.extension == "jpg" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        )
    }

    val selectedFiles = remember { mutableStateListOf<File>() }

    fun refresh() {
        imageFiles = context.filesDir
            .listFiles { file -> file.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(imageFiles) { file ->
                val bitmap = loadRotatedBitmap(file)?.asImageBitmap()
                val isSelected = selectedFiles.contains(file)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val encodedPath = Uri.encode(file.absolutePath)
                            navController.navigate("gallery_preview/$encodedPath")
                        }
                        .padding(12.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            if (it) selectedFiles.add(file) else selectedFiles.remove(file)
                        }
                    )
                    bitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "저장된 이미지",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(end = 16.dp)
                        )
                    }
                    Text(file.name, modifier = Modifier.weight(1f))
                }
                Divider()
            }
        }

        // 선택 삭제 버튼
        if (selectedFiles.isNotEmpty()) {
            Button(
                onClick = {
                    selectedFiles.forEach { it.delete() }
                    selectedFiles.clear()
                    refresh()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("🗑 선택 삭제 (${selectedFiles.size})")
            }
        }

        // 전체 삭제 버튼
        Button(
            onClick = {
                imageFiles.forEach { it.delete() }
                selectedFiles.clear()
                refresh()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔥 전체 삭제")
        }

        // 음성 검색 버튼
        Button(
            onClick = { navController.navigate("voice_search_guide") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Text("🎤 음성 검색")
        }
    }
}


