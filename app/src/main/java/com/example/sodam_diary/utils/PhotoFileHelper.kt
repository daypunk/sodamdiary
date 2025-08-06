package com.example.sodam_diary.utils

import android.content.Context
import java.io.File

/**
 * 앱 내부 저장소에서 사진 파일 관리를 위한 유틸리티 클래스
 */
class PhotoFileHelper(private val context: Context) {
    
    // 사진 저장을 위한 디렉토리
    private val photosDir: File by lazy {
        File(context.filesDir, "photos").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * 새로운 사진 파일 경로 생성
     * @return 앱 내부 저장소의 사진 파일 경로
     * 예: /data/data/com.example.sodam_diary/files/photos/photo_1234567890.jpg
     */
    fun createNewPhotoPath(): String {
        val timestamp = System.currentTimeMillis()
        val photoFile = File(photosDir, "photo_$timestamp.jpg")
        return photoFile.absolutePath
    }
    
    /**
     * 사진 파일이 존재하는지 확인
     */
    fun photoExists(photoPath: String): Boolean {
        return File(photoPath).exists()
    }
    
    /**
     * 사진 파일 삭제
     */
    fun deletePhoto(photoPath: String): Boolean {
        return try {
            File(photoPath).delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 사진 디렉토리의 모든 파일 목록 조회
     */
    fun getAllPhotoFiles(): List<File> {
        return photosDir.listFiles()?.toList() ?: emptyList()
    }
    
    /**
     * 사진 디렉토리 전체 크기 계산 (bytes)
     */
    fun getTotalPhotoSize(): Long {
        return getAllPhotoFiles().sumOf { it.length() }
    }
}