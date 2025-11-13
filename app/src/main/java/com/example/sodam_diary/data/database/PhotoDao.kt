package com.example.sodam_diary.data.database

import androidx.room.*
import com.example.sodam_diary.data.entity.PhotoEntity

/**
 * 사진 데이터 접근을 위한 DAO (Data Access Object)
 */
@Dao
interface PhotoDao {
    
    /**
     * 새로운 사진 정보 저장
     */
    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long
    
    /**
     * 1. 전체 사진 시간별 내림차순 정렬
     */
    @Query("SELECT * FROM photos ORDER BY captureDate DESC")
    suspend fun getAllPhotos(): List<PhotoEntity>
    
    /**
    // ID로 특정 사진 조회
     */
    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Long): PhotoEntity?
    
    /**
     * 2. 특정 년/월에 해당하는 사진들의 시간별 내림차순 정렬
     * @param year 년도 (예: 2024)
     * @param month 월 (1-12)
     */
    @Query("""
        SELECT * FROM photos 
        WHERE strftime('%Y', datetime(captureDate/1000, 'unixepoch')) = :year 
        AND strftime('%m', datetime(captureDate/1000, 'unixepoch')) = :month
        ORDER BY captureDate DESC
    """)
    suspend fun getPhotosByYearMonth(year: String, month: String): List<PhotoEntity>
    
    /**
     * 3. 특정 위치에서 찍은 사진들의 시간별 내림차순 정렬
     */
    @Query("SELECT * FROM photos WHERE locationName LIKE '%' || :location || '%' ORDER BY captureDate DESC")
    suspend fun getPhotosByLocation(location: String): List<PhotoEntity>
    
    /**
     * 4. 특정 년/월 + 위치 교집합 검색 (시간별 내림차순)
     * @param year 년도 (예: "2024")
     * @param month 월 (예: "01", "02", ..., "12")
     * @param location 위치명 (예: "서울")
     */
    @Query("""
        SELECT * FROM photos 
        WHERE strftime('%Y', datetime(captureDate/1000, 'unixepoch')) = :year 
        AND strftime('%m', datetime(captureDate/1000, 'unixepoch')) = :month
        AND locationName LIKE '%' || :location || '%'
        ORDER BY captureDate DESC
    """)
    suspend fun getPhotosByYearMonthAndLocation(year: String, month: String, location: String): List<PhotoEntity>
    
    /**
     * 사진 삭제
     */
    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)
    
    /**
     * 음성 검색: caption 또는 tags에 검색어가 포함된 사진들 조회
     * @param query 검색어 (예: "산", "바다")
     */
    @Query("""
        SELECT * FROM photos 
        WHERE caption LIKE '%' || :query || '%' 
        OR tags LIKE '%' || :query || '%'
        OR userDescription LIKE '%' || :query || '%'
        OR locationName LIKE '%' || :query || '%'
        ORDER BY captureDate DESC
    """)
    suspend fun searchByVoiceQuery(query: String): List<PhotoEntity>
}