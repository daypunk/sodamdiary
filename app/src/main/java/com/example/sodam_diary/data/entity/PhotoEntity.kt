package com.example.sodam_diary.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사진과 관련된 모든 정보를 저장하는 데이터 클래스
 * - 촬영한 사진
 * - 촬영한 일자
 * - 촬영한 장소
 * - 서버로부터 받은 이미지 설명
 * - 사용자가 입력한 설명
 */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 촬영한 사진 (파일 경로)
    val photoPath: String,
    
    // 촬영한 일자 (타임스탬프)
    val captureDate: Long,
    
    // 촬영한 장소 - 위도
    val latitude: Double?,
    
    // 촬영한 장소 - 경도
    val longitude: Double?,
    
    // 촬영한 장소 - 주소 텍스트
    val locationName: String?,
    
    // 서버로부터 받은 이미지 설명
    val imageDescription: String?,
    
    // 사용자가 입력한 설명 (STT로 변환된 텍스트, 선택사항)
    val userDescription: String?
)