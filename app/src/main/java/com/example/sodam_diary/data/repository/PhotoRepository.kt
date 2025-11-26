package com.example.sodam_diary.data.repository

import android.content.Context
import android.util.Log
import com.example.sodam_diary.data.database.AppDatabase
import com.example.sodam_diary.data.database.PhotoDao
import com.example.sodam_diary.data.entity.PhotoEntity
import com.example.sodam_diary.data.network.ApiService
import com.example.sodam_diary.data.network.NetworkClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * 사진 관련 데이터를 관리하는 Repository
 * 서버 통신과 로컬 DB 작업을 담당
 */
class PhotoRepository(context: Context) {
    
    private val photoDao: PhotoDao = AppDatabase.getDatabase(context).photoDao()
    private val apiService: ApiService = NetworkClient.apiService
    
    /**
     * 2단계 API 호출로 사진 저장 (새 버전)
     * 1단계: 이미지 분석 (BLIP 캡션)
     * 2단계: 일기 생성 (LLM)
     */
    suspend fun savePhotoWithEmotion(
        photoPath: String,
        userDescription: String?,
        userVoicePath: String?,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        captureDate: Long
    ): Result<Long> {
        return try {
            Log.d("PhotoRepository", "사진 저장 시작: $photoPath")
            Log.d("PhotoRepository", "위치 정보 - lat: $latitude, lng: $longitude, 주소: $locationName")
            Log.d("PhotoRepository", "사용자 입력: ${userDescription?.take(50)}")
            Log.d("PhotoRepository", "음성 파일: $userVoicePath")
            
            // 1단계: 이미지 분석 (BLIP 캡션)
            val caption = analyzeImageForCaption(photoPath)
            Log.d("PhotoRepository", "BLIP 캡션: ${caption?.take(100)}")
            
            // 2단계: 일기 생성 (LLM) - caption이 있으면 항상 호출 (userDescription은 nullable)
            var imageDescription: String? = null
            var tags: String? = null
            
            if (caption != null) {
                val diaryResult = generateDiaryWithLLM(
                    userInput = userDescription,  // nullable로 전달
                    blipCaption = caption,
                    latitude = latitude,
                    longitude = longitude,
                    location = locationName
                )
                imageDescription = diaryResult?.first
                tags = diaryResult?.second
                Log.d("PhotoRepository", "LLM 일기: ${imageDescription?.take(100)}")
                Log.d("PhotoRepository", "태그: $tags")
            } else {
                Log.w("PhotoRepository", "caption이 없어서 일기 생성 스킵")
            }
            
            // 3. 모든 정보를 로컬 DB에 저장
            val photoEntity = PhotoEntity(
                photoPath = photoPath,
                captureDate = captureDate,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                imageDescription = imageDescription,
                userDescription = userDescription,
                userVoicePath = userVoicePath,
                caption = caption,
                tags = tags
            )
            
            val photoId = photoDao.insertPhoto(photoEntity)
            Log.d("PhotoRepository", "DB 저장 완료 - Photo ID: $photoId")
            Result.success(photoId)
            
        } catch (e: Exception) {
            Log.e("PhotoRepository", "사진 저장 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 서버 통신 없이 빠르게 로컬 DB에만 저장 (caption, diary, tags 포함 가능)
     */
    suspend fun savePhotoLocal(
        photoPath: String,
        userDescription: String?,
        userVoicePath: String?,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        captureDate: Long,
        caption: String? = null,
        imageDescription: String? = null,
        tags: String? = null
    ): Result<Long> {
        return try {
            val photoEntity = PhotoEntity(
                photoPath = photoPath,
                captureDate = captureDate,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                imageDescription = imageDescription,
                userDescription = userDescription,
                userVoicePath = userVoicePath,
                caption = caption,
                tags = tags
            )
            
            val photoId = photoDao.insertPhoto(photoEntity)
            Result.success(photoId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 1단계: 이미지 분석 API 호출 (BLIP 캡션) - Public 메서드
     */
    suspend fun analyzeImageForCaption(photoPath: String): String? {
        return try {
            Log.d("PhotoRepository", "1단계 API 시작 - analyze")
            val photoFile = File(photoPath)
            
            if (!photoFile.exists()) {
                Log.w("PhotoRepository", "파일이 존재하지 않음: $photoPath")
                return null
            }
            
            Log.d("PhotoRepository", "파일 확인 - Size: ${photoFile.length()} bytes")
            
            // MIME 타입을 명시적으로 image/jpeg로 설정
            val mimeType = when {
                photoFile.extension.lowercase() == "png" -> "image/png"
                photoFile.extension.lowercase() == "jpg" -> "image/jpeg"
                photoFile.extension.lowercase() == "jpeg" -> "image/jpeg"
                else -> "image/jpeg"
            }
            Log.d("PhotoRepository", "MIME Type: $mimeType")
            
            val photoRequestBody = photoFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("image_file", photoFile.name, photoRequestBody)
            
            Log.d("PhotoRepository", "analyze API 요청 시작 (15초 타임아웃)")
            val response = withTimeoutOrNull(15_000) {
                apiService.analyzeImage(photoPart)
            }
            
            when {
                response == null -> {
                    Log.w("PhotoRepository", "analyze API 타임아웃 (15초)")
                    null
                }
                response.isSuccessful -> {
                    val caption = response.body()?.caption
                    Log.d("PhotoRepository", "analyze API 성공 - caption: $caption")
                    caption
                }
                else -> {
                    Log.w("PhotoRepository", "analyze API 실패 - Code: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "analyze API 오류", e)
            null
        }
    }
    
    /**
     * 2단계: 일기 생성 API 호출 (LLM) - Public 메서드
     * @return Pair<diary, tags> 또는 null
     */
    suspend fun generateDiaryWithLLM(
        userInput: String?,        // nullable로 변경
        blipCaption: String?,
        latitude: Double?,
        longitude: Double?,
        location: String?
    ): Pair<String, String>? {
        return try {
            Log.d("PhotoRepository", "2단계 API 시작 - generate")
            Log.d("PhotoRepository", "userInput: ${userInput?.take(30) ?: "null"}")
            Log.d("PhotoRepository", "blipCaption: ${blipCaption?.take(30) ?: "null"}")
            Log.d("PhotoRepository", "location: lat=$latitude, lng=$longitude, name=$location")
            
            val request = com.example.sodam_diary.data.network.GenerateRequest(
                user_input = userInput,
                blip_caption = blipCaption,
                latitude = latitude,
                longitude = longitude,
                location = location
            )
            
            Log.d("PhotoRepository", "generate API 요청 시작 (20초 타임아웃)")
            val response = withTimeoutOrNull(20_000) {
                apiService.generateDiary(request)
            }
            
            when {
                response == null -> {
                    Log.w("PhotoRepository", "generate API 타임아웃 (20초)")
                    null
                }
                response.isSuccessful -> {
                    val body = response.body()
                    val diary = body?.diary
                    val tagsList = body?.tags
                    val tagsString = tagsList?.joinToString(",") // 쉼표 구분 문자열로 변환
                    
                    Log.d("PhotoRepository", "generate API 성공 - diary: ${diary?.take(50)}")
                    Log.d("PhotoRepository", "tags: $tagsString")
                    
                    if (diary != null && tagsString != null) {
                        Pair(diary, tagsString)
                    } else {
                        null
                    }
                }
                else -> {
                    Log.w("PhotoRepository", "generate API 실패 - Code: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "generate API 오류", e)
            null
        }
    }
    
    // === 요구사항에 맞는 쿼리 메서드들 ===
    
    /**
     * 1. 전체 사진에 대한 시간별 내림차순 정렬
     */
    suspend fun getAllPhotos(): List<PhotoEntity> {
        return photoDao.getAllPhotos()
    }
    
    /**
     * 2. 해당 년, 월에 해당하는 사진들의 시간별 내림차순 정렬
     * @param year 년도 (예: 2024)
     * @param month 월 (1-12)
     */
    suspend fun getPhotosByYearMonth(year: Int, month: Int): List<PhotoEntity> {
        val yearStr = year.toString()
        val monthStr = String.format("%02d", month) // 01, 02, ..., 12 형태로 변환
        return photoDao.getPhotosByYearMonth(yearStr, monthStr)
    }
    
    /**
     * 3. 해당 시(위치)에서 찍은 사진들의 시간별 내림차순 정렬
     */
    suspend fun getPhotosByLocation(location: String): List<PhotoEntity> {
        return photoDao.getPhotosByLocation(location)
    }
    
    /**
     * 4. 년/월 + 위치 교집합: 해당 년월과 위치에서 찍은 사진들의 시간별 내림차순 정렬
     * @param year 년도 (예: 2024)
     * @param month 월 (1-12)
     * @param location 위치명 (예: "서울")
     */
    suspend fun getPhotosByYearMonthAndLocation(year: Int, month: Int, location: String): List<PhotoEntity> {
        val yearStr = year.toString()
        val monthStr = String.format("%02d", month)
        return photoDao.getPhotosByYearMonthAndLocation(yearStr, monthStr, location)
    }
    
    // === 기타 필요한 메서드들 ===
    
    /**
     * ID로 특정 사진 조회
     */
    suspend fun getPhotoById(photoId: Long): PhotoEntity? {
        return photoDao.getPhotoById(photoId)
    }
    
    /**
     * 사진 삭제 (연관 음성 파일도 함께 삭제)
     */
    suspend fun deletePhoto(photo: PhotoEntity) {
        // 1. 음성 파일 삭제
        photo.userVoicePath?.let { voicePath ->
            try {
                val voiceFile = File(voicePath)
                if (voiceFile.exists()) {
                    voiceFile.delete()
                    Log.d("PhotoRepository", "음성 파일 삭제 완료: $voicePath")
                }
            } catch (e: Exception) {
                Log.e("PhotoRepository", "음성 파일 삭제 실패", e)
            }
        }
        
        // 2. 사진 파일 삭제
        try {
            val photoFile = File(photo.photoPath)
            if (photoFile.exists()) {
                photoFile.delete()
                Log.d("PhotoRepository", "사진 파일 삭제 완료: ${photo.photoPath}")
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "사진 파일 삭제 실패", e)
        }
        
        // 3. DB에서 삭제
        photoDao.deletePhoto(photo)
        Log.d("PhotoRepository", "DB 레코드 삭제 완료 - Photo ID: ${photo.id}")
    }
    
    /**
     * 음성 검색: imageDescription, tags, userDescription, locationName에서 검색
     */
    suspend fun searchPhotosByVoice(query: String): List<PhotoEntity> {
        val trimmedQuery = query.trim()
        Log.d("PhotoRepository", "음성 검색 시작: '$trimmedQuery'")
        
        if (trimmedQuery.isEmpty()) {
            Log.d("PhotoRepository", "빈 검색어")
            return emptyList()
        }
        
        val results = photoDao.searchByVoiceQuery(trimmedQuery)
        Log.d("PhotoRepository", "검색 결과: ${results.size}개")
        return results
    }
}