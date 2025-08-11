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
     * 사진과 사용자 설명을 서버에 전송하고 이미지 설명을 받아서 로컬 DB에 저장
     */
    suspend fun savePhotoWithEmotion(
        photoPath: String,
        userDescription: String?,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        captureDate: Long
    ): Result<Long> {
        return try {
            Log.d("PhotoRepository", "📸 사진 저장 시작 - Path: $photoPath")
            Log.d("PhotoRepository", "🌍 위치 정보 - lat: $latitude, lng: $longitude, 주소: $locationName")
            Log.d("PhotoRepository", "✏️ 사용자 입력 - userDescription: ${userDescription?.take(50)}")
            
            // 1. 서버에 사진과 설명 전송
            val imageDescription = uploadPhotoAndGetDescription(photoPath, userDescription)
            Log.d("PhotoRepository", "🤖 서버 응답 - imageDescription: ${imageDescription?.take(100)}")
            
            // 2. 모든 정보를 로컬 DB에 저장
            val photoEntity = PhotoEntity(
                photoPath = photoPath,
                captureDate = captureDate,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                imageDescription = imageDescription,
                userDescription = userDescription
            )
            
            val photoId = photoDao.insertPhoto(photoEntity)
            Log.d("PhotoRepository", "💾 DB 저장 완료 - Photo ID: $photoId")
            Result.success(photoId)
            
        } catch (e: Exception) {
            Log.e("PhotoRepository", "❌ 사진 저장 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 서버 통신 없이 빠르게 로컬 DB에만 저장 (개발/테스트용)
     */
    suspend fun savePhotoLocal(
        photoPath: String,
        userDescription: String?,
        latitude: Double?,
        longitude: Double?,
        locationName: String?,
        captureDate: Long
    ): Result<Long> {
        return try {
            // 임시 이미지 설명 (서버 통신 없이)
            val imageDescription = "사진이 성공적으로 저장되었습니다."
            
            val photoEntity = PhotoEntity(
                photoPath = photoPath,
                captureDate = captureDate,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                imageDescription = imageDescription,
                userDescription = userDescription
            )
            
            val photoId = photoDao.insertPhoto(photoEntity)
            Result.success(photoId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 서버에 사진과 설명을 전송하여 이미지 설명 받기
     */
    private suspend fun uploadPhotoAndGetDescription(
        photoPath: String,
        userDescription: String?
    ): String? {
        return try {
            Log.d("PhotoRepository", "🌐 서버 통신 시작 - File: $photoPath")
            val photoFile = File(photoPath)
            
            // 사진 파일을 MultipartBody로 변환 (선택사항이므로 null 가능)
            val photoPart = if (photoFile.exists()) {
                Log.d("PhotoRepository", "📁 파일 존재 확인 - Size: ${photoFile.length()} bytes")
                val photoRequestBody = photoFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", photoFile.name, photoRequestBody)
            } else {
                Log.w("PhotoRepository", "⚠️ 파일이 존재하지 않음: $photoPath")
                null
            }
            
            // 사용자 설명을 RequestBody로 변환 (선택사항이므로 null 가능)
            val fileInfoRequestBody = if (!userDescription.isNullOrBlank()) {
                Log.d("PhotoRepository", "📝 사용자 설명 포함: ${userDescription.take(30)}")
                userDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                Log.d("PhotoRepository", "📝 사용자 설명 없음")
                null
            }
            
            // 서버에 전송 (6초 타임아웃로 완화)
            Log.d("PhotoRepository", "⏱️ 서버 요청 시작 (15초 타임아웃)")
            val response = withTimeoutOrNull(15_000) {
                apiService.analyzePhoto(photoPart, fileInfoRequestBody)
            }
            
            when {
                response == null -> {
                    Log.w("PhotoRepository", "⏰ 서버 응답 타임아웃 (15초)")
                    null
                }
                response.isSuccessful -> {
                    val description = response.body()?.data?.refined_caption
                    Log.d("PhotoRepository", "✅ 서버 응답 성공: $description")
                    description
                }
                else -> {
                    Log.w("PhotoRepository", "❌ 서버 응답 실패 - Code: ${response.code()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "🚫 네트워크 오류 (오프라인 모드)", e)
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
     * 사진 삭제
     */
    suspend fun deletePhoto(photo: PhotoEntity) {
        photoDao.deletePhoto(photo)
    }
}