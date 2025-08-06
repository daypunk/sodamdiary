package com.example.sodam_diary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SearchScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "사진 검색",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // 년도 입력
            OutlinedTextField(
                value = selectedYear,
                onValueChange = { selectedYear = it },
                label = { Text("년도") },
                placeholder = { Text("예: 2024") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            // 월 입력
            OutlinedTextField(
                value = selectedMonth,
                onValueChange = { selectedMonth = it },
                label = { Text("월") },
                placeholder = { Text("예: 1") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            // 위치 입력
            OutlinedTextField(
                value = selectedLocation,
                onValueChange = { selectedLocation = it },
                label = { Text("위치") },
                placeholder = { Text("예: 서울") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    // TODO: PhotoRepository를 사용해서 검색 조건에 따라 사진 검색
                    // val photoRepository = PhotoRepository(context)
                    // lifecycleScope.launch {
                    //     val photos = when {
                    //         selectedYear.isNotBlank() && selectedMonth.isNotBlank() && selectedLocation.isNotBlank() ->
                    //             photoRepository.getPhotosByYearMonthAndLocation(selectedYear.toInt(), selectedMonth.toInt(), selectedLocation)
                    //         selectedYear.isNotBlank() && selectedMonth.isNotBlank() ->
                    //             photoRepository.getPhotosByYearMonth(selectedYear.toInt(), selectedMonth.toInt())
                    //         selectedLocation.isNotBlank() ->
                    //             photoRepository.getPhotosByLocation(selectedLocation)
                    //         else -> photoRepository.getAllPhotos()
                    //     }
                    // }
                    navController.navigate("gallery")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text("검색하기")
            }
        }
    }
}

// 호환성을 위한 별칭
@Composable
fun VoiceSearchGuideScreen(navController: NavController) {
    SearchScreen(navController)
}
