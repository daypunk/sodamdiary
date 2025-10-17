package com.example.sodam_diary.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 서버와의 통신을 위한 API 인터페이스
 */
interface ApiService {
    
    /**
     * 사진과 사용자 설명을 서버에 전송하여 이미지 설명을 받음
     * @param photo 촬영한 사진 파일
     * @param fileInfo 사용자가 입력한 설명 (STT로 변환된 텍스트, 선택사항)
     * @return 서버로부터 받은 이미지 설명
     */
    @Multipart
    @POST("api/v1/images/caption/")
    suspend fun analyzePhoto(
        @Part file: MultipartBody.Part?,
        @Part("file_info") fileInfo: RequestBody?
    ): Response<ApiResponse>
}

/**
 * 서버 응답 데이터 클래스
 */
data class ApiResponse(
    val file: String?,
    val refined_caption: String?,
    val blip_text: String?,
    val clip_text: String?,
    val file_info: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val id: Int?,
    val created_at: String?
)