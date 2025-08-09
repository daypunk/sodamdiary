package com.example.sodam_diary.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.sodam_diary.ui.components.ScreenLayout
import com.example.sodam_diary.utils.PhotoManager
import java.io.File

@Composable
fun PhotoInfoChoiceScreen(
    navController: NavController,
    imagePath: String
) {
    val context = LocalContext.current
    val decodedPath = Uri.decode(imagePath)
    val imageFile = File(decodedPath)
    val photoManager = remember { PhotoManager(context) }
    
    var showDialog by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    
    // 이미지 로드
    val bitmap = remember(decodedPath) {
        photoManager.loadRotatedBitmap(imageFile)
    }
    
    // 시각장애인용 고대비 디자인
    ScreenLayout {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 여백
            Spacer(modifier = Modifier.height(80.dp))
            
            // 중앙 영역: 타이틀과 본문을 한 묶음으로
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // 타이틀
                Text(
                    text = "사진에 정보를\n추가할까요?",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 48.sp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .semantics { contentDescription = "사진에 정보를 추가할까요?" }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 본문 설명
                Text(
                    text = "지금의 상황이나 감정을 추가하면\n일기가 더욱 다채로워져요.",
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .semantics { 
                            contentDescription = "지금의 상황이나 감정을 추가하면 일기가 더욱 다채로워져요" 
                        }
                )
            }
            
            // 하단 버튼 영역 - 다른 화면들과 동일한 하단 위치
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 상단 버튼: 괜찮아요 (화이트 보더 + 화이트 텍스트)
                Button(
                    onClick = { 
                        // 바로 결과 화면으로 (userDescription 없이)
                        val encodedPath = Uri.encode(decodedPath)
                        navController.navigate("photo_detail/$encodedPath") {
                            popUpTo("main") { inclusive = false }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .semantics { contentDescription = "정보 추가 없이 저장하기" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "괜찮아요",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                // 하단 버튼: 추가하기 (화이트 백그라운드 + 블랙 텍스트)
                Button(
                    onClick = { 
                        showDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { contentDescription = "정보 추가하기" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        text = "추가하기",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // 정보 입력 다이얼로그
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f) // 폭을 더 넓게
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 다이얼로그 타이틀
                    Text(
                        text = "정보 추가하기",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // 텍스트 입력 필드
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("상황이나 감정을 입력해주세요") },
                        placeholder = { Text("예: 친구들과 맛있는 점심을 먹었어요") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "사용자 정보 입력 필드" },
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    // 버튼 영역
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 취소 버튼 - 배경없는 검은색 텍스트
                        TextButton(
                            onClick = { 
                                showDialog = false
                                userInput = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .semantics { contentDescription = "취소" }
                        ) {
                            Text(
                                text = "취소",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        
                        // 확인 버튼 - 크기 증가
                        Button(
                            onClick = { 
                                // userDescription과 함께 결과 화면으로
                                val encodedPath = Uri.encode(decodedPath)
                                navController.navigate("photo_detail/$encodedPath?userInput=${Uri.encode(userInput)}") {
                                    popUpTo("main") { inclusive = false }
                                }
                                showDialog = false
                            },
                            modifier = Modifier
                                .weight(1.5f) // 확인 버튼을 더 넓게
                                .height(50.dp) // 높이 증가
                                .semantics { contentDescription = "확인" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "확인",
                                fontSize = 20.sp, // 텍스트 크기 증가
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}