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
     * 1단계: 이미지 분석 (BLIP 캡션 생성)
     * @param imageFile 촬영한 사진 파일
     * @return BLIP 기반 이미지 캡션
     */
    @Multipart
    @POST("api/v1/analyze/")
    suspend fun analyzeImage(
        @Part image_file: MultipartBody.Part
    ): Response<AnalyzeResponse>
    
    /**
     * 2단계: 일기 생성 (LLM 기반)
     * @param request 사용자 입력과 BLIP 캡션
     * @return LLM 생성 일기와 태그
     */
    @POST("api/v1/generate/")
    suspend fun generateDiary(
        @Body request: GenerateRequest
    ): Response<GenerateResponse>
}

/**
 * 이미지 분석 응답 (1단계)
 */
data class AnalyzeResponse(
    val caption: String
)

/**
 * 일기 생성 요청 (2단계)
 */
data class GenerateRequest(
    val user_input: String,
    val blip_caption: String
)

/**
 * 일기 생성 응답 (2단계)
 */
data class GenerateResponse(
    val diary: String,
    val tags: List<String>
)

// === 기존 API 호환성 유지 (deprecated) ===
/**
 * @deprecated 새로운 2단계 API(analyze, generate)를 사용하세요
 */
@Multipart
@Deprecated("Use analyzeImage and generateDiary instead")
suspend fun ApiService.analyzePhoto(
    @Part file: MultipartBody.Part?,
    @Part("file_info") fileInfo: RequestBody?
): Response<LegacyApiResponse> {
    throw UnsupportedOperationException("This endpoint is deprecated")
}

/**
 * @deprecated 레거시 응답 형식
 */
@Deprecated("Use AnalyzeResponse and GenerateResponse instead")
data class LegacyApiResponse(
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