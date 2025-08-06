package com.example.sodam_diary.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * 통합 이미지 및 파일 관리 유틸리티
 */
class PhotoManager(private val context: Context) {
    
    // 사진 저장을 위한 디렉토리
    private val photosDir: File by lazy {
        File(context.filesDir, "photos").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    // === 파일 관리 기능 ===
    
    /**
     * 새로운 사진 파일 경로 생성
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
    
    // === 이미지 처리 기능 ===
    
    /**
     * 회전된 이미지를 올바른 방향으로 로드
     */
    fun loadRotatedBitmap(imageFile: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null
        val exif = ExifInterface(imageFile.absolutePath)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * 경로로부터 회전된 이미지 로드
     */
    fun loadRotatedBitmap(imagePath: String): Bitmap? {
        return loadRotatedBitmap(File(imagePath))
    }
}

// 호환성을 위한 별칭들
class PhotoFileHelper(context: Context) : PhotoManager(context)

fun loadRotatedBitmap(imageFile: File): Bitmap? {
    return PhotoManager(imageFile.parentFile?.parent?.let { 
        android.app.Application().applicationContext 
    } ?: return null).loadRotatedBitmap(imageFile)
}
