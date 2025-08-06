package com.example.sodam_diary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// === 데이터베이스 모델 ===

/**
 * 사진과 관련된 모든 정보를 저장하는 데이터 클래스
 */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val photoPath: String,                     // 촬영한 사진 (파일 경로)
    val captureDate: Long,                     // 촬영한 일자 (타임스탬프)
    val latitude: Double?,                     // 촬영한 장소 - 위도
    val longitude: Double?,                    // 촬영한 장소 - 경도
    val locationName: String?,                 // 촬영한 장소 - 주소 텍스트
    val imageDescription: String?,             // 서버로부터 받은 이미지 설명
    val userDescription: String?               // 사용자가 입력한 설명 (선택사항)
)

// === 네트워크 모델 ===

/**
 * 서버 응답 데이터 클래스
 */
data class ApiResponse(
    val status: String,
    val message: String,
    val data: ResponseData?
)

/**
 * 응답 데이터 내부 구조
 */
data class ResponseData(
    val file_description: String
)

// === 유틸리티 모델 ===

/**
 * 위치 정보 데이터 클래스
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val locationName: String?
)