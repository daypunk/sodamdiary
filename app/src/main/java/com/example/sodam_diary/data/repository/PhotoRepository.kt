package com.example.sodam_diary.data.repository

import android.content.Context
import com.example.sodam_diary.data.database.AppDatabase
import com.example.sodam_diary.data.database.PhotoDao
import com.example.sodam_diary.data.PhotoEntity
import com.example.sodam_diary.data.network.ApiService
import com.example.sodam_diary.data.network.NetworkClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
            // 1. 서버에 사진과 설명 전송
            val imageDescription = uploadPhotoAndGetDescription(photoPath, userDescription)
            
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
            val photoFile = File(photoPath)
            
            // 사진 파일을 MultipartBody로 변환 (선택사항이므로 null 가능)
            val photoPart = if (photoFile.exists()) {
                val photoRequestBody = photoFile.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", photoFile.name, photoRequestBody)
            } else {
                null
            }
            
            // 사용자 설명을 RequestBody로 변환 (선택사항이므로 null 가능)
            val fileInfoRequestBody = if (!userDescription.isNullOrBlank()) {
                userDescription.toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                null
            }
            
            // 서버에 전송
            val response = apiService.analyzePhoto(photoPart, fileInfoRequestBody)
            
            if (response.isSuccessful) {
                response.body()?.data?.file_description
            } else {
                null
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 null 반환 (오프라인 모드)
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